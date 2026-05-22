package com.example.bookmanagement.controller

import com.example.bookmanagement.dto.book.BookResponse
import com.example.bookmanagement.dto.book.CreateBookRequest
import com.example.bookmanagement.dto.book.UpdateBookRequest
import com.example.bookmanagement.service.BookService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 書籍に関するREST APIを提供するController。
 *
 * 書籍の登録・更新リクエストを受け取り、Validation後の業務ルール検証や永続化はService層へ委譲する。
 * 書籍と著者の関連付けもリクエストの `authorIds` を通じて扱う。
 */
@RestController
@RequestMapping("/books")
class BookController(
	private val bookService: BookService,
) {
	/**
	 * 書籍を新規登録し、登録結果を `201 Created` で返す。
	 *
	 * `authorIds` で指定された著者との関連も同時に登録する。
	 */
	@PostMapping
	fun create(
		@Valid @RequestBody request: CreateBookRequest,
	): ResponseEntity<BookResponse> {
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(bookService.create(request))
	}

	/**
	 * 指定された書籍IDの書籍情報を更新し、更新後の内容を返す。
	 *
	 * `authorIds` の指定内容に合わせて、書籍と著者の関連も更新する。
	 */
	@PutMapping("/{bookId}")
	fun update(
		@PathVariable bookId: Long,
		@Valid @RequestBody request: UpdateBookRequest,
	): ResponseEntity<BookResponse> {
		return ResponseEntity.ok(
			bookService.update(bookId, request),
		)
	}
}
