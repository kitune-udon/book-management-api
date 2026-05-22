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
import java.time.Clock
import java.time.LocalDate

/**
 * 著者に関する業務処理を担当するService。
 *
 * Controllerから受け取ったリクエストに対し、業務ルール検証とトランザクション境界を担い、
 * DBアクセスはRepositoryへ委譲する。
 */
@Service
class AuthorService(
	private val authorRepository: AuthorRepository,
	private val clock: Clock,
) {
	/**
	 * 著者を新規登録する。
	 *
	 * 著者名は前後空白を除去して保存し、生年月日が未来日でないことを検証する。
	 */
	@Transactional
	fun create(request: CreateAuthorRequest): AuthorResponse {
		validateBirthDate(request.birthDate)

		val author = authorRepository.insert(
			name = request.name.trim(),
			birthDate = request.birthDate,
		)

		return author.toResponse()
	}

	/**
	 * 指定された著者IDの著者情報を更新する。
	 *
	 * 対象著者が存在しない場合は `NotFoundException` を投げる。
	 */
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

	/**
	 * 生年月日が現在日以前であることを検証する。
	 */
	private fun validateBirthDate(birthDate: LocalDate) {
		if (birthDate.isAfter(LocalDate.now(clock))) {
			throw BusinessRuleViolationException(
				"Author birth date must be today or past date",
			)
		}
	}

	/**
	 * jOOQの著者レコードをAPIレスポンスDTOへ変換する。
	 */
	private fun AuthorsRecord.toResponse(): AuthorResponse {
		return AuthorResponse(
			id = id!!,
			name = name!!,
			birthDate = birthDate!!,
		)
	}
}
