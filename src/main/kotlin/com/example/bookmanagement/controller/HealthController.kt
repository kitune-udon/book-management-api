package com.example.bookmanagement.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * アプリケーションの起動確認用エンドポイントを提供するController。
 *
 * 外部監視というより、ローカル起動後にAPIが応答できる状態かを簡易確認する目的で利用する。
 */
@RestController
class HealthController {

	/**
	 * アプリケーションが起動していることを示す固定レスポンスを返す。
	 */
	@GetMapping("/health")
	fun health(): Map<String, String> {
		return mapOf("status" to "UP")
	}
}
