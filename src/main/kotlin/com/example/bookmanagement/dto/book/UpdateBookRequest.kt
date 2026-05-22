package com.example.bookmanagement.dto.book

import com.example.bookmanagement.model.PublicationStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

/**
 * 書籍更新APIのリクエストDTO。
 *
 * 更新対象の書籍IDはパスパラメータで受け取り、このDTOでは更新後の書籍情報と著者関連を表す。
 * 著者IDの存在確認、重複確認、出版状態の遷移制約はService層で検証する。
 *
 * @property title 更新後の書籍名。空文字や空白のみ、DBの最大長を超える値は許可しない。
 * @property price 更新後の価格。0以上のみ許可する。
 * @property publicationStatus 更新後の出版状態。
 * @property authorIds 更新後に書籍へ紐づける著者ID一覧。1件以上の指定が必要。
 */
data class UpdateBookRequest(
	@field:NotBlank(message = "Book title must not be blank")
	@field:Size(max = 255, message = "Book title must be 255 characters or less")
	val title: String,

	@field:Min(value = 0, message = "Book price must be greater than or equal to 0")
	val price: Int,

	val publicationStatus: PublicationStatus,

	@field:NotEmpty(message = "Book must have at least one author")
	val authorIds: List<Long>,
)
