package com.example.bookmanagement.service

import com.example.bookmanagement.dto.author.AuthorResponse
import com.example.bookmanagement.dto.book.BookResponse
import com.example.bookmanagement.dto.book.BookSummaryResponse
import com.example.bookmanagement.dto.book.CreateBookRequest
import com.example.bookmanagement.dto.book.UpdateBookRequest
import com.example.bookmanagement.exception.BusinessRuleViolationException
import com.example.bookmanagement.exception.NotFoundException
import com.example.bookmanagement.jooq.tables.records.AuthorsRecord
import com.example.bookmanagement.jooq.tables.records.BooksRecord
import com.example.bookmanagement.model.PublicationStatus
import com.example.bookmanagement.repository.AuthorRepository
import com.example.bookmanagement.repository.BookRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 書籍に関する業務処理を担当するService。
 *
 * 書籍本体と著者関連の整合性を同一トランザクション内で保ち、
 * 著者IDの妥当性や出版状態の遷移制約などの業務ルールを検証する。
 */
@Service
class BookService(
	private val bookRepository: BookRepository,
	private val authorRepository: AuthorRepository,
) {
	/**
	 * 書籍を新規登録し、指定された著者との関連も同時に登録する。
	 *
	 * 書籍名は前後空白を除去して保存し、レスポンスの著者一覧はリクエストの指定順に揃える。
	 */
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

	/**
	 * 指定された書籍IDの書籍情報と著者関連を更新する。
	 *
	 * 対象書籍が存在しない場合は `NotFoundException` を投げる。
	 */
	@Transactional
	fun update(bookId: Long, request: UpdateBookRequest): BookResponse {
		val currentBook = bookRepository.findById(bookId)
			?: throw NotFoundException("Book not found: id=$bookId")

		validateAuthorIds(request.authorIds)

		val authors = authorRepository.findByIds(request.authorIds)
		validateAllAuthorsExist(
			authorIds = request.authorIds,
			authors = authors,
		)

		validatePublicationStatusTransition(
			currentStatus = PublicationStatus.valueOf(currentBook.publicationStatus!!),
			requestedStatus = request.publicationStatus,
		)

		val updatedBook = bookRepository.update(
			id = bookId,
			title = request.title.trim(),
			price = request.price,
			publicationStatus = request.publicationStatus,
		) ?: throw NotFoundException("Book not found: id=$bookId")

		bookRepository.deleteBookAuthors(bookId)
		bookRepository.insertBookAuthors(
			bookId = bookId,
			authorIds = request.authorIds,
		)

		val sortedAuthors = sortAuthorsByRequestOrder(
			authorIds = request.authorIds,
			authors = authors,
		)

		return updatedBook.toResponse(sortedAuthors)
	}

	/**
	 * 指定された著者に紐づく書籍一覧を取得する。
	 *
	 * 著者が存在しない場合は `NotFoundException` を投げ、存在するが書籍がない場合は空配列を返す。
	 */
	@Transactional(readOnly = true)
	fun findBooksByAuthorId(authorId: Long): List<BookSummaryResponse> {
		if (!authorRepository.existsById(authorId)) {
			throw NotFoundException("Author not found: id=$authorId")
		}

		return bookRepository.findBooksByAuthorId(authorId)
			.map { it.toSummaryResponse() }
	}

	/**
	 * 書籍に紐づける著者ID一覧の基本ルールを検証する。
	 */
	private fun validateAuthorIds(authorIds: List<Long>) {
		if (authorIds.isEmpty()) {
			throw BusinessRuleViolationException("Book must have at least one author")
		}

		if (authorIds.size != authorIds.toSet().size) {
			throw BusinessRuleViolationException("Author ids must not contain duplicates")
		}
	}

	/**
	 * 出版状態の遷移ルールを検証する。
	 *
	 * 出版済みの書籍を未出版へ戻すことは許可しない。
	 */
	private fun validatePublicationStatusTransition(
		currentStatus: PublicationStatus,
		requestedStatus: PublicationStatus,
	) {
		if (
			currentStatus == PublicationStatus.PUBLISHED &&
			requestedStatus == PublicationStatus.UNPUBLISHED
		) {
			throw BusinessRuleViolationException(
				"Published book cannot be changed to unpublished",
			)
		}
	}

	/**
	 * 指定された著者IDがすべて存在することを検証する。
	 */
	private fun validateAllAuthorsExist(
		authorIds: List<Long>,
		authors: List<AuthorsRecord>,
	) {
		if (authorIds.size != authors.size) {
			throw BusinessRuleViolationException("Specified author does not exist")
		}
	}

	/**
	 * Repositoryから取得した著者一覧を、リクエストで指定された著者ID順に並べ替える。
	 */
	private fun sortAuthorsByRequestOrder(
		authorIds: List<Long>,
		authors: List<AuthorsRecord>,
	): List<AuthorsRecord> {
		val authorMap = authors.associateBy { it.id!! }
		return authorIds.mapNotNull { authorMap[it] }
	}

	/**
	 * jOOQの書籍レコードと著者レコードを、詳細レスポンスDTOへ変換する。
	 */
	private fun BooksRecord.toResponse(authors: List<AuthorsRecord>): BookResponse {
		return BookResponse(
			id = id!!,
			title = title!!,
			price = price!!,
			publicationStatus = PublicationStatus.valueOf(publicationStatus!!),
			authors = authors.map { it.toResponse() },
		)
	}

	/**
	 * jOOQの書籍レコードを、一覧用の簡易レスポンスDTOへ変換する。
	 */
	private fun BooksRecord.toSummaryResponse(): BookSummaryResponse {
		return BookSummaryResponse(
			id = id!!,
			title = title!!,
			price = price!!,
			publicationStatus = PublicationStatus.valueOf(publicationStatus!!),
		)
	}

	/**
	 * jOOQの著者レコードを、著者レスポンスDTOへ変換する。
	 */
	private fun AuthorsRecord.toResponse(): AuthorResponse {
		return AuthorResponse(
			id = id!!,
			name = name!!,
			birthDate = birthDate!!,
		)
	}
}
