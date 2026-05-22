package com.example.bookmanagement.exception

/**
 * API共通のエラーレスポンスDTO。
 *
 * 例外ハンドラーでHTTPステータスと利用者向けメッセージをこの形式に揃えて返す。
 *
 * @property status HTTPステータスコード。
 * @property message エラー内容を表すメッセージ。
 */
data class ErrorResponse(
	val status: Int,
	val message: String,
)
