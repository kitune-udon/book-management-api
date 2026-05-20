package com.example.bookmanagement.service

import com.example.bookmanagement.dto.author.AuthorResponse
import com.example.bookmanagement.dto.book.BookResponse
import com.example.bookmanagement.dto.book.CreateBookRequest
import com.example.bookmanagement.exception.BusinessRuleViolationException
import com.example.bookmanagement.jooq.tables.records.AuthorsRecord
import com.example.bookmanagement.jooq.tables.records.BooksRecord
import com.example.bookmanagement.model.PublicationStatus
import com.example.bookmanagement.repository.AuthorRepository
import com.example.bookmanagement.repository.BookRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(
	private val bookRepository: BookRepository,
	private val authorRepository: AuthorRepository,
) {
	@Transactional
	fun create(request: CreateBookRequest): BookResponse {
		validateAuthorIds(request.authorIds)

		val authors = authorRepository.findByIds(request.authorIds)
		validateAllAuthorsExist(
			authorIds = request.authorIds,
			authors = authors,
		)

		val book = bookRepository.insert(
			title = request.title.trim(),
			price = request.price,
			publicationStatus = request.publicationStatus,
		)

		bookRepository.insertBookAuthors(
			bookId = book.id!!,
			authorIds = request.authorIds,
		)

		val sortedAuthors = sortAuthorsByRequestOrder(
			authorIds = request.authorIds,
			authors = authors,
		)

		return book.toResponse(sortedAuthors)
	}

	private fun validateAuthorIds(authorIds: List<Long>) {
		if (authorIds.isEmpty()) {
			throw BusinessRuleViolationException("Book must have at least one author")
		}

		if (authorIds.size != authorIds.toSet().size) {
			throw BusinessRuleViolationException("Author ids must not contain duplicates")
		}
	}

	private fun validateAllAuthorsExist(
		authorIds: List<Long>,
		authors: List<AuthorsRecord>,
	) {
		if (authorIds.size != authors.size) {
			throw BusinessRuleViolationException("Specified author does not exist")
		}
	}

	private fun sortAuthorsByRequestOrder(
		authorIds: List<Long>,
		authors: List<AuthorsRecord>,
	): List<AuthorsRecord> {
		val authorMap = authors.associateBy { it.id!! }
		return authorIds.mapNotNull { authorMap[it] }
	}

	private fun BooksRecord.toResponse(authors: List<AuthorsRecord>): BookResponse {
		return BookResponse(
			id = id!!,
			title = title!!,
			price = price!!,
			publicationStatus = PublicationStatus.valueOf(publicationStatus!!),
			authors = authors.map { it.toResponse() },
		)
	}

	private fun AuthorsRecord.toResponse(): AuthorResponse {
		return AuthorResponse(
			id = id!!,
			name = name!!,
			birthDate = birthDate!!,
		)
	}
}
