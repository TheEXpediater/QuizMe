package com.quizifyai.data.model

import androidx.annotation.Keep
import com.quizifyai.domain.model.User

@Keep
data class UserDto(
    val id: String = "",
    val email: String = "",
    val createdAt: Long = 0L,
)

fun UserDto.toDomain() = User(
    id = id,
    email = email,
    createdAt = createdAt,
)

fun User.toDto() = UserDto(
    id = id,
    email = email,
    createdAt = createdAt,
)
