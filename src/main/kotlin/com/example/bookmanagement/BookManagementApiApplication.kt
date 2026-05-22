package com.example.bookmanagement

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.Clock

/**
 * Book Management APIのSpring Bootアプリケーション起動クラス。
 */
@SpringBootApplication
class BookManagementApiApplication {
	/**
	 * 日付に依存する業務ルールで利用するClock。
	 *
	 * テストでは固定Clockに差し替えることで、現在日判定を安定して検証できる。
	 */
	@Bean
	fun clock(): Clock {
		return Clock.systemDefaultZone()
	}
}

/**
 * アプリケーションを起動するエントリポイント。
 */
fun main(args: Array<String>) {
	runApplication<BookManagementApiApplication>(*args)
}
