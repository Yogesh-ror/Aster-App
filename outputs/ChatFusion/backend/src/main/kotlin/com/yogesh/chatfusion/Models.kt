package com.yogesh.chatfusion

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
    val name: String? = null,
    val mobileNumber: String? = null,
    val photoUrl: String? = null
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String,
    val name: String = "",
    val mobileNumber: String = "",
    val photoUrl: String = ""
)

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val password: String? = null,
    val mobileNumber: String? = null,
    val photoUrl: String? = null
)

@Serializable
data class UserProfileDto(
    val userId: String,
    val email: String,
    val name: String = "",
    val mobileNumber: String = "",
    val photoUrl: String = ""
)

@Serializable
data class ChatCreateRequest(
    val provider: String? = null
)

@Serializable
data class ChatDto(
    val id: String,
    val title: String,
    val provider: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SendMessageRequest(
    val message: String,
    val provider: String? = null
)

@Serializable
data class MessageDto(
    val id: String,
    val chatId: String,
    val role: String,
    val content: String,
    val provider: String,
    val createdAt: Long
)

@Serializable
data class SendMessageResponse(
    val userMessage: MessageDto,
    val assistantMessage: MessageDto
)

@Serializable
data class ErrorResponse(
    val error: String
)

data class ConversationTurn(
    val role: String,
    val content: String
)
