package com.example.bookmanagement.controller

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 共通例外ハンドラーのController統合テスト。
 *
 * Spring MVC由来のクライアントエラーが、想定外エラーとして500に変換されないことを確認する。
 */
class ApiExceptionHandlerTest : ControllerTestSupport() {

	@Test
	fun `未対応HTTPメソッドの場合は405になる`() {
		mockMvc.perform(delete("/authors"))
			.andExpect(status().isMethodNotAllowed)
			.andExpectErrorBody(405, "Method not allowed")
	}
}
