package com.example.bookmanagement.dto.author

import java.time.LocalDate

data class AuthorResponse(
	val id: Long,
	val name: String,
	val birthDate: LocalDate,
)
