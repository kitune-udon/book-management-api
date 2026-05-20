package com.example.bookmanagement.repository

import com.example.bookmanagement.jooq.tables.references.AUTHORS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AuthorRepository(
	private val dsl: DSLContext,
) {
	fun existsById(id: Long): Boolean {
		return dsl.fetchExists(
			dsl.selectOne()
				.from(AUTHORS)
				.where(AUTHORS.ID.eq(id)),
		)
	}
}
