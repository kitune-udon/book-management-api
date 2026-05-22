package com.example.bookmanagement.controller

import com.example.bookmanagement.dto.author.AuthorResponse
import com.example.bookmanagement.dto.author.CreateAuthorRequest
import com.example.bookmanagement.dto.author.UpdateAuthorRequest
import com.example.bookmanagement.dto.book.BookSummaryResponse
import com.example.bookmanagement.service.AuthorService
import com.example.bookmanagement.service.BookService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

/**
 * 著者に関するREST APIを提供するController。
 *
 * 著者の登録・更新に加えて、課題要件である「指定著者に紐づく書籍一覧取得」も扱う。
 * 業務ルールの検証やDBアクセスはService層へ委譲し、このクラスではHTTPリクエストとレスポンスの変換に責務を絞る。
 */
@RestController
@RequestMapping("/authors")
class AuthorController(
	private val authorService: AuthorService,
	private val bookService: BookService,
) {
	/**
	 * 著者を新規登録し、登録結果を `201 Created` で返す。
	 */
	@PostMapping
	fun create(
		@Valid @RequestBody request: CreateAuthorRequest,
	): ResponseEntity<AuthorResponse> {
		val response = authorService.create(request)

		return ResponseEntity
			.created(URI.create("/authors/${response.id}"))
			.body(response)
	}

	/**
	 * 指定された著者IDの著者情報を更新し、更新後の内容を返す。
	 */
	@PutMapping("/{authorId}")
	fun update(
		@PathVariable authorId: Long,
		@Valid @RequestBody request: UpdateAuthorRequest,
	): ResponseEntity<AuthorResponse> {
		return ResponseEntity.ok(
			authorService.update(authorId, request),
		)
	}

	/**
	 * 指定された著者に紐づく書籍一覧を取得する。
	 *
	 * 書籍一覧用途のため、各書籍に著者一覧は含めない。
	 */
	@GetMapping("/{authorId}/books")
	fun findBooksByAuthorId(
		@PathVariable authorId: Long,
	): ResponseEntity<List<BookSummaryResponse>> {
		return ResponseEntity.ok(
			bookService.findBooksByAuthorId(authorId),
		)
	}
}
