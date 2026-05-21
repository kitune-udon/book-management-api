package com.example.bookmanagement.dto.book

import com.example.bookmanagement.model.PublicationStatus

data class BookSummaryResponse(
	val id: Long,
	val title: String,
	val price: Int,
	val publicationStatus: PublicationStatus,
)
