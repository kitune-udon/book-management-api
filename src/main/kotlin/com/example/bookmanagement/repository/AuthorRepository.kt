package com.example.bookmanagement.repository

import com.example.bookmanagement.jooq.tables.records.AuthorsRecord
import com.example.bookmanagement.jooq.tables.references.AUTHORS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class AuthorRepository(
	private val dsl: DSLContext,
) {
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

	fun update(id: Long, name: String, birthDate: LocalDate): AuthorsRecord? {
		return dsl.update(AUTHORS)
			.set(AUTHORS.NAME, name)
			.set(AUTHORS.BIRTH_DATE, birthDate)
			.set(AUTHORS.UPDATED_AT, LocalDateTime.now())
			.where(AUTHORS.ID.eq(id))
			.returning()
			.fetchOne()
	}

	fun findById(id: Long): AuthorsRecord? {
		return dsl.selectFrom(AUTHORS)
			.where(AUTHORS.ID.eq(id))
			.fetchOne()
	}

	fun existsById(id: Long): Boolean {
		return dsl.fetchExists(
			dsl.selectOne()
				.from(AUTHORS)
				.where(AUTHORS.ID.eq(id)),
		)
	}
}
