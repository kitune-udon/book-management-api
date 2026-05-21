package com.example.bookmanagement.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var objectMapper: ObjectMapper

	@Test
	fun `書籍を登録できる`() {
		val authorId = createAuthor(name = "書籍登録著者")

		mockMvc.perform(
			post("/books")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "吾輩は猫である",
					  "price": 1200,
					  "publicationStatus": "PUBLISHED",
					  "authorIds": [$authorId]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.title").value("吾輩は猫である"))
			.andExpect(jsonPath("$.price").value(1200))
			.andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"))
			.andExpect(jsonPath("$.authors", hasSize<Any>(1)))
			.andExpect(jsonPath("$.authors[0].id").value(authorId))
	}

	@Test
	fun `複数著者の書籍を登録できる`() {
		val authorId = createAuthor(name = "共著著者A")
		val coAuthorId = createAuthor(name = "共著著者B")

		mockMvc.perform(
			post("/books")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "共著サンプル",
					  "price": 1500,
					  "publicationStatus": "UNPUBLISHED",
					  "authorIds": [$authorId, $coAuthorId]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.title").value("共著サンプル"))
			.andExpect(jsonPath("$.publicationStatus").value("UNPUBLISHED"))
			.andExpect(jsonPath("$.authors", hasSize<Any>(2)))
			.andExpect(jsonPath("$.authors[0].id").value(authorId))
			.andExpect(jsonPath("$.authors[1].id").value(coAuthorId))
	}

	@Test
	fun `書籍タイトルが空の場合は400を返す`() {
		val authorId = createAuthor()

		mockMvc.perform(
			post("/books")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "",
					  "price": 1200,
					  "publicationStatus": "PUBLISHED",
					  "authorIds": [$authorId]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Book title must not be blank"))
	}

	@Test
	fun `書籍価格がマイナスの場合は400を返す`() {
		val authorId = createAuthor()

		mockMvc.perform(
			post("/books")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "価格不正書籍",
					  "price": -1,
					  "publicationStatus": "PUBLISHED",
					  "authorIds": [$authorId]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Book price must be greater than or equal to 0"))
	}

	@Test
	fun `著者ID一覧が空の場合は400を返す`() {
		mockMvc.perform(
			post("/books")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "著者なし書籍",
					  "price": 1200,
					  "publicationStatus": "PUBLISHED",
					  "authorIds": []
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Book must have at least one author"))
	}

	@Test
	fun `著者ID一覧に重複がある場合は400を返す`() {
		val authorId = createAuthor()

		mockMvc.perform(
			post("/books")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "著者重複書籍",
					  "price": 1200,
					  "publicationStatus": "PUBLISHED",
					  "authorIds": [$authorId, $authorId]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Author ids must not contain duplicates"))
	}

	@Test
	fun `存在しない著者IDで書籍登録する場合は400を返す`() {
		val authorId = Long.MAX_VALUE

		mockMvc.perform(
			post("/books")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "存在しない著者の書籍",
					  "price": 1200,
					  "publicationStatus": "PUBLISHED",
					  "authorIds": [$authorId]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Specified author does not exist"))
	}

	@Test
	fun `書籍を更新できる`() {
		val authorId = createAuthor(name = "更新用著者")
		val bookId = createBook(authorIds = listOf(authorId))

		mockMvc.perform(
			put("/books/$bookId")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "更新後書籍",
					  "price": 1800,
					  "publicationStatus": "PUBLISHED",
					  "authorIds": [$authorId]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(bookId))
			.andExpect(jsonPath("$.title").value("更新後書籍"))
			.andExpect(jsonPath("$.price").value(1800))
			.andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"))
			.andExpect(jsonPath("$.authors", hasSize<Any>(1)))
			.andExpect(jsonPath("$.authors[0].id").value(authorId))
	}

	@Test
	fun `書籍の著者関連を更新できる`() {
		val originalAuthorId = createAuthor(name = "更新前著者")
		val newAuthorId = createAuthor(name = "更新後著者")
		val bookId = createBook(authorIds = listOf(originalAuthorId))

		mockMvc.perform(
			put("/books/$bookId")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "著者更新書籍",
					  "price": 1600,
					  "publicationStatus": "UNPUBLISHED",
					  "authorIds": [$newAuthorId]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(bookId))
			.andExpect(jsonPath("$.title").value("著者更新書籍"))
			.andExpect(jsonPath("$.authors", hasSize<Any>(1)))
			.andExpect(jsonPath("$.authors[0].id").value(newAuthorId))
	}

	@Test
	fun `存在しない書籍を更新する場合は404を返す`() {
		val authorId = createAuthor()
		val bookId = Long.MAX_VALUE

		mockMvc.perform(
			put("/books/$bookId")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "存在しない書籍",
					  "price": 1200,
					  "publicationStatus": "PUBLISHED",
					  "authorIds": [$authorId]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.message").value("Book not found: id=$bookId"))
	}

	@Test
	fun `出版済み書籍を未出版へ戻す場合は400を返す`() {
		val authorId = createAuthor()
		val bookId = createBook(
			publicationStatus = "PUBLISHED",
			authorIds = listOf(authorId),
		)

		mockMvc.perform(
			put("/books/$bookId")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "未出版戻し書籍",
					  "price": 1200,
					  "publicationStatus": "UNPUBLISHED",
					  "authorIds": [$authorId]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Published book cannot be changed to unpublished"))
	}

	@Test
	fun `書籍更新時にタイトルが空の場合は400を返す`() {
		val authorId = createAuthor()
		val bookId = createBook(authorIds = listOf(authorId))

		mockMvc.perform(
			put("/books/$bookId")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "",
					  "price": 1200,
					  "publicationStatus": "UNPUBLISHED",
					  "authorIds": [$authorId]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Book title must not be blank"))
	}

	@Test
	fun `書籍更新時に価格がマイナスの場合は400を返す`() {
		val authorId = createAuthor()
		val bookId = createBook(authorIds = listOf(authorId))

		mockMvc.perform(
			put("/books/$bookId")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "価格不正更新書籍",
					  "price": -1,
					  "publicationStatus": "UNPUBLISHED",
					  "authorIds": [$authorId]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Book price must be greater than or equal to 0"))
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
