package com.example.bookmanagement.repository

import com.example.bookmanagement.jooq.tables.records.AuthorsRecord
import com.example.bookmanagement.jooq.tables.references.AUTHORS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 著者テーブルへのDBアクセスを担当するRepository。
 *
 * jOOQの `DSLContext` を使い、Service層で必要な著者の登録・更新・検索を提供する。
 */
@Repository
class AuthorRepository(
	private val dsl: DSLContext,
) {
	/**
	 * 著者を登録し、採番されたIDを含む登録後のレコードを返す。
	 */
	fun insert(name: String, birthDate: LocalDate): AuthorsRecord {
		val now = LocalDateTime.now()

		return dsl.insertInto(AUTHORS)
			.set(AUTHORS.NAME, name)
			.set(AUTHORS.BIRTH_DATE, birthDate)
			.set(AUTHORS.CREATED_AT, now)
			.set(AUTHORS.UPDATED_AT, now)
			.returning()
			.fetchOne()!!
	}

	/**
	 * 指定された著者IDの著者情報を更新し、更新後のレコードを返す。
	 *
	 * 対象が存在しない場合は `null` を返す。
	 */
	fun update(id: Long, name: String, birthDate: LocalDate): AuthorsRecord? {
		return dsl.update(AUTHORS)
			.set(AUTHORS.NAME, name)
			.set(AUTHORS.BIRTH_DATE, birthDate)
			.set(AUTHORS.UPDATED_AT, LocalDateTime.now())
			.where(AUTHORS.ID.eq(id))
			.returning()
			.fetchOne()
	}

	/**
	 * 著者IDを指定して著者レコードを1件取得する。
	 */
	fun findById(id: Long): AuthorsRecord? {
		return dsl.selectFrom(AUTHORS)
			.where(AUTHORS.ID.eq(id))
			.fetchOne()
	}

	/**
	 * 複数の著者IDに対応する著者レコードを取得する。
	 */
	fun findByIds(ids: List<Long>): List<AuthorsRecord> {
		if (ids.isEmpty()) {
			return emptyList()
		}

		return dsl.selectFrom(AUTHORS)
			.where(AUTHORS.ID.`in`(ids))
			.fetch()
	}

	/**
	 * 指定された著者IDの存在有無を確認する。
	 */
	fun existsById(id: Long): Boolean {
		return dsl.fetchExists(
			dsl.selectOne()
				.from(AUTHORS)
				.where(AUTHORS.ID.eq(id)),
		)
	}
}
