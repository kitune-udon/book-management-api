package com.example.bookmanagement.service

import com.example.bookmanagement.dto.book.CreateBookRequest
import com.example.bookmanagement.dto.book.UpdateBookRequest
import com.example.bookmanagement.exception.BusinessRuleViolationException
import com.example.bookmanagement.jooq.tables.records.AuthorsRecord
import com.example.bookmanagement.jooq.tables.records.BooksRecord
import com.example.bookmanagement.model.PublicationStatus
import com.example.bookmanagement.repository.AuthorRepository
import com.example.bookmanagement.repository.BookRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 書籍Serviceの単体テスト。
 *
 * Controllerを経由せず、Service層が担当する著者ID検証と出版状態遷移ルールを直接確認する。
 */
class BookServiceTest {

	private val bookRepository = mock(BookRepository::class.java)
	private val authorRepository = mock(AuthorRepository::class.java)
	private val bookService = BookService(bookRepository, authorRepository)

	@Test
	fun `出版済み書籍を未出版へ戻す更新はできない`() {
		val bookId = 1L
		val authorId = 10L
		`when`(bookRepository.findById(bookId)).thenReturn(
			BooksRecord(
				id = bookId,
				title = "出版済み書籍",
				price = 1200,
				publicationStatus = PublicationStatus.PUBLISHED.name,
			),
		)
		`when`(authorRepository.findByIds(listOf(authorId))).thenReturn(
			listOf(authorRecord(authorId)),
		)

		val exception = assertFailsWith<BusinessRuleViolationException> {
			bookService.update(
				bookId,
				UpdateBookRequest(
					title = "未出版戻し書籍",
					price = 1200,
					publicationStatus = PublicationStatus.UNPUBLISHED,
					authorIds = listOf(authorId),
				),
			)
		}

		assertEquals("Published book cannot be changed to unpublished", exception.message)
	}

	@Test
	fun `authorIdsに重複がある場合は登録できない`() {
		val exception = assertFailsWith<BusinessRuleViolationException> {
			bookService.create(
				CreateBookRequest(
					title = "著者重複書籍",
					price = 1200,
					publicationStatus = PublicationStatus.PUBLISHED,
					authorIds = listOf(1L, 1L),
				),
			)
		}

		assertEquals("Author ids must not contain duplicates", exception.message)
	}

	@Test
	fun `存在しない著者IDを指定した書籍登録はできない`() {
		val authorIds = listOf(1L, 2L)
		`when`(authorRepository.findByIds(authorIds)).thenReturn(
			listOf(authorRecord(1L)),
		)

		val exception = assertFailsWith<BusinessRuleViolationException> {
			bookService.create(
				CreateBookRequest(
					title = "存在しない著者の書籍",
					price = 1200,
					publicationStatus = PublicationStatus.PUBLISHED,
					authorIds = authorIds,
				),
			)
		}

		assertEquals("Specified author does not exist", exception.message)
	}

	private fun authorRecord(id: Long): AuthorsRecord {
		return AuthorsRecord(
			id = id,
			name = "テスト著者$id",
			birthDate = LocalDate.of(1900, 1, 1),
		)
	}
}
