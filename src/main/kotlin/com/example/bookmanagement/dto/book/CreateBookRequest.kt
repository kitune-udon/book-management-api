package com.example.bookmanagement.dto.book

import com.example.bookmanagement.model.PublicationStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

/**
 * 書籍登録APIのリクエストDTO。
 *
 * Controllerで基本的な入力値検証を行い、著者IDの存在確認や重複確認はService層で行う。
 *
 * @property title 書籍名。空文字や空白のみ、DBの最大長を超える値は許可しない。
 * @property price 価格。0以上のみ許可する。
 * @property publicationStatus 出版状態。
 * @property authorIds 書籍に紐づける著者ID一覧。1件以上の指定が必要。
 */
data class CreateBookRequest(
	@field:NotBlank(message = "Book title must not be blank")
	@field:Size(max = 255, message = "Book title must be 255 characters or less")
	val title: String,

	@field:Min(value = 0, message = "Book price must be greater than or equal to 0")
	val price: Int,

	val publicationStatus: PublicationStatus,

	@field:NotEmpty(message = "Book must have at least one author")
	val authorIds: List<Long>,
)
