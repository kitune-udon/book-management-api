package com.example.bookmanagement.controller

import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BookControllerTest : ControllerTestSupport() {

	@Test
	fun `B-01 書籍を登録できる`() {
		val authorId = createAuthor(name = "書籍登録著者")

		postJson(
			"/books",
			"""
			{
			  "title": "吾輩は猫である",
			  "price": 1200,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
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
	fun `B-02 複数著者の書籍を登録できる`() {
		val authorId = createAuthor(name = "共著著者A")
		val coAuthorId = createAuthor(name = "共著著者B")

		postJson(
			"/books",
			"""
			{
			  "title": "共著サンプル",
			  "price": 1500,
			  "publicationStatus": "UNPUBLISHED",
			  "authorIds": [$authorId, $coAuthorId]
			}
			""",
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.title").value("共著サンプル"))
			.andExpect(jsonPath("$.publicationStatus").value("UNPUBLISHED"))
			.andExpect(jsonPath("$.authors", hasSize<Any>(2)))
			.andExpect(jsonPath("$.authors[0].id").value(authorId))
			.andExpect(jsonPath("$.authors[1].id").value(coAuthorId))
	}

	@Test
	fun `B-03 書籍名が空の場合は登録できない`() {
		val authorId = createAuthor()

		postJson(
			"/books",
			"""
			{
			  "title": "",
			  "price": 1200,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Book title must not be blank")
	}

	@Test
	fun `B-04 価格がマイナスの場合は登録できない`() {
		val authorId = createAuthor()

		postJson(
			"/books",
			"""
			{
			  "title": "価格不正書籍",
			  "price": -1,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Book price must be greater than or equal to 0")
	}

	@Test
	fun `B-05 価格が0の場合は登録できる`() {
		val authorId = createAuthor()

		postJson(
			"/books",
			"""
			{
			  "title": "価格0書籍",
			  "price": 0,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.title").value("価格0書籍"))
			.andExpect(jsonPath("$.price").value(0))
	}

	@Test
	fun `B-06 authorIdsが空の場合は登録できない`() {
		postJson(
			"/books",
			"""
			{
			  "title": "著者なし書籍",
			  "price": 1200,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": []
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Book must have at least one author")
	}

	@Test
	fun `B-07 authorIdsに重複がある場合は登録できない`() {
		val authorId = createAuthor()

		postJson(
			"/books",
			"""
			{
			  "title": "著者重複書籍",
			  "price": 1200,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId, $authorId]
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Author ids must not contain duplicates")
	}

	@Test
	fun `B-08 存在しない著者IDを指定すると登録できない`() {
		val authorId = Long.MAX_VALUE

		postJson(
			"/books",
			"""
			{
			  "title": "存在しない著者の書籍",
			  "price": 1200,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Specified author does not exist")
	}

	@Test
	fun `B-09 UNPUBLISHEDの書籍を登録できる`() {
		val authorId = createAuthor()

		postJson(
			"/books",
			"""
			{
			  "title": "未出版登録書籍",
			  "price": 1200,
			  "publicationStatus": "UNPUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.publicationStatus").value("UNPUBLISHED"))
	}

	@Test
	fun `B-10 PUBLISHEDの書籍を登録できる`() {
		val authorId = createAuthor()

		postJson(
			"/books",
			"""
			{
			  "title": "出版済み登録書籍",
			  "price": 1200,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"))
	}

	@Test
	fun `B-11 書籍を更新できる`() {
		val authorId = createAuthor(name = "更新用著者")
		val bookId = createBook(
			publicationStatus = "PUBLISHED",
			authorIds = listOf(authorId),
		)

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "更新後書籍",
			  "price": 1800,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
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
	fun `B-12 書籍更新時に著者関連を更新できる`() {
		val originalAuthorId = createAuthor(name = "更新前著者")
		val newAuthorId = createAuthor(name = "更新後著者")
		val bookId = createBook(
			publicationStatus = "PUBLISHED",
			authorIds = listOf(originalAuthorId),
		)

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "著者更新書籍",
			  "price": 1600,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$newAuthorId]
			}
			""",
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(bookId))
			.andExpect(jsonPath("$.title").value("著者更新書籍"))
			.andExpect(jsonPath("$.authors", hasSize<Any>(1)))
			.andExpect(jsonPath("$.authors[0].id").value(newAuthorId))
	}

	@Test
	fun `B-13 存在しない書籍IDを更新すると404になる`() {
		val authorId = createAuthor()
		val bookId = Long.MAX_VALUE

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "存在しない書籍",
			  "price": 1200,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isNotFound)
			.andExpectErrorBody(404, "Book not found: id=$bookId")
	}

	@Test
	fun `B-14 価格がマイナスの場合は更新できない`() {
		val authorId = createAuthor()
		val bookId = createBook(authorIds = listOf(authorId))

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "価格不正更新書籍",
			  "price": -1,
			  "publicationStatus": "UNPUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Book price must be greater than or equal to 0")
	}

	@Test
	fun `B-15 authorIdsが空の場合は更新できない`() {
		val authorId = createAuthor()
		val bookId = createBook(authorIds = listOf(authorId))

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "著者なし更新書籍",
			  "price": 1200,
			  "publicationStatus": "UNPUBLISHED",
			  "authorIds": []
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Book must have at least one author")
	}

	@Test
	fun `B-16 authorIdsに重複がある場合は更新できない`() {
		val authorId = createAuthor()
		val bookId = createBook(authorIds = listOf(authorId))

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "著者重複更新書籍",
			  "price": 1200,
			  "publicationStatus": "UNPUBLISHED",
			  "authorIds": [$authorId, $authorId]
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Author ids must not contain duplicates")
	}

	@Test
	fun `B-17 存在しない著者IDを指定すると更新できない`() {
		val authorId = createAuthor()
		val bookId = createBook(authorIds = listOf(authorId))

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "存在しない著者更新書籍",
			  "price": 1200,
			  "publicationStatus": "UNPUBLISHED",
			  "authorIds": [${Long.MAX_VALUE}]
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Specified author does not exist")
	}

	@Test
	fun `B-18 UNPUBLISHEDからPUBLISHEDへ更新できる`() {
		val authorId = createAuthor()
		val bookId = createBook(
			publicationStatus = "UNPUBLISHED",
			authorIds = listOf(authorId),
		)

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "出版済み更新書籍",
			  "price": 1200,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(bookId))
			.andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"))
	}

	@Test
	fun `B-19 PUBLISHEDからUNPUBLISHEDへ更新できない`() {
		val authorId = createAuthor()
		val bookId = createBook(
			publicationStatus = "PUBLISHED",
			authorIds = listOf(authorId),
		)

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "未出版戻し書籍",
			  "price": 1200,
			  "publicationStatus": "UNPUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Published book cannot be changed to unpublished")
	}

	@Test
	fun `B-20 PUBLISHEDからPUBLISHEDへ更新できる`() {
		val authorId = createAuthor()
		val bookId = createBook(
			publicationStatus = "PUBLISHED",
			authorIds = listOf(authorId),
		)

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "出版済み維持書籍",
			  "price": 1300,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(bookId))
			.andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"))
	}

	@Test
	fun `B-21 UNPUBLISHEDからUNPUBLISHEDへ更新できる`() {
		val authorId = createAuthor()
		val bookId = createBook(
			publicationStatus = "UNPUBLISHED",
			authorIds = listOf(authorId),
		)

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "未出版維持書籍",
			  "price": 1300,
			  "publicationStatus": "UNPUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(bookId))
			.andExpect(jsonPath("$.publicationStatus").value("UNPUBLISHED"))
	}

	@Test
	fun `B-22 publicationStatusが定義外の場合は登録できない`() {
		val authorId = createAuthor()

		postJson(
			"/books",
			"""
			{
			  "title": "出版状態不正書籍",
			  "price": 1200,
			  "publicationStatus": "DRAFT",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid request body")
	}

	@Test
	fun `B-23 priceが数値でない場合は登録できない`() {
		val authorId = createAuthor()

		postJson(
			"/books",
			"""
			{
			  "title": "価格型不正書籍",
			  "price": "abc",
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid request body")
	}

	@Test
	fun `B-24 JSON構文不正の場合は登録できない`() {
		val authorId = createAuthor()

		postJson(
			"/books",
			"""
			{
			  "title": "JSON不正書籍",
			  "price": 1200,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId],
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid request body")
	}

	@Test
	fun `B-25 authorIdsが未指定の場合は登録できない`() {
		postJson(
			"/books",
			"""
			{
			  "title": "著者未指定書籍",
			  "price": 1200,
			  "publicationStatus": "PUBLISHED"
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid request body")
	}

	@Test
	fun `B-26 authorIdsがnullの場合は登録できない`() {
		postJson(
			"/books",
			"""
			{
			  "title": "著者null書籍",
			  "price": 1200,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": null
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid request body")
	}

	@Test
	fun `B-27 bookIdが数値でない場合は400になる`() {
		val authorId = createAuthor()

		putJson(
			"/books/abc",
			"""
			{
			  "title": "パス不正更新書籍",
			  "price": 1200,
			  "publicationStatus": "PUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid path parameter")
	}

	@Test
	fun `B-28 publicationStatusが定義外の場合は更新できない`() {
		val authorId = createAuthor()
		val bookId = createBook(authorIds = listOf(authorId))

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "出版状態不正更新書籍",
			  "price": 1200,
			  "publicationStatus": "DRAFT",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid request body")
	}

	@Test
	fun `B-29 priceが数値でない場合は更新できない`() {
		val authorId = createAuthor()
		val bookId = createBook(authorIds = listOf(authorId))

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "価格型不正更新書籍",
			  "price": "abc",
			  "publicationStatus": "UNPUBLISHED",
			  "authorIds": [$authorId]
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid request body")
	}

	@Test
	fun `B-30 JSON構文不正の場合は更新できない`() {
		val authorId = createAuthor()
		val bookId = createBook(authorIds = listOf(authorId))

		putJson(
			"/books/$bookId",
			"""
			{
			  "title": "JSON不正更新書籍",
			  "price": 1200,
			  "publicationStatus": "UNPUBLISHED",
			  "authorIds": [$authorId],
			}
			""",
		)
			.andExpect(status().isBadRequest)
			.andExpectErrorBody(400, "Invalid request body")
	}

}
