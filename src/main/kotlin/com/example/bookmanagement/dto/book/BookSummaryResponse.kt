package com.example.bookmanagement.dto.book

import com.example.bookmanagement.model.PublicationStatus

/**
 * 書籍一覧表示用の簡易レスポンスDTO。
 *
 * 著者別書籍取得APIで利用し、一覧用途のため著者一覧は含めない。
 *
 * @property id 書籍ID。
 * @property title 書籍名。
 * @property price 価格。
 * @property publicationStatus 出版状態。
 */
data class BookSummaryResponse(
	val id: Long,
	val title: String,
	val price: Int,
	val publicationStatus: PublicationStatus,
)
