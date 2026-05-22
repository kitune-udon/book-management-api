package com.example.bookmanagement.dto.author

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

/**
 * 著者更新APIのリクエストDTO。
 *
 * 更新対象の著者IDはパスパラメータで受け取り、このDTOでは更新後の著者情報だけを表す。
 *
 * @property name 更新後の著者名。空文字や空白のみは許可しない。
 * @property birthDate 更新後の生年月日。現在日以前であることを業務ルールとして検証する。
 */
data class UpdateAuthorRequest(
	@field:NotBlank(message = "Author name must not be blank")
	val name: String,

	val birthDate: LocalDate,
)
