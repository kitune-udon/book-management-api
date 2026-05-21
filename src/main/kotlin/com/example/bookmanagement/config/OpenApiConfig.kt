package com.example.bookmanagement.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

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
