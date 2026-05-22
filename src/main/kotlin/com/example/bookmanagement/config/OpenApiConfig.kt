package com.example.bookmanagement.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger UI / OpenAPI に表示するAPI全体のメタ情報を定義する設定クラス。
 *
 * 各エンドポイントの詳細はControllerやDTOから自動生成し、
 * ここではAPI名・バージョン・概要のようなドキュメント共通情報だけを扱う。
 */
@Configuration
class OpenApiConfig {

	/**
	 * Swagger UIのヘッダーや `/v3/api-docs` の `info` セクションに出力する情報を設定する。
	 */
	@Bean
	fun openAPI(): OpenAPI {
		return OpenAPI()
			.info(
				Info()
					.title("Book Management API")
					.version("1.0.0")
					.description("書籍と著者を管理するためのREST API"),
			)
	}
}
