package com.example.bookmanagement.dto.book

import com.example.bookmanagement.dto.author.AuthorResponse
import com.example.bookmanagement.model.PublicationStatus

/**
 * 書籍APIの詳細レスポンスDTO。
 *
 * 書籍登録・更新の結果として、書籍情報と紐づく著者一覧をまとめて返す。
 *
 * @property id 書籍ID。
 * @property title 書籍名。
 * @property price 価格。
 * @property publicationStatus 出版状態。
 * @property authors 書籍に紐づく著者一覧。
 */
data class BookResponse(
	val id: Long,
	val title: String,
	val price: Int,
	val publicationStatus: PublicationStatus,
	val authors: List<AuthorResponse>,
)
