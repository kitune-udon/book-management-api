package com.example.bookmanagement.dto.book

import com.example.bookmanagement.model.PublicationStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class UpdateBookRequest(
	@field:NotBlank(message = "Book title must not be blank")
	val title: String,

	@field:Min(value = 0, message = "Book price must be greater than or equal to 0")
	val price: Int,

	val publicationStatus: PublicationStatus,

	@field:NotEmpty(message = "Book must have at least one author")
	val authorIds: List<Long>,
)
