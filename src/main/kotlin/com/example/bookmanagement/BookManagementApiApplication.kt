package com.example.bookmanagement

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BookManagementApiApplication

fun main(args: Array<String>) {
	runApplication<BookManagementApiApplication>(*args)
}
