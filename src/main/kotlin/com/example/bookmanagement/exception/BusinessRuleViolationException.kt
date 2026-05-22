package com.example.bookmanagement.exception

/**
 * 業務ルール違反を表す例外。
 *
 * Service層で検出した仕様上許可しない操作を表し、APIレスポンスでは400 Bad Requestへ変換する。
 */
class BusinessRuleViolationException(
	message: String,
) : RuntimeException(message)
