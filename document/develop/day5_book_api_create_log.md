# 5日目 書籍API実装 前半 作業メモ

## 2026-05-20

### Step 1 developブランチ最新化

- `git checkout develop` を実行。
  - 結果: `Already on 'develop'`
  - `develop` は `origin/develop` と同期済み。
- `git pull origin develop` を実行。
  - 結果: `Already up to date.`
- 最新commit:
  - `6de0cf0 feat: implement author APIs and common error handling`
- 作業開始時点の差分:
  - Day2〜Day5の作業手順書が未追跡ファイルとして存在。
  - これまでの運用どおり、実装commitには含めない想定で進める。

### Step 2 作業メモ作成

- `document/develop/day5_book_api_create_log.md` を作成。
- 以降、各Stepの実行結果を本ファイルに追記する。

### Step 3 PostgreSQLコンテナ起動

- `docker compose up -d` を実行。
  - 初回はsandbox制限によりDockerデーモンへ接続できなかったため、権限昇格で再実行。
  - 結果: `book-management-postgres` は起動済み。
- `docker compose ps` を実行。
  - 結果: `book-management-postgres` が `Up`。
  - `0.0.0.0:5432->5432/tcp` で公開されていることを確認。

### Step 4 実装前のビルド確認

- `./gradlew clean generateJooq build` を実行。
  - 初回はsandbox制限により `~/.gradle` 配下のロックファイルへアクセスできなかったため、権限昇格で再実行。
  - 結果: `BUILD SUCCESSFUL`
- jOOQコード生成、Kotlinコンパイル、テスト、buildが成功していることを確認。
- 実装前の既存状態に問題なし。

### Step 5 PublicationStatus enum作成

- `src/main/kotlin/com/example/bookmanagement/model/PublicationStatus.kt` を作成。
- 定義値:
  - `UNPUBLISHED`
  - `PUBLISHED`
- DBの `publication_status` CHECK制約と一致する値として実装。

### Step 6 書籍DTO作成

- `src/main/kotlin/com/example/bookmanagement/dto/book/CreateBookRequest.kt` を作成。
  - `title`: `@NotBlank`
  - `price`: `@Min(0)`
  - `publicationStatus`: `PublicationStatus`
  - `authorIds`: `@NotEmpty`
- `src/main/kotlin/com/example/bookmanagement/dto/book/BookResponse.kt` を作成。
  - 書籍情報と著者レスポンス一覧を返すDTOとして実装。
- Request DTOは非null型で定義し、未指定・null・型不一致・enum不正はJackson / Kotlinの変換エラーとして400にする方針。

### Step 7 AuthorRepositoryに著者一覧取得メソッドを追加

- `src/main/kotlin/com/example/bookmanagement/repository/AuthorRepository.kt` を更新。
- `findByIds(ids: List<Long>): List<AuthorsRecord>` を追加。
  - 空リストの場合はDBアクセスせず `emptyList()` を返す。
  - jOOQの `in` はKotlin予約語のため、`AUTHORS.ID.\`in\`(ids)` の形で実装。
- 取得順はDB側で保証しないため、必要な並び替えはService層で行う方針。

### Step 8 BookRepository作成

- `src/main/kotlin/com/example/bookmanagement/repository/BookRepository.kt` を作成。
- `insert(title, price, publicationStatus)` を実装。
  - `books` に登録し、`returning().fetchOne()!!` で登録レコードを返す。
  - `publication_status` はDB上 `VARCHAR(20)` のため、`PublicationStatus.name` を保存する。
  - `created_at` / `updated_at` は同じ `LocalDateTime.now()` を設定する。
- `insertBookAuthors(bookId, authorIds)` を実装。
  - 空リストの場合は何もしない。
  - jOOQ生成Recordのプロパティ名差分を避けるため、手順書の代替案どおりループinsertを採用。

### Step 9 BookService作成

- `src/main/kotlin/com/example/bookmanagement/service/BookService.kt` を作成。
- `create(request: CreateBookRequest): BookResponse` を実装。
  - `@Transactional` を付与。
  - `authorIds` の空チェック、重複チェックをService層でも実施。
  - `AuthorRepository.findByIds` で著者を取得し、全IDが存在することを検証。
  - `BookRepository.insert` で `books` に登録。
  - `BookRepository.insertBookAuthors` で `book_authors` に登録。
  - レスポンスの著者順はリクエストの `authorIds` 順に並べ替える。
- 存在しない著者IDは `BusinessRuleViolationException("Specified author does not exist")` として400扱いにする方針。

### Step 10 BookController作成

- `src/main/kotlin/com/example/bookmanagement/controller/BookController.kt` を作成。
- `POST /books` を実装。
  - `@Valid @RequestBody` で `CreateBookRequest` を受け取る。
  - `BookService.create` に処理を委譲。
  - 正常時は `201 Created` で `BookResponse` を返す。
- Controllerでは業務ルール判定やDBアクセスを行わない方針。

### Step 11 ビルド確認

- `docker compose ps` を実行。
  - sandbox制限によりDockerデーモンへ接続できなかったため、権限昇格で再実行。
  - 結果: `book-management-postgres` が `Up`。
- `./gradlew clean generateJooq build` を実行。
  - 初回はsandbox制限により `~/.gradle` 配下のロックファイルへアクセスできなかったため、権限昇格で再実行。
  - 結果: `BUILD SUCCESSFUL`
- Day5実装後のjOOQコード生成、Kotlinコンパイル、既存テスト、buildが成功していることを確認。

### Step 12 アプリケーション起動確認

- `./gradlew bootRun` を実行。
  - 結果: `Started BookManagementApiApplicationKt`
  - Tomcatが `8080` ポートで起動。
  - Flywayは `Schema "public" is up to date. No migration necessary.` を確認。
- 以降のcurl確認のため、bootRunは起動したまま維持。

### Step 13 事前に著者を登録

- `POST /authors` で夏目漱石を登録。
  - 初回はsandbox内から `localhost:8080` に接続できなかったため、権限昇格でcurlを再実行。
  - 結果: `HTTP/1.1 201`
- レスポンス:
  - `{"id":2,"name":"夏目漱石","birthDate":"1867-02-09"}`
- 以降の `AUTHOR_ID_1` は `2` として扱う。

### Step 14 書籍登録API 正常系確認

- `POST /books` を実行。
  - `title`: `吾輩は猫である`
  - `price`: `1200`
  - `publicationStatus`: `PUBLISHED`
  - `authorIds`: `[2]`
- 結果: `HTTP/1.1 201`
- レスポンス:
  - `{"id":1,"title":"吾輩は猫である","price":1200,"publicationStatus":"PUBLISHED","authors":[{"id":2,"name":"夏目漱石","birthDate":"1867-02-09"}]}`
- 書籍情報と著者情報がレスポンスに含まれることを確認。

### Step 15 複数著者の書籍登録確認

- `POST /authors` で森鴎外を登録。
  - 結果: `HTTP/1.1 201`
  - レスポンス: `{"id":3,"name":"森鴎外","birthDate":"1862-02-17"}`
  - 以降の `AUTHOR_ID_2` は `3` として扱う。
- `POST /books` を実行。
  - `title`: `共著サンプル`
  - `price`: `1500`
  - `publicationStatus`: `UNPUBLISHED`
  - `authorIds`: `[2,3]`
- 結果: `HTTP/1.1 201`
- レスポンス:
  - `{"id":2,"title":"共著サンプル","price":1500,"publicationStatus":"UNPUBLISHED","authors":[{"id":2,"name":"夏目漱石","birthDate":"1867-02-09"},{"id":3,"name":"森鴎外","birthDate":"1862-02-17"}]}`
- 著者が2件返ること、`publicationStatus` が `UNPUBLISHED` で返ること、著者順がリクエスト順であることを確認。

### Step 16 主要異常系確認

- `title` が空のケースを確認。
  - 結果: `HTTP/1.1 400`
  - レスポンス: `{"status":400,"message":"Book title must not be blank"}`
- `price` がマイナスのケースを確認。
  - 結果: `HTTP/1.1 400`
  - レスポンス: `{"status":400,"message":"Book price must be greater than or equal to 0"}`
- `authorIds` が空のケースを確認。
  - 結果: `HTTP/1.1 400`
  - レスポンス: `{"status":400,"message":"Book must have at least one author"}`
- `authorIds` に重複があるケースを確認。
  - 結果: `HTTP/1.1 400`
  - レスポンス: `{"status":400,"message":"Author ids must not contain duplicates"}`
- 存在しない著者IDを指定したケースを確認。
  - 結果: `HTTP/1.1 400`
  - レスポンス: `{"status":400,"message":"Specified author does not exist"}`
- `publicationStatus` が不正なケースを確認。
  - 結果: `HTTP/1.1 400`
  - レスポンス: `{"status":400,"message":"Invalid request body"}`
- `price` が文字列のケースを確認。
  - 結果: `HTTP/1.1 400`
  - レスポンス: `{"status":400,"message":"Invalid request body"}`
- curl確認完了後、bootRunを停止。
  - `Ctrl+C` 相当の入力を送れなかったため、起動ログ上のPID `52712` を指定して停止。
  - 停止後、`8080` ポートでLISTENしているプロセスがないことを確認。
  - Gradle上は手動停止により `bootRun` が exit value `143` で終了したが、アプリ起動・API確認自体は成功。

### Step 17 DB状態確認

- `books` テーブルを確認。
  - 初回はsandbox制限により `localhost:5432` に接続できなかったため、権限昇格で再実行。
  - 結果:

```text
 id |     title      | price | publication_status
----+----------------+-------+--------------------
  1 | 吾輩は猫である |  1200 | PUBLISHED
  2 | 共著サンプル   |  1500 | UNPUBLISHED
```

- `book_authors` テーブルを確認。
  - 結果:

```text
 book_id | author_id
---------+-----------
       1 |         2
       2 |         2
       2 |         3
```

- 単著書籍と複数著者書籍の関連が期待どおり登録されていることを確認。

### Step 18 簡易テスト追加

- 手順書上は任意項目のため、今回はスキップ。
- 書籍登録APIの主要ケースはcurlで確認済み。
- Controller統合テストなどのテスト強化は後工程でまとめて対応する方針。

### Step 19 テスト実行

- `docker compose ps` を実行。
  - 初回はsandbox制限によりDockerデーモンへ接続できなかったため、権限昇格で再実行。
  - 結果: `book-management-postgres` が `Up`。
- `./gradlew test` を実行。
  - 結果: `BUILD SUCCESSFUL`
  - jOOQコード生成後、既存テストが成功していることを確認。

### Step 20 作業メモ更新

#### 実装したファイル

- `src/main/kotlin/com/example/bookmanagement/model/PublicationStatus.kt`
- `src/main/kotlin/com/example/bookmanagement/dto/book/CreateBookRequest.kt`
- `src/main/kotlin/com/example/bookmanagement/dto/book/BookResponse.kt`
- `src/main/kotlin/com/example/bookmanagement/repository/AuthorRepository.kt`
- `src/main/kotlin/com/example/bookmanagement/repository/BookRepository.kt`
- `src/main/kotlin/com/example/bookmanagement/service/BookService.kt`
- `src/main/kotlin/com/example/bookmanagement/controller/BookController.kt`

#### 実装内容

- `PublicationStatus` enumを作成。
- 書籍登録リクエスト / レスポンスDTOを作成。
- `AuthorRepository.findByIds` を追加。
- `BookRepository` を作成し、`books` / `book_authors` への登録処理を実装。
- `BookService.create` を作成し、以下の業務ルールを実装。
  - `authorIds` 空チェック
  - `authorIds` 重複チェック
  - 著者存在チェック
  - レスポンス著者順のリクエスト順への並べ替え
- `BookController` を作成し、`POST /books` を `201 Created` で返すよう実装。

#### 検証結果

- `./gradlew clean generateJooq build`
  - `BUILD SUCCESSFUL`
- `POST /books` 正常系
  - 単著: `HTTP/1.1 201`
  - 複数著者: `HTTP/1.1 201`
- 主要異常系
  - title空: `400`
  - priceマイナス: `400`
  - authorIds空: `400`
  - authorIds重複: `400`
  - 存在しない著者ID: `400`
  - publicationStatus不正: `400`
  - price型不一致: `400`
- DB状態確認
  - `books` に2件登録されていることを確認。
  - `book_authors` に単著1件、複数著者2件の関連が登録されていることを確認。
- `./gradlew test`
  - `BUILD SUCCESSFUL`

#### 判断メモ

- `book_authors` 登録は、jOOQ生成Recordのプロパティ名差分を避けるためループinsertを採用。
- `CreateBookRequest` は非null型で定義し、未指定・null・型不一致・enum不正は `HttpMessageNotReadableException` として400にする方針。
- 簡易テスト追加は手順書上任意のためスキップし、後工程のテスト強化でまとめて対応する。
- Day2〜Day5の作業手順書は未追跡ファイルとして存在するが、これまでの運用どおり今回の実装commitには含めない想定。

#### commit対象想定

- Day5実装ファイル一式。
- `document/develop/day5_book_api_create_log.md`

### Step 21 Git差分確認

- `git status --short --untracked-files=all` を実行。
- Day5実装で想定される差分:
  - `M src/main/kotlin/com/example/bookmanagement/repository/AuthorRepository.kt`
  - `A src/main/kotlin/com/example/bookmanagement/model/PublicationStatus.kt`
  - `A src/main/kotlin/com/example/bookmanagement/dto/book/CreateBookRequest.kt`
  - `A src/main/kotlin/com/example/bookmanagement/dto/book/BookResponse.kt`
  - `A src/main/kotlin/com/example/bookmanagement/repository/BookRepository.kt`
  - `A src/main/kotlin/com/example/bookmanagement/service/BookService.kt`
  - `A src/main/kotlin/com/example/bookmanagement/controller/BookController.kt`
  - `A document/develop/day5_book_api_create_log.md`
- Day2〜Day5の作業手順書は未追跡ファイルとして存在するが、今回の実装commitには含めない。
- `git diff --check` を実行。
  - 結果: 指摘なし。
- `build/generated-src/jooq`、`.gradle`、`build`、`.idea` はGit差分に出ていないことを確認。

### Step 22 commit

- Day5実装ファイル一式と作業メモをcommit対象にする。
- Day2〜Day5の作業手順書は今回のcommit対象外にする。
- commit message:
  - `feat: implement book creation API`
