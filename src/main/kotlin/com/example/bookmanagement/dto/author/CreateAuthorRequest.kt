package com.example.bookmanagement.dto.author

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * 著者登録APIのリクエストDTO。
 *
 * Controllerで基本的な入力値検証を行い、生年月日の業務ルール検証はService層で行う。
 *
 * @property name 著者名。空文字や空白のみ、DBの最大長を超える値は許可しない。
 * @property birthDate 生年月日。現在日以前であることを業務ルールとして検証する。
 */
data class CreateAuthorRequest(
	@field:NotBlank(message = "Author name must not be blank")
	@field:Size(max = 255, message = "Author name must be 255 characters or less")
	val name: String,

	val birthDate: LocalDate,
)
