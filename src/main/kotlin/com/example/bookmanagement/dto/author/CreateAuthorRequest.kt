package com.example.bookmanagement.dto.author

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class CreateAuthorRequest(
	@field:NotBlank(message = "Author name must not be blank")
	val name: String,

	val birthDate: LocalDate,
)
