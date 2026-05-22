package com.example.bookmanagement.exception

/**
 * 指定されたリソースが存在しないことを表す例外。
 *
 * Service層で著者や書籍が見つからない場合に利用し、APIレスポンスでは404 Not Foundへ変換する。
 */
class NotFoundException(
	message: String,
) : RuntimeException(message)
