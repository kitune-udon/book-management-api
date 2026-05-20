package com.example.bookmanagement.repository

import com.example.bookmanagement.jooq.tables.records.BooksRecord
import com.example.bookmanagement.jooq.tables.references.BOOKS
import com.example.bookmanagement.jooq.tables.references.BOOK_AUTHORS
import com.example.bookmanagement.model.PublicationStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BookRepository(
	private val dsl: DSLContext,
) {
	fun insert(
		title: String,
		price: Int,
		publicationStatus: PublicationStatus,
	): BooksRecord {
		val now = LocalDateTime.now()

		return dsl.insertInto(BOOKS)
			.set(BOOKS.TITLE, title)
			.set(BOOKS.PRICE, price)
			.set(BOOKS.PUBLICATION_STATUS, publicationStatus.name)
			.set(BOOKS.CREATED_AT, now)
			.set(BOOKS.UPDATED_AT, now)
			.returning()
			.fetchOne()!!
	}

	fun findById(id: Long): BooksRecord? {
		return dsl.selectFrom(BOOKS)
			.where(BOOKS.ID.eq(id))
			.fetchOne()
	}

	fun update(
		id: Long,
		title: String,
		price: Int,
		publicationStatus: PublicationStatus,
	): BooksRecord? {
		return dsl.update(BOOKS)
			.set(BOOKS.TITLE, title)
			.set(BOOKS.PRICE, price)
			.set(BOOKS.PUBLICATION_STATUS, publicationStatus.name)
			.set(BOOKS.UPDATED_AT, LocalDateTime.now())
			.where(BOOKS.ID.eq(id))
			.returning()
			.fetchOne()
	}

	fun deleteBookAuthors(bookId: Long) {
		dsl.deleteFrom(BOOK_AUTHORS)
			.where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
			.execute()
	}

	fun insertBookAuthors(bookId: Long, authorIds: List<Long>) {
		if (authorIds.isEmpty()) {
			return
		}

		val now = LocalDateTime.now()

		authorIds.forEach { authorId ->
			dsl.insertInto(BOOK_AUTHORS)
				.set(BOOK_AUTHORS.BOOK_ID, bookId)
				.set(BOOK_AUTHORS.AUTHOR_ID, authorId)
				.set(BOOK_AUTHORS.CREATED_AT, now)
				.execute()
		}
	}
}
