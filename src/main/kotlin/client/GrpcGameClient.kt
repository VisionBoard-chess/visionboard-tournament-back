package com.example.client

import chesslive.*
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.flow

class GrpcGameClient(host: String = "localhost", port: Int = 50051) {
    private val channel = ManagedChannelBuilder
        .forAddress(host, port)
        .usePlaintext()
        .build()

    private val stub = GameServiceGrpcKt.GameServiceCoroutineStub(channel)

    suspend fun editMove(gameId: String, moveIndex: Int, newMoveSan: String): Boolean{
        val requestFlow = flow {
            emit(editGameMessage {
                request = editGameRequest { gameName = gameId }
            })

            emit(editGameMessage {
                editMove = editMoveRequest {
                    moveNumber = moveIndex
                    newMove = newMoveSan
                }
            })
        }

        var success = false
        try {
            println(">>> [gRPC Client] Intentando enviar editMove a $gameId...")
            stub.editGame(requestFlow).collect { response ->
                println(">>> [gRPC Client] Respuesta recibida de Python: $response")
                when {
                    response.hasEditResult() -> {
                        success = response.editResult.success
                        println(">>> [gRPC Client] editResult.success = $success (Mensaje: ${response.editResult.message})")
                    }
                    response.hasMovesList() -> {
                        println(">>> [gRPC Client] movesList devuelto: ${response.movesList.movesList}")
                    }
                }
            }
        } catch (e: Exception) {
            println(">>> [gRPC Client] EXCEPCIÓN DE RED AL HABLAR CON PYTHON:")
            e.printStackTrace()
        }
        return success
    }

    suspend fun addMove(gameId: String, newMoveSan: String): Boolean{
        val requestFlow = flow {
            emit(addMovesMessage {
                request = addMovesRequest{
                    gameName = gameId
                }
            })

            emit(addMovesMessage {
                newMove = newMove { move = newMoveSan}
            })

            emit(addMovesMessage {
                done = doneSignal {done = true}
            })
        }

        var success = false
        stub.addMoves(requestFlow).collect{response ->
            when{
                response.hasValidation() -> success = response.validation.success
            }
        }
        return success
    }

    fun shutdown() {
        channel.shutdown()
    }
}