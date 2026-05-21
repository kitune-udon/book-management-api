package com.example.bookmanagement.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class AuthorControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var objectMapper: ObjectMapper

	@Test
	fun `著者を登録できる`() {
		mockMvc.perform(
			post("/authors")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "name": "夏目漱石",
					  "birthDate": "1867-02-09"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.name").value("夏目漱石"))
			.andExpect(jsonPath("$.birthDate").value("1867-02-09"))
	}

	@Test
	fun `著者を更新できる`() {
		val authorId = createAuthor(name = "更新前著者")

		mockMvc.perform(
			put("/authors/$authorId")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "name": "更新後著者",
					  "birthDate": "1901-01-01"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(authorId))
			.andExpect(jsonPath("$.name").value("更新後著者"))
			.andExpect(jsonPath("$.birthDate").value("1901-01-01"))
	}

	@Test
	fun `著者名が空の場合は400を返す`() {
		mockMvc.perform(
			post("/authors")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "name": "",
					  "birthDate": "1867-02-09"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Author name must not be blank"))
	}

	@Test
	fun `著者の生年月日が未来日の場合は400を返す`() {
		mockMvc.perform(
			post("/authors")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "name": "未来著者",
					  "birthDate": "2999-01-01"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Author birth date must be today or past date"))
	}

	@Test
	fun `存在しない著者を更新する場合は404を返す`() {
		val authorId = Long.MAX_VALUE

		mockMvc.perform(
			put("/authors/$authorId")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "name": "存在しない著者",
					  "birthDate": "1900-01-01"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.message").value("Author not found: id=$authorId"))
	}

	@Test
	fun `著者IDのパスパラメータが不正な場合は400を返す`() {
		mockMvc.perform(
			put("/authors/abc")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "name": "著者",
					  "birthDate": "1900-01-01"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Invalid path parameter"))
	}

	@Test
	fun `著者に紐づく書籍一覧を取得できる`() {
		val authorId = createAuthor(name = "書籍あり著者A")
		val coAuthorId = createAuthor(name = "書籍あり著者B")

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
	fun `著者に紐づく書籍がない場合は空配列を返す`() {
		val authorId = createAuthor(name = "書籍なし著者")

		mockMvc.perform(get("/authors/$authorId/books"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$", hasSize<Any>(0)))
	}

	@Test
	fun `存在しない著者の書籍一覧取得は404を返す`() {
		val authorId = Long.MAX_VALUE

		mockMvc.perform(get("/authors/$authorId/books"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.message").value("Author not found: id=$authorId"))
	}

	@Test
	fun `著者別書籍取得のパスパラメータが不正な場合は400を返す`() {
		mockMvc.perform(get("/authors/abc/books"))
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Invalid path parameter"))
	}

	private fun createAuthor(
		name: String = "テスト著者",
		birthDate: String = "1900-01-01",
	): Long {
		val result = mockMvc.perform(
			post("/authors")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "name": "$name",
					  "birthDate": "$birthDate"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andReturn()

		return objectMapper.readTree(result.response.contentAsString)
			.get("id")
			.asLong()
	}

	private fun createBook(
		title: String = "テスト書籍",
		price: Int = 1000,
		publicationStatus: String = "UNPUBLISHED",
		authorIds: List<Long>,
	): Long {
		val result = mockMvc.perform(
			post("/books")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "$title",
					  "price": $price,
					  "publicationStatus": "$publicationStatus",
					  "authorIds": ${objectMapper.writeValueAsString(authorIds)}
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andReturn()

		return objectMapper.readTree(result.response.contentAsString)
			.get("id")
			.asLong()
	}
}
