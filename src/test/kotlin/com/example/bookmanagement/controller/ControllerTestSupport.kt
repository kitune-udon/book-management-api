package com.example.bookmanagement.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.Rollback
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

/**
 * Controller統合テストの共通基底クラス。
 *
 * Spring BootのテストコンテキストとMockMvcを共有し、JSONリクエスト送信やテストデータ作成の重複を抑える。
 * 各テストで作成したDBデータはrollbackし、ローカルDBへテストデータが残らないようにする。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
abstract class ControllerTestSupport {

	@Autowired
	protected lateinit var mockMvc: MockMvc

	@Autowired
	protected lateinit var objectMapper: ObjectMapper

	/**
	 * JSONボディ付きのPOSTリクエストを送信する。
	 */
	protected fun postJson(
		path: String,
		content: String,
	): ResultActions {
		return mockMvc.perform(
			post(path)
				.contentType(MediaType.APPLICATION_JSON)
				.content(content.trimIndent()),
		)
	}

	/**
	 * ボディなしのPOSTリクエストを送信する。
	 */
	protected fun postJsonWithoutBody(path: String): ResultActions {
		return mockMvc.perform(
			post(path)
				.contentType(MediaType.APPLICATION_JSON),
		)
	}

	/**
	 * JSONボディ付きのPUTリクエストを送信する。
	 */
	protected fun putJson(
		path: String,
		content: String,
	): ResultActions {
		return mockMvc.perform(
			put(path)
				.contentType(MediaType.APPLICATION_JSON)
				.content(content.trimIndent()),
		)
	}

	/**
	 * 後続テストの前提データとして著者をAPI経由で作成し、作成されたIDを返す。
	 */
	protected fun createAuthor(
		name: String = "テスト著者",
		birthDate: String = "1900-01-01",
	): Long {
		val result = postJson(
			"/authors",
			"""
			{
			  "name": "$name",
			  "birthDate": "$birthDate"
			}
			""",
		)
			.andExpect(status().isCreated)
			.andReturn()

		return objectMapper.readTree(result.response.contentAsString)
			.get("id")
			.asLong()
	}

	/**
	 * 後続テストの前提データとして書籍をAPI経由で作成し、作成されたIDを返す。
	 */
	protected fun createBook(
		title: String = "テスト書籍",
		price: Int = 1000,
		publicationStatus: String = "UNPUBLISHED",
		authorIds: List<Long>,
	): Long {
		val result = postJson(
			"/books",
			"""
			{
			  "title": "$title",
			  "price": $price,
			  "publicationStatus": "$publicationStatus",
			  "authorIds": ${objectMapper.writeValueAsString(authorIds)}
			}
			""",
		)
			.andExpect(status().isCreated)
			.andReturn()

		return objectMapper.readTree(result.response.contentAsString)
			.get("id")
			.asLong()
	}

	/**
	 * 共通エラーレスポンスの本文を検証する。
	 *
	 * HTTPステータスそのものは各テストメソッド側で明示的に検証する。
	 */
	protected fun ResultActions.andExpectErrorBody(
		status: Int,
		message: String,
	): ResultActions {
		return andExpect(jsonPath("$.status").value(status))
			.andExpect(jsonPath("$.message").value(message))
	}
}
