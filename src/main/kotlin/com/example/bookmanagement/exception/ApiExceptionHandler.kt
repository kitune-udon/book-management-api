package com.example.bookmanagement.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class ApiExceptionHandler {

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

	private fun FieldError.toErrorMessage(): String {
		return defaultMessage ?: "Invalid request parameter: $field"
	}
}
