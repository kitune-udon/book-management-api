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

@RestController
@RequestMapping("/books")
class BookController(
	private val bookService: BookService,
) {
	@PostMapping
	fun create(
		@Valid @RequestBody request: CreateBookRequest,
	): ResponseEntity<BookResponse> {
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(bookService.create(request))
	}

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
