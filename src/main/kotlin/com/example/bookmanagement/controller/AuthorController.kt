package com.example.bookmanagement.controller

import com.example.bookmanagement.dto.author.AuthorResponse
import com.example.bookmanagement.dto.author.CreateAuthorRequest
import com.example.bookmanagement.dto.author.UpdateAuthorRequest
import com.example.bookmanagement.service.AuthorService
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
@RequestMapping("/authors")
class AuthorController(
	private val authorService: AuthorService,
) {
	@PostMapping
	fun create(
		@Valid @RequestBody request: CreateAuthorRequest,
	): ResponseEntity<AuthorResponse> {
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(authorService.create(request))
	}

	@PutMapping("/{authorId}")
	fun update(
		@PathVariable authorId: Long,
		@Valid @RequestBody request: UpdateAuthorRequest,
	): ResponseEntity<AuthorResponse> {
		return ResponseEntity.ok(
			authorService.update(authorId, request),
		)
	}
}
