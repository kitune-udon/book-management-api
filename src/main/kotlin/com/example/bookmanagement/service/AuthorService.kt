package com.example.bookmanagement.service

import com.example.bookmanagement.dto.author.AuthorResponse
import com.example.bookmanagement.dto.author.CreateAuthorRequest
import com.example.bookmanagement.dto.author.UpdateAuthorRequest
import com.example.bookmanagement.exception.BusinessRuleViolationException
import com.example.bookmanagement.exception.NotFoundException
import com.example.bookmanagement.jooq.tables.records.AuthorsRecord
import com.example.bookmanagement.repository.AuthorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AuthorService(
	private val authorRepository: AuthorRepository,
) {
	@Transactional
	fun create(request: CreateAuthorRequest): AuthorResponse {
		validateBirthDate(request.birthDate)

		val author = authorRepository.insert(
			name = request.name.trim(),
			birthDate = request.birthDate,
		)

		return author.toResponse()
	}

	@Transactional
	fun update(authorId: Long, request: UpdateAuthorRequest): AuthorResponse {
		validateBirthDate(request.birthDate)

		if (!authorRepository.existsById(authorId)) {
			throw NotFoundException("Author not found: id=$authorId")
		}

		val author = authorRepository.update(
			id = authorId,
			name = request.name.trim(),
			birthDate = request.birthDate,
		) ?: throw NotFoundException("Author not found: id=$authorId")

		return author.toResponse()
	}

	private fun validateBirthDate(birthDate: LocalDate) {
		if (birthDate.isAfter(LocalDate.now())) {
			throw BusinessRuleViolationException(
				"Author birth date must be today or past date",
			)
		}
	}

	private fun AuthorsRecord.toResponse(): AuthorResponse {
		return AuthorResponse(
			id = id!!,
			name = name!!,
			birthDate = birthDate!!,
		)
	}
}
