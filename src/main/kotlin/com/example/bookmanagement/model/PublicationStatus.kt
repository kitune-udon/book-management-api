package com.example.bookmanagement.model

/**
 * 書籍の出版状態。
 *
 * APIリクエスト・レスポンスでは文字列として扱われ、DBにはenum名を保存する。
 * 出版済みから未出版へ戻すことは業務ルールとしてService層で禁止する。
 */
enum class PublicationStatus {
	/**
	 * 未出版の書籍。
	 */
	UNPUBLISHED,

	/**
	 * 出版済みの書籍。
	 */
	PUBLISHED,
}
