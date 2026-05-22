package com.example.bookmanagement.repository

import com.example.bookmanagement.jooq.tables.records.BooksRecord
import com.example.bookmanagement.jooq.tables.references.BOOKS
import com.example.bookmanagement.jooq.tables.references.BOOK_AUTHORS
import com.example.bookmanagement.model.PublicationStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 書籍テーブルと書籍・著者中間テーブルへのDBアクセスを担当するRepository。
 *
 * 書籍本体の登録・更新・検索に加えて、書籍と著者の多対多関連の登録・削除も扱う。
 */
@Repository
class BookRepository(
	private val dsl: DSLContext,
) {
	/**
	 * 書籍を登録し、採番されたIDを含む登録後のレコードを返す。
	 */
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

	/**
	 * 書籍IDを指定して書籍レコードを1件取得する。
	 */
	fun findById(id: Long): BooksRecord? {
		return dsl.selectFrom(BOOKS)
			.where(BOOKS.ID.eq(id))
			.fetchOne()
	}

	/**
	 * 指定された著者に紐づく書籍一覧を取得する。
	 *
	 * 著者別書籍取得APIで利用するため、書籍ID昇順で返す。
	 */
	fun findBooksByAuthorId(authorId: Long): List<BooksRecord> {
		return dsl.select(
			BOOKS.ID,
			BOOKS.TITLE,
			BOOKS.PRICE,
			BOOKS.PUBLICATION_STATUS,
			BOOKS.CREATED_AT,
			BOOKS.UPDATED_AT,
		)
			.from(BOOKS)
			.join(BOOK_AUTHORS)
			.on(BOOK_AUTHORS.BOOK_ID.eq(BOOKS.ID))
			.where(BOOK_AUTHORS.AUTHOR_ID.eq(authorId))
			.orderBy(BOOKS.ID.asc())
			.fetchInto(BooksRecord::class.java)
	}

	/**
	 * 指定された書籍IDの書籍情報を更新し、更新後のレコードを返す。
	 *
	 * 対象が存在しない場合は `null` を返す。
	 */
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

	/**
	 * 指定された書籍に紐づく著者関連を削除する。
	 *
	 * 書籍更新時に、現在の関連を入れ替えるために利用する。
	 */
	fun deleteBookAuthors(bookId: Long) {
		dsl.deleteFrom(BOOK_AUTHORS)
			.where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
			.execute()
	}

	/**
	 * 指定された書籍に著者関連を登録する。
	 */
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
