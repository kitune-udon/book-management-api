package com.example.bookmanagement.dto.author

import java.time.LocalDate

/**
 * 著者APIのレスポンスDTO。
 *
 * 登録・更新後の著者情報や、書籍レスポンス内に含める著者情報として利用する。
 *
 * @property id 著者ID。
 * @property name 著者名。
 * @property birthDate 生年月日。
 */
data class AuthorResponse(
	val id: Long,
	val name: String,
	val birthDate: LocalDate,
)
