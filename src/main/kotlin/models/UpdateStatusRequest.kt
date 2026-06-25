package com.example.models

import kotlinx.serialization.Serializable


@Serializable
data class UpdateStatusRequest(
    val status: String
)

enum class Status {
    NOT_STARTED,
    ACTIVE,
    FINISHED
}
