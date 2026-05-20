# 6日目 書籍更新API実装 作業メモ

## 2026-05-20

### Step 1 developブランチ最新化

- `git checkout develop` を実行。
  - 結果: `Already on 'develop'`
  - `develop` は `origin/develop` と同期済み。
- `git pull origin develop` を実行。
  - 結果: `Already up to date.`
- 最新commit:
  - `fc4d0f2 feat: implement book creation API`
- 作業開始時点の差分:
  - Day2〜Day6の作業手順書が未追跡ファイルとして存在。
  - これまでの運用どおり、実装commitには含めない想定で進める。

### Step 2 作業メモ作成

- `document/develop/day6_book_api_update_log.md` を作成。
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

### Step 5 UpdateBookRequest作成

- `src/main/kotlin/com/example/bookmanagement/dto/book/UpdateBookRequest.kt` を作成。
  - `CreateBookRequest` と同じ検証ルールで実装。
  - `title`: `@NotBlank`
  - `price`: `@Min(0)`
  - `publicationStatus`: `PublicationStatus`
  - `authorIds`: `@NotEmpty`
- Request DTOは非null型で定義し、未指定・null・型不一致・enum不正はJackson / Kotlinの変換エラーとして400にする方針。

### Step 6 BookRepository.findById実装

- `src/main/kotlin/com/example/bookmanagement/repository/BookRepository.kt` を更新。
- `findById(id: Long): BooksRecord?` を追加。
  - `BOOKS.ID.eq(id)` で `books` を1件取得する。
  - 見つからない場合は `null` を返し、Service層で404へ変換する方針。

### Step 7 BookRepository.update実装

- `src/main/kotlin/com/example/bookmanagement/repository/BookRepository.kt` を更新。
- `update(id, title, price, publicationStatus): BooksRecord?` を追加。
  - `title` / `price` / `publication_status` / `updated_at` を更新する。
  - `publication_status` はDB上 `VARCHAR(20)` のため、`PublicationStatus.name` を保存する。
  - `returning().fetchOne()` で更新後レコードを返す。
  - 更新対象が存在しない場合は `null` を返し、Service層で404へ変換する方針。

### Step 8 BookRepository.deleteBookAuthors実装

- `src/main/kotlin/com/example/bookmanagement/repository/BookRepository.kt` を更新。
- `deleteBookAuthors(bookId: Long)` を追加。
  - `BOOK_AUTHORS.BOOK_ID.eq(bookId)` を条件に、対象書籍の著者関連を削除する。
  - 書籍更新時は関連を削除してからリクエストの `authorIds` で再登録する方針。

### Step 9 BookService.update実装

- `src/main/kotlin/com/example/bookmanagement/service/BookService.kt` を更新。
- `update(bookId: Long, request: UpdateBookRequest): BookResponse` を追加。
  - `@Transactional` を付与。
  - 更新対象書籍が存在しない場合は `NotFoundException("Book not found: id=$bookId")` を投げる。
  - `authorIds` の空チェック、重複チェックは登録時と同じ `validateAuthorIds` を再利用。
  - 著者存在チェックは登録時と同じ `validateAllAuthorsExist` を再利用。
  - `BookRepository.update` で `books` を更新。
  - `BookRepository.deleteBookAuthors` で既存の著者関連を削除。
  - `BookRepository.insertBookAuthors` でリクエストの著者関連を再登録。
  - レスポンスの著者順はリクエストの `authorIds` 順に並べ替える。
- 出版状態遷移チェックは手順書のStep 10で追加する。

### Step 10 出版状態遷移ルール実装

- `src/main/kotlin/com/example/bookmanagement/service/BookService.kt` を更新。
- `validatePublicationStatusTransition(currentStatus, requestedStatus)` を追加。
  - `PUBLISHED` から `UNPUBLISHED` への変更のみ拒否する。
  - 業務ルール違反として `BusinessRuleViolationException("Published book cannot be changed to unpublished")` を投げる。
- `BookService.update` で `BookRepository.update` を呼ぶ前に遷移チェックを実行するよう修正。
  - 禁止ケースでは `books` / `book_authors` が更新されないようにする。

### Step 11 BookControllerにPUT追加

- `src/main/kotlin/com/example/bookmanagement/controller/BookController.kt` を更新。
- `PUT /books/{bookId}` を追加。
  - `@PathVariable bookId: Long` で書籍IDを受け取る。
  - `@Valid @RequestBody` で `UpdateBookRequest` を受け取る。
  - `BookService.update` に処理を委譲。
  - 正常時は `200 OK` で `BookResponse` を返す。
- `bookId` の型不一致は既存の `MethodArgumentTypeMismatchException` ハンドリングで400になる方針。

### Step 12 ビルド確認

- `docker compose ps` を実行。
  - 初回はsandbox制限によりDockerデーモンへ接続できなかったため、権限昇格で再実行。
  - 結果: `book-management-postgres` が `Up`。
- `./gradlew clean generateJooq build` を実行。
  - 初回はsandbox制限により `~/.gradle` 配下のロックファイルへアクセスできなかったため、権限昇格で再実行。
  - 結果: `BUILD SUCCESSFUL`
- Day6実装後のjOOQコード生成、Kotlinコンパイル、既存テスト、buildが成功していることを確認。

### Step 13 アプリケーション起動確認

- `./gradlew bootRun` を実行。
  - 結果: `Started BookManagementApiApplicationKt`
  - Tomcatが `8080` ポートで起動。
  - Flywayは `Schema "public" is up to date. No migration necessary.` を確認。
- 以降のcurl確認のため、bootRunは起動したまま維持。

### Step 14 事前データ作成

- `POST /authors` で夏目漱石を登録。
  - 結果: `HTTP/1.1 201`
  - レスポンス: `{"id":4,"name":"夏目漱石","birthDate":"1867-02-09"}`
  - `AUTHOR_ID_1=4`
- `POST /authors` で森鴎外を登録。
  - 結果: `HTTP/1.1 201`
  - レスポンス: `{"id":5,"name":"森鴎外","birthDate":"1862-02-17"}`
  - `AUTHOR_ID_2=5`
- `POST /books` で更新対象の未出版書籍を登録。
  - `title`: `吾輩は猫である`
  - `price`: `1200`
  - `publicationStatus`: `UNPUBLISHED`
  - `authorIds`: `[4]`
  - 結果: `HTTP/1.1 201`
  - レスポンス: `{"id":3,"title":"吾輩は猫である","price":1200,"publicationStatus":"UNPUBLISHED","authors":[{"id":4,"name":"夏目漱石","birthDate":"1867-02-09"}]}`
  - `BOOK_ID=3`

### Step 15 書籍更新API 正常系確認

- `PUT /books/3` を実行。
  - `title`: `吾輩は猫である 改訂版`
  - `price`: `1500`
  - `publicationStatus`: `UNPUBLISHED`
  - `authorIds`: `[4]`
- 結果: `HTTP/1.1 200`
- レスポンス:
  - `{"id":3,"title":"吾輩は猫である 改訂版","price":1500,"publicationStatus":"UNPUBLISHED","authors":[{"id":4,"name":"夏目漱石","birthDate":"1867-02-09"}]}`
- `title` / `price` が更新され、著者情報が返ることを確認。

### Step 16 著者関連の更新確認

- `PUT /books/3` を実行。
  - `title`: `吾輩は猫である 共著版`
  - `price`: `1600`
  - `publicationStatus`: `UNPUBLISHED`
  - `authorIds`: `[4,5]`
- 結果: `HTTP/1.1 200`
- レスポンス:
  - `{"id":3,"title":"吾輩は猫である 共著版","price":1600,"publicationStatus":"UNPUBLISHED","authors":[{"id":4,"name":"夏目漱石","birthDate":"1867-02-09"},{"id":5,"name":"森鴎外","birthDate":"1862-02-17"}]}`
- 著者が2件返ること、著者順がリクエスト順であることを確認。

### Step 17 出版状態遷移確認

- `UNPUBLISHED` から `PUBLISHED` への変更を確認。
  - `PUT /books/3`
  - 結果: `HTTP/1.1 200`
  - レスポンス: `{"id":3,"title":"吾輩は猫である 出版版","price":1700,"publicationStatus":"PUBLISHED","authors":[{"id":4,"name":"夏目漱石","birthDate":"1867-02-09"}]}`
- `PUBLISHED` のままの更新を確認。
  - `PUT /books/3`
  - 結果: `HTTP/1.1 200`
  - レスポンス: `{"id":3,"title":"吾輩は猫である 出版後改訂版","price":1800,"publicationStatus":"PUBLISHED","authors":[{"id":4,"name":"夏目漱石","birthDate":"1867-02-09"},{"id":5,"name":"森鴎外","birthDate":"1862-02-17"}]}`
- `PUBLISHED` から `UNPUBLISHED` への変更を確認。
  - `PUT /books/3`
  - 結果: `HTTP/1.1 400`
  - レスポンス: `{"status":400,"message":"Published book cannot be changed to unpublished"}`
- 許可される遷移は成功し、禁止される遷移のみ400になることを確認。

### Step 18 主要異常系確認

- 存在しない `bookId` のケースを確認。
  - 結果: `HTTP/1.1 404`
  - レスポンス: `{"status":404,"message":"Book not found: id=999999"}`
- `bookId` が数値でないケースを確認。
  - 結果: `HTTP/1.1 400`
  - レスポンス: `{"status":400,"message":"Invalid path parameter"}`
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

### Step 19 DB状態確認

- `books` テーブルの対象レコードを確認。
  - 初回はsandbox制限により `localhost:5432` に接続できなかったため、権限昇格で再実行。
  - 結果:

```text
 id |            title            | price | publication_status
----+-----------------------------+-------+--------------------
  3 | 吾輩は猫である 出版後改訂版 |  1800 | PUBLISHED
```

- `book_authors` テーブルの対象レコードを確認。
  - 結果:

```text
 book_id | author_id
---------+-----------
       3 |         4
       3 |         5
```

- `PUBLISHED` から `UNPUBLISHED` への更新失敗後も、DB状態が直前の成功状態のままであることを確認。
- curl確認とDB確認完了後、bootRunを `Ctrl+C` で停止。
  - 停止後、`8080` ポートでLISTENしているプロセスがないことを確認。

### Step 20 簡易テスト追加

- 手順書上は任意項目のため、今回はスキップ。
- 書籍更新APIの主要ケースはcurlで確認済み。
- Controller統合テストなどのテスト強化は後工程でまとめて対応する方針。

### Step 21 テスト実行

- `docker compose ps` を実行。
  - 初回はsandbox制限によりDockerデーモンへ接続できなかったため、権限昇格で再実行。
  - 結果: `book-management-postgres` が `Up`。
- `./gradlew test` を実行。
  - 結果: `BUILD SUCCESSFUL`
  - jOOQコード生成後、既存テストが成功していることを確認。

### Step 22 作業メモ更新

#### 実装したファイル

- `src/main/kotlin/com/example/bookmanagement/dto/book/UpdateBookRequest.kt`
- `src/main/kotlin/com/example/bookmanagement/repository/BookRepository.kt`
- `src/main/kotlin/com/example/bookmanagement/service/BookService.kt`
- `src/main/kotlin/com/example/bookmanagement/controller/BookController.kt`

#### 実装内容

- `UpdateBookRequest` を作成。
- `BookRepository.findById` を追加。
- `BookRepository.update` を追加。
- `BookRepository.deleteBookAuthors` を追加。
- `BookService.update` を追加。
  - 更新対象書籍の存在確認。
  - `authorIds` の空・重複チェック。
  - 著者存在チェック。
  - 出版状態遷移チェック。
  - `books` 更新と `book_authors` 削除・再登録。
- `BookController` に `PUT /books/{bookId}` を追加。

#### 検証結果

- `./gradlew clean generateJooq build`
  - `BUILD SUCCESSFUL`
- `PUT /books/{bookId}` 正常系
  - 書籍更新: `HTTP/1.1 200`
  - 著者関連更新: `HTTP/1.1 200`
- 出版状態遷移
  - `UNPUBLISHED` から `PUBLISHED`: `HTTP/1.1 200`
  - `PUBLISHED` から `PUBLISHED`: `HTTP/1.1 200`
  - `PUBLISHED` から `UNPUBLISHED`: `HTTP/1.1 400`
- 主要異常系
  - 存在しないbookId: `404`
  - bookId型不一致: `400`
  - title空: `400`
  - priceマイナス: `400`
  - authorIds空: `400`
  - authorIds重複: `400`
  - 存在しない著者ID: `400`
  - publicationStatus不正: `400`
  - price型不一致: `400`
- DB状態確認
  - `books.id=3` が `PUBLISHED` のままであることを確認。
  - `book_authors` は `book_id=3` に対して `author_id=4,5` が紐づくことを確認。
  - `PUBLISHED` から `UNPUBLISHED` への更新失敗後も、DB状態が直前の成功状態で保たれることを確認。
- `./gradlew test`
  - `BUILD SUCCESSFUL`

#### 判断メモ

- 書籍更新時の著者関連は、既存関連を削除してからリクエストの `authorIds` で再登録する方針。
- 出版状態遷移チェックは `books` 更新前に実行し、禁止ケースではDB更新しない。
- `UpdateBookRequest` は非null型で定義し、未指定・null・型不一致・enum不正は `HttpMessageNotReadableException` として400にする方針。
- 簡易テスト追加は手順書上任意のためスキップし、後工程のテスト強化でまとめて対応する。
- Day2〜Day6の作業手順書は未追跡ファイルとして存在するが、これまでの運用どおり今回の実装commitには含めない想定。

#### commit対象想定

- Day6実装ファイル一式。
- `document/develop/day6_book_api_update_log.md`

### Step 23 Git差分確認

- `git status --short --untracked-files=all` を実行。
- Day6実装で想定される差分:
  - `A src/main/kotlin/com/example/bookmanagement/dto/book/UpdateBookRequest.kt`
  - `M src/main/kotlin/com/example/bookmanagement/repository/BookRepository.kt`
  - `M src/main/kotlin/com/example/bookmanagement/service/BookService.kt`
  - `M src/main/kotlin/com/example/bookmanagement/controller/BookController.kt`
  - `A document/develop/day6_book_api_update_log.md`
- Day2〜Day6の作業手順書は未追跡ファイルとして存在するが、今回の実装commitには含めない。
- `git diff --check` を実行。
  - 結果: 指摘なし。
- `build/generated-src/jooq`、`.gradle`、`build`、`.idea` はGit差分に出ていないことを確認。

### Step 24 commit

- Day6実装ファイル一式と作業メモをcommit対象にする。
- Day2〜Day6の作業手順書は今回のcommit対象外にする。
- commit message:
  - `feat: implement book update API`
