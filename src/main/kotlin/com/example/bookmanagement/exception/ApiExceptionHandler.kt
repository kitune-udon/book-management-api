package com.example.bookmanagement.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * API全体で発生した例外を共通エラーレスポンスへ変換するハンドラー。
 *
 * ControllerやServiceから投げられた例外をHTTPステータスと `ErrorResponse` に揃え、
 * 入力不正が想定外の500エラーとして返らないようにする。
 */
@RestControllerAdvice
class ApiExceptionHandler {

	/**
	 * 業務ルール違反を400 Bad Requestへ変換する。
	 */
	@ExceptionHandler(BusinessRuleViolationException::class)
	fun handleBusinessRuleViolationException(
		exception: BusinessRuleViolationException,
	): ResponseEntity<ErrorResponse> {
		return ResponseEntity
			.badRequest()
			.body(
				ErrorResponse(
					status = HttpStatus.BAD_REQUEST.value(),
					message = exception.message ?: "Bad request",
				),
		)
	}

	/**
	 * 指定されたリソースが存在しない場合の例外を404 Not Foundへ変換する。
	 */
	@ExceptionHandler(NotFoundException::class)
	fun handleNotFoundException(
		exception: NotFoundException,
	): ResponseEntity<ErrorResponse> {
		return ResponseEntity
			.status(HttpStatus.NOT_FOUND)
			.body(
				ErrorResponse(
					status = HttpStatus.NOT_FOUND.value(),
					message = exception.message ?: "Resource not found",
				),
		)
	}

	/**
	 * JSON構文不正、日付形式不正、enum不正、型不一致などのリクエストボディ不正を400へ変換する。
	 */
	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleHttpMessageNotReadableException(
		exception: HttpMessageNotReadableException,
	): ResponseEntity<ErrorResponse> {
		return ResponseEntity
			.badRequest()
			.body(
				ErrorResponse(
					status = HttpStatus.BAD_REQUEST.value(),
					message = "Invalid request body",
				),
		)
	}

	/**
	 * パスパラメータの型不一致を400 Bad Requestへ変換する。
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException::class)
	fun handleMethodArgumentTypeMismatchException(
		exception: MethodArgumentTypeMismatchException,
	): ResponseEntity<ErrorResponse> {
		return ResponseEntity
			.badRequest()
			.body(
				ErrorResponse(
					status = HttpStatus.BAD_REQUEST.value(),
					message = "Invalid path parameter",
				),
		)
	}

	/**
	 * Bean Validationの入力エラーを400 Bad Requestへ変換する。
	 */
	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleMethodArgumentNotValidException(
		exception: MethodArgumentNotValidException,
	): ResponseEntity<ErrorResponse> {
		val message = exception.bindingResult
			.fieldErrors
			.firstOrNull()
			?.toErrorMessage()
			?: "Invalid request parameters"

		return ResponseEntity
			.badRequest()
			.body(
				ErrorResponse(
					status = HttpStatus.BAD_REQUEST.value(),
					message = message,
				),
		)
	}

	/**
	 * 想定外の例外を500 Internal Server Errorへ変換する。
	 */
	@ExceptionHandler(Exception::class)
	fun handleException(
		exception: Exception,
	): ResponseEntity<ErrorResponse> {
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(
				ErrorResponse(
					status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
					message = "Internal server error",
				),
		)
	}

	/**
	 * Validationエラーの先頭項目から、レスポンスに返すメッセージを組み立てる。
	 */
	private fun FieldError.toErrorMessage(): String {
		return defaultMessage ?: "Invalid request parameter: $field"
	}
}
