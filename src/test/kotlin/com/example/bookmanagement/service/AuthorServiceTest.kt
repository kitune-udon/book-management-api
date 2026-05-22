package com.example.bookmanagement.service

import com.example.bookmanagement.dto.author.CreateAuthorRequest
import com.example.bookmanagement.exception.BusinessRuleViolationException
import com.example.bookmanagement.repository.AuthorRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 著者Serviceの単体テスト。
 *
 * Controllerを経由せず、Service層が担当する生年月日の業務ルールを直接確認する。
 */
class AuthorServiceTest {

	private val authorRepository = mock(AuthorRepository::class.java)
	private val fixedClock = Clock.fixed(
		Instant.parse("2026-05-22T00:00:00Z"),
		ZoneId.of("Asia/Tokyo"),
	)
	private val authorService = AuthorService(authorRepository, fixedClock)

	@Test
	fun `生年月日が未来日の場合は登録できない`() {
		val exception = assertFailsWith<BusinessRuleViolationException> {
			authorService.create(
				CreateAuthorRequest(
					name = "未来著者",
					birthDate = LocalDate.of(2026, 5, 23),
				),
			)
		}

		assertEquals("Author birth date must be today or past date", exception.message)
	}
}
