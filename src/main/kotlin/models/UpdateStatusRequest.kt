package com.example.models

import kotlinx.serialization.Serializable


@Serializable
data class UpdateStatusRequest(
    val status: String
)
