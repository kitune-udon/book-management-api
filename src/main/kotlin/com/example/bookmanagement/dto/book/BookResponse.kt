package com.example.bookmanagement.dto.book

import com.example.bookmanagement.dto.author.AuthorResponse
import com.example.bookmanagement.model.PublicationStatus

data class BookResponse(
	val id: Long,
	val title: String,
	val price: Int,
	val publicationStatus: PublicationStatus,
	val authors: List<AuthorResponse>,
)
