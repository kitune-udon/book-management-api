package com.example.bookmanagement.controller

import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

class AuthorControllerTest : ControllerTestSupport() {

	@Test
	fun `A-01 著者を登録できる`() {
		postJson(
			"/authors",
			"""
			{
			  "name": "夏目漱石",
			  "birthDate": "1867-02-09"
			}
			""",
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.name").value("夏目漱石"))
			.andExpect(jsonPath("$.birthDate").value("1867-02-09"))
	}

	@Test
	fun `A-02 著者名が空の場合は登録できない`() {
		postJson(
			"/authors",
			"""
			{
			  "name": "",
			  "birthDate": "1867-02-09"
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Author name must not be blank")
	}

	@Test
	fun `A-03 著者名が空白のみの場合は登録できない`() {
		postJson(
			"/authors",
			"""
			{
			  "name": "   ",
			  "birthDate": "1867-02-09"
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Author name must not be blank")
	}

	@Test
	fun `A-04 生年月日が未来日の場合は登録できない`() {
		postJson(
			"/authors",
			"""
			{
			  "name": "未来著者",
			  "birthDate": "2999-01-01"
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Author birth date must be today or past date")
	}

	@Test
	fun `A-05 生年月日が当日の場合は登録できる`() {
		val today = LocalDate.now()

		postJson(
			"/authors",
			"""
			{
			  "name": "当日生年月日著者",
			  "birthDate": "$today"
			}
			""",
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.name").value("当日生年月日著者"))
			.andExpect(jsonPath("$.birthDate").value(today.toString()))
	}

	@Test
	fun `A-06 著者を更新できる`() {
		val authorId = createAuthor(name = "更新前著者")

		putJson(
			"/authors/$authorId",
			"""
			{
			  "name": "更新後著者",
			  "birthDate": "1901-01-01"
			}
			""",
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(authorId))
			.andExpect(jsonPath("$.name").value("更新後著者"))
			.andExpect(jsonPath("$.birthDate").value("1901-01-01"))
	}

	@Test
	fun `A-07 存在しない著者IDを更新すると404になる`() {
		val authorId = Long.MAX_VALUE

		putJson(
			"/authors/$authorId",
			"""
			{
			  "name": "存在しない著者",
			  "birthDate": "1900-01-01"
			}
			""",
		)
			.andExpect(status().isNotFound)
			.andExpectErrorBody(404, "Author not found: id=$authorId")
	}

	@Test
	fun `A-08 著者名が空の場合は更新できない`() {
		val authorId = createAuthor()

		putJson(
			"/authors/$authorId",
			"""
			{
			  "name": "",
			  "birthDate": "1900-01-01"
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Author name must not be blank")
	}

	@Test
	fun `A-09 生年月日が未来日の場合は更新できない`() {
		val authorId = createAuthor()

		putJson(
			"/authors/$authorId",
			"""
			{
			  "name": "未来日更新著者",
			  "birthDate": "2999-01-01"
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Author birth date must be today or past date")
	}

	@Test
	fun `A-10 生年月日が日付形式でない場合は登録できない`() {
		postJson(
			"/authors",
			"""
			{
			  "name": "日付不正著者",
			  "birthDate": "invalid-date"
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid request body")
	}

	@Test
	fun `A-11 JSON構文不正の場合は登録できない`() {
		postJson(
			"/authors",
			"""
			{
			  "name": "JSON不正著者",
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid request body")
	}

	@Test
	fun `A-12 リクエストボディ未指定の場合は登録できない`() {
		postJsonWithoutBody("/authors")
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid request body")
	}

	@Test
	fun `A-13 authorIdが数値でない場合は400になる`() {
		putJson(
			"/authors/abc",
			"""
			{
			  "name": "著者",
			  "birthDate": "1900-01-01"
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid path parameter")
	}

	@Test
	fun `A-14 生年月日が日付形式でない場合は更新できない`() {
		val authorId = createAuthor()

		putJson(
			"/authors/$authorId",
			"""
			{
			  "name": "日付不正更新著者",
			  "birthDate": "invalid-date"
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid request body")
	}

	@Test
	fun `AB-01 著者に紐づく書籍一覧を取得できる`() {
		val authorId = createAuthor(name = "書籍あり著者")

		createBook(
			title = "著者の書籍",
			price = 1200,
			publicationStatus = "PUBLISHED",
			authorIds = listOf(authorId),
		)

		mockMvc.perform(get("/authors/$authorId/books"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$", hasSize<Any>(1)))
			.andExpect(jsonPath("$[0].title").value("著者の書籍"))
			.andExpect(jsonPath("$[0].price").value(1200))
			.andExpect(jsonPath("$[0].publicationStatus").value("PUBLISHED"))
			.andExpect(jsonPath("$[0].authors").doesNotExist())
	}

	@Test
	fun `AB-02 複数書籍が紐づく著者の書籍一覧を取得できる`() {
		val authorId = createAuthor(name = "複数書籍あり著者A")
		val coAuthorId = createAuthor(name = "複数書籍あり著者B")

		createBook(
			title = "著者Aのみの書籍",
			price = 1200,
			publicationStatus = "PUBLISHED",
			authorIds = listOf(authorId),
		)
		createBook(
			title = "著者Aと著者Bの共著",
			price = 1500,
			publicationStatus = "UNPUBLISHED",
			authorIds = listOf(authorId, coAuthorId),
		)

		mockMvc.perform(get("/authors/$authorId/books"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$", hasSize<Any>(2)))
			.andExpect(jsonPath("$[0].title").value("著者Aのみの書籍"))
			.andExpect(jsonPath("$[0].price").value(1200))
			.andExpect(jsonPath("$[0].publicationStatus").value("PUBLISHED"))
			.andExpect(jsonPath("$[0].authors").doesNotExist())
			.andExpect(jsonPath("$[1].title").value("著者Aと著者Bの共著"))
			.andExpect(jsonPath("$[1].price").value(1500))
			.andExpect(jsonPath("$[1].publicationStatus").value("UNPUBLISHED"))
			.andExpect(jsonPath("$[1].authors").doesNotExist())
	}

	@Test
	fun `AB-03 書籍が0件の著者では空配列を返す`() {
		val authorId = createAuthor(name = "書籍なし著者")

		mockMvc.perform(get("/authors/$authorId/books"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$", hasSize<Any>(0)))
	}

	@Test
	fun `AB-04 存在しない著者IDを指定すると404になる`() {
		val authorId = Long.MAX_VALUE

		mockMvc.perform(get("/authors/$authorId/books"))
			.andExpect(status().isNotFound)
			.andExpectErrorBody(404, "Author not found: id=$authorId")
	}

	@Test
	fun `AB-05 authorIdが数値でない場合は400になる`() {
		mockMvc.perform(get("/authors/abc/books"))
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid path parameter")
	}

}
