# 4日目 共通基盤 / 著者API実装 作業メモ

## 2026-05-20

### Step 1 developブランチ最新化

- 作業開始時のブランチは `develop`。
- 作業開始時点の最新commit:

```text
0ad2970 docs: update day3 setup log
```

- 作業開始時点で以下の差分・未追跡ファイルが存在。
  - `M document/business_rules_design.md`
  - `?? document/develop/作業手順/coding_test_day_2_project_setup_steps.md`
  - `?? document/develop/作業手順/coding_test_day_3_db_flyway_jooq_steps.md`
  - `?? document/develop/作業手順/coding_test_day_4_common_foundation_author_api_steps.md`
- `document/business_rules_design.md` は、Day3/Day4方針に合わせて生年月日の未来日チェックをService層に統一した設計書修正。
- 手順書3件はユーザー提供の作業手順書として扱い、このStepでは変更・ステージングしない。
- `git pull origin develop` を実行。
- 結果: `Already up to date.`
- `develop` は `origin/develop` と同期済み。

### Step 2 PostgreSQLコンテナ起動

- `docker compose up -d` を実行。
- `book-management-postgres` は既に起動中だったため、状態は `Running`。
- `docker compose ps` で以下を確認。
  - container: `book-management-postgres`
  - image: `postgres:16`
  - status: `Up`
  - port: `0.0.0.0:5432->5432/tcp`
- `docker compose logs --tail=20 postgres` で以下を確認。
  - PostgreSQL: `16.14`
  - `database system is ready to accept connections`

### Step 3 jOOQコード生成・ビルド確認

- 実装着手前の健全性確認として `./gradlew clean generateJooq build` を実行。
- jOOQ codegenログで以下を確認。
  - jOOQ version: `3.19.32`
  - database URL: `jdbc:postgresql://localhost:5432/book_management`
  - target package: `com.example.bookmanagement.jooq`
  - excludes: `flyway_schema_history`
  - fetched tables: `4`
  - included tables: `3`
  - excluded tables: `1`
- 生成対象テーブル:
  - `authors`
  - `book_authors`
  - `books`
- `build` まで実行され、結果は `BUILD SUCCESSFUL`。
- Day4実装着手前のdevelopは正常にビルド可能な状態。

### Step 4 パッケージ作成

- 以下のパッケージディレクトリを作成。
  - `src/main/kotlin/com/example/bookmanagement/service`
  - `src/main/kotlin/com/example/bookmanagement/dto/author`
  - `src/main/kotlin/com/example/bookmanagement/exception`
- 既存ディレクトリ:
  - `src/main/kotlin/com/example/bookmanagement/controller`
  - `src/main/kotlin/com/example/bookmanagement/repository`
- Day4時点では `model` 配下に配置するファイルがないため、`model` パッケージは作成しない。
- `find src/main/kotlin/com/example/bookmanagement -type d | sort` でディレクトリ構成を確認。

### Step 5 共通エラーレスポンス作成

- `src/main/kotlin/com/example/bookmanagement/exception/ErrorResponse.kt` を新規作成。
- 実装内容:

```kotlin
package com.example.bookmanagement.exception

data class ErrorResponse(
    val status: Int,
    val message: String,
)
```

- エラー時はHTTPステータスコードとメッセージを共通形式で返す方針。
- 詳細なエラーコードやフィールド別エラー配列はDay4では実装しない。

### Step 6 業務例外作成

- `src/main/kotlin/com/example/bookmanagement/exception/BusinessRuleViolationException.kt` を新規作成。
- `src/main/kotlin/com/example/bookmanagement/exception/NotFoundException.kt` を新規作成。
- `BusinessRuleViolationException`:
  - 入力値不正・業務ルール違反を400として扱うための例外。
- `NotFoundException`:
  - 更新対象や取得対象が存在しない場合に404として扱うための例外。
- どちらも `RuntimeException` を継承し、messageを受け取るシンプルな実装。

### Step 7 共通例外ハンドラー作成

- `src/main/kotlin/com/example/bookmanagement/exception/ApiExceptionHandler.kt` を新規作成。
- `@RestControllerAdvice` によりAPI全体の例外レスポンスを統一する。
- ハンドリング対象:
  - `BusinessRuleViolationException`: 400
  - `NotFoundException`: 404
  - `HttpMessageNotReadableException`: 400
  - `MethodArgumentTypeMismatchException`: 400
  - `MethodArgumentNotValidException`: 400
  - `Exception`: 500
- `HttpMessageNotReadableException` では `Invalid request body` を返す。
- `MethodArgumentTypeMismatchException` では `Invalid path parameter` を返す。
- Bean Validationエラーでは先頭の `FieldError.defaultMessage` を返す。
- 想定外エラーでは内部例外メッセージを返さず、`Internal server error` を返す。
- `./gradlew compileKotlin` を実行。
- 結果: `BUILD SUCCESSFUL`

### Step 8 著者DTO作成

- 以下を新規作成。
  - `src/main/kotlin/com/example/bookmanagement/dto/author/CreateAuthorRequest.kt`
  - `src/main/kotlin/com/example/bookmanagement/dto/author/UpdateAuthorRequest.kt`
  - `src/main/kotlin/com/example/bookmanagement/dto/author/AuthorResponse.kt`
- `CreateAuthorRequest`:
  - `name: String`
  - `birthDate: LocalDate`
  - `name` に `@field:NotBlank(message = "Author name must not be blank")` を付与。
- `UpdateAuthorRequest`:
  - `name: String`
  - `birthDate: LocalDate`
  - `name` に `@field:NotBlank(message = "Author name must not be blank")` を付与。
- `AuthorResponse`:
  - `id: Long`
  - `name: String`
  - `birthDate: LocalDate`
- 手順書どおり、`name` / `birthDate` は非null型で定義。
- 未指定・null・日付形式不正は `HttpMessageNotReadableException` として400に変換する方針。
- `./gradlew compileKotlin` を実行。
- 結果: `BUILD SUCCESSFUL`

### Step 9 AuthorRepository拡張

- 既存の `src/main/kotlin/com/example/bookmanagement/repository/AuthorRepository.kt` を拡張。
- 追加したimport:
  - `com.example.bookmanagement.jooq.tables.records.AuthorsRecord`
  - `java.time.LocalDate`
  - `java.time.LocalDateTime`
- 追加・維持したメソッド:
  - `insert(name: String, birthDate: LocalDate): AuthorsRecord`
  - `update(id: Long, name: String, birthDate: LocalDate): AuthorsRecord?`
  - `findById(id: Long): AuthorsRecord?`
  - `existsById(id: Long): Boolean`
- `insert` では `created_at` / `updated_at` に同一の `LocalDateTime.now()` を設定。
- `update` では `updated_at` を更新し、対象がない場合は `null` を返す。
- `findById` は `AUTHORS.ID` で1件検索する。
- `./gradlew compileKotlin` を実行。
- 結果: `BUILD SUCCESSFUL`

### Step 10 AuthorService作成

- `src/main/kotlin/com/example/bookmanagement/service/AuthorService.kt` を新規作成。
- 実装したメソッド:
  - `create(request: CreateAuthorRequest): AuthorResponse`
  - `update(authorId: Long, request: UpdateAuthorRequest): AuthorResponse`
- `create`:
  - 生年月日の未来日チェックを実施。
  - `request.name.trim()` で前後空白を除去して登録。
  - Repositoryの `insert` 結果を `AuthorResponse` に変換。
- `update`:
  - 生年月日の未来日チェックを実施。
  - `existsById` で対象著者の存在確認を行い、存在しない場合は `NotFoundException("Author not found: id=$authorId")` をthrow。
  - Repositoryの `update` 結果が `null` の場合も `NotFoundException` に変換。
  - 更新結果を `AuthorResponse` に変換。
- `validateBirthDate`:
  - `birthDate.isAfter(LocalDate.now())` の場合、`BusinessRuleViolationException("Author birth date must be today or past date")` をthrow。
- `AuthorsRecord.toResponse()`:
  - DB RecordをAPIレスポンスDTOへ変換。
- `@Transactional` を `create` / `update` に付与。
- `./gradlew compileKotlin` を実行。
- 結果: `BUILD SUCCESSFUL`

### Step 11 AuthorController作成

- `src/main/kotlin/com/example/bookmanagement/controller/AuthorController.kt` を新規作成。
- `@RestController` / `@RequestMapping("/authors")` を付与。
- 実装したエンドポイント:
  - `POST /authors`
  - `PUT /authors/{authorId}`
- `POST /authors`:
  - `@Valid @RequestBody request: CreateAuthorRequest` を受け取る。
  - `authorService.create(request)` を呼び出す。
  - `201 Created` で `AuthorResponse` を返す。
- `PUT /authors/{authorId}`:
  - `@PathVariable authorId: Long` を受け取る。
  - `@Valid @RequestBody request: UpdateAuthorRequest` を受け取る。
  - `authorService.update(authorId, request)` を呼び出す。
  - `200 OK` で `AuthorResponse` を返す。
- Controllerでは業務ルール判定やDBアクセスを行わず、Serviceへ委譲する構成。
- `./gradlew compileKotlin` を実行。
- 結果: `BUILD SUCCESSFUL`

### Step 12 ビルド確認

- `docker compose ps` でPostgreSQLコンテナが起動中であることを確認。
  - container: `book-management-postgres`
  - status: `Up`
  - port: `0.0.0.0:5432->5432/tcp`
- `./gradlew clean generateJooq build` を実行。
- jOOQ codegenログで以下を確認。
  - target package: `com.example.bookmanagement.jooq`
  - excludes: `flyway_schema_history`
  - fetched tables: `4`
  - included tables: `3`
  - excluded tables: `1`
- Kotlinコンパイル、テスト、buildまで成功。
- 結果: `BUILD SUCCESSFUL`

### Step 13 アプリケーション起動確認

- `./gradlew bootRun` を実行。
- `generateJooq` が実行された後、Spring Bootアプリケーションが起動。
- 起動ログで以下を確認。
  - `Tomcat started on port 8080`
  - `Started BookManagementApiApplicationKt`
  - Flyway: `Current version of schema "public": 1`
  - Flyway: `Schema "public" is up to date. No migration necessary.`
  - `RequestMappingHandlerMapping`: `5 mappings`
  - `ExceptionHandlerExceptionResolver`: `ControllerAdvice beans: 1 @ExceptionHandler`
- Step 14以降のcurl確認で利用するため、アプリケーションは起動したままにしている。

### Step 14 著者登録API curl確認

- 起動中アプリケーションに対して `POST /authors` を実行。

```bash
curl -i -X POST http://localhost:8080/authors -H 'Content-Type: application/json' -d '{"name":"夏目漱石","birthDate":"1867-02-09"}'
```

- 確認結果:
  - HTTP status: `201`
  - response body: `{"id":1,"name":"夏目漱石","birthDate":"1867-02-09"}`
- 著者登録APIが正常に登録結果を返すことを確認。
- Step 15の著者更新API確認では、登録レスポンスの `id=1` を利用する。

### Step 15 著者更新API curl確認

- Step 14で登録された `id=1` を使い、起動中アプリケーションに対して `PUT /authors/1` を実行。

```bash
curl -i -X PUT http://localhost:8080/authors/1 -H 'Content-Type: application/json' -d '{"name":"夏目漱石 更新","birthDate":"1867-02-09"}'
```

- 確認結果:
  - HTTP status: `200`
  - response body: `{"id":1,"name":"夏目漱石 更新","birthDate":"1867-02-09"}`
- 著者更新APIが正常に更新結果を返すことを確認。

### Step 16 主要異常系 curl確認

- 起動中アプリケーションに対して主要異常系を確認。

#### 著者名が空

```bash
curl -i -X POST http://localhost:8080/authors -H 'Content-Type: application/json' -d '{"name":"","birthDate":"1867-02-09"}'
```

- 確認結果:
  - HTTP status: `400`
  - response body: `{"status":400,"message":"Author name must not be blank"}`

#### 生年月日が未来日

```bash
curl -i -X POST http://localhost:8080/authors -H 'Content-Type: application/json' -d '{"name":"未来太郎","birthDate":"2999-01-01"}'
```

- 確認結果:
  - HTTP status: `400`
  - response body: `{"status":400,"message":"Author birth date must be today or past date"}`

#### 日付形式不正

```bash
curl -i -X POST http://localhost:8080/authors -H 'Content-Type: application/json' -d '{"name":"夏目漱石","birthDate":"invalid-date"}'
```

- 確認結果:
  - HTTP status: `400`
  - response body: `{"status":400,"message":"Invalid request body"}`

#### 存在しない著者IDを更新

```bash
curl -i -X PUT http://localhost:8080/authors/999999 -H 'Content-Type: application/json' -d '{"name":"存在しない著者","birthDate":"1900-01-01"}'
```

- 確認結果:
  - HTTP status: `404`
  - response body: `{"status":404,"message":"Author not found: id=999999"}`

#### パスID型不一致

```bash
curl -i -X PUT http://localhost:8080/authors/abc -H 'Content-Type: application/json' -d '{"name":"夏目漱石","birthDate":"1867-02-09"}'
```

- 確認結果:
  - HTTP status: `400`
  - response body: `{"status":400,"message":"Invalid path parameter"}`

- 主要異常系がすべて期待どおりのHTTPステータスと共通エラーレスポンスを返すことを確認。
- curl確認後、`Ctrl + C` で `bootRun` を停止。
- 停止時の終了コードは `130` だが、これは確認後に手動停止したためであり、起動失敗ではない。

### Step 17 簡易テスト追加

- 手順書では余力対応の位置づけ。
- ユーザー確認のうえ、著者APIのController統合テスト追加は後工程でまとめて対応する方針とした。
- このStepでは新規テストファイルは追加しない。

### Step 18 テスト実行

- `docker compose ps` でPostgreSQLコンテナが起動中であることを確認。
  - container: `book-management-postgres`
  - status: `Up`
  - port: `0.0.0.0:5432->5432/tcp`
- `./gradlew test` を実行。
- `compileKotlin` が `generateJooq` に依存しているため、テスト実行時にも `generateJooq` が実行された。
- jOOQ codegenログで以下を確認。
  - target package: `com.example.bookmanagement.jooq`
  - excludes: `flyway_schema_history`
  - fetched tables: `4`
  - included tables: `3`
  - excluded tables: `1`
- `test` タスクまで実行され、結果は `BUILD SUCCESSFUL`。

### Step 19 Git差分確認

- `git status --short --untracked-files=all` を実行。
- 現在の差分:
  - `M document/business_rules_design.md`
  - `M src/main/kotlin/com/example/bookmanagement/repository/AuthorRepository.kt`
  - `?? document/develop/day4_common_foundation_author_api_log.md`
  - `?? document/develop/作業手順/coding_test_day_2_project_setup_steps.md`
  - `?? document/develop/作業手順/coding_test_day_3_db_flyway_jooq_steps.md`
  - `?? document/develop/作業手順/coding_test_day_4_common_foundation_author_api_steps.md`
  - `?? src/main/kotlin/com/example/bookmanagement/controller/AuthorController.kt`
  - `?? src/main/kotlin/com/example/bookmanagement/dto/author/AuthorResponse.kt`
  - `?? src/main/kotlin/com/example/bookmanagement/dto/author/CreateAuthorRequest.kt`
  - `?? src/main/kotlin/com/example/bookmanagement/dto/author/UpdateAuthorRequest.kt`
  - `?? src/main/kotlin/com/example/bookmanagement/exception/ApiExceptionHandler.kt`
  - `?? src/main/kotlin/com/example/bookmanagement/exception/BusinessRuleViolationException.kt`
  - `?? src/main/kotlin/com/example/bookmanagement/exception/ErrorResponse.kt`
  - `?? src/main/kotlin/com/example/bookmanagement/exception/NotFoundException.kt`
  - `?? src/main/kotlin/com/example/bookmanagement/service/AuthorService.kt`
- `document/business_rules_design.md`:
  - 生年月日の未来日チェックの実装場所を `Service` に統一。
  - `CHECK (birth_date <= CURRENT_DATE)` は設定しない方針に修正。
- `AuthorRepository.kt`:
  - `insert`
  - `update`
  - `findById`
  - `existsById`
- 新規実装ファイル:
  - `AuthorController.kt`
  - 著者DTO 3件
  - 共通例外/例外ハンドラー 4件
  - `AuthorService.kt`
- `git check-ignore -v build/generated-src/jooq/main/com/example/bookmanagement/jooq/tables/Authors.kt` を実行。
- 生成コードは `.gitignore:3:build/` によりGit管理対象外であることを確認。
- `build/generated-src/jooq` / `.gradle` / `build` / `.idea` はGit差分に出ていない。
- 手順書3件はユーザー提供の作業手順書として扱い、今回のcommit対象には含めない方針。

### Step 20 commit

- commit対象はDay4実装差分、設計書修正、作業メモに限定する。
- ユーザー提供の手順書3件は未追跡のままとし、このcommitには含めない。
- commit対象:
  - `document/business_rules_design.md`
  - `document/develop/day4_common_foundation_author_api_log.md`
  - `src/main/kotlin/com/example/bookmanagement/exception/`
  - `src/main/kotlin/com/example/bookmanagement/dto/author/`
  - `src/main/kotlin/com/example/bookmanagement/repository/AuthorRepository.kt`
  - `src/main/kotlin/com/example/bookmanagement/service/AuthorService.kt`
  - `src/main/kotlin/com/example/bookmanagement/controller/AuthorController.kt`
