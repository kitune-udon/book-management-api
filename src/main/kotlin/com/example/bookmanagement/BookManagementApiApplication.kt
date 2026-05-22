package com.example.bookmanagement

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Book Management APIのSpring Bootアプリケーション起動クラス。
 */
@SpringBootApplication
class BookManagementApiApplication

/**
 * アプリケーションを起動するエントリポイント。
 */
fun main(args: Array<String>) {
	runApplication<BookManagementApiApplication>(*args)
}
