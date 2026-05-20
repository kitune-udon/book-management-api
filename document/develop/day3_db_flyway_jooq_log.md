# 3日目 DB / Flyway / jOOQ構築 作業メモ

## 2026-05-20

### Step 1 developブランチ最新化

- 作業開始時のブランチは `develop`。
- 作業開始時点で以下の未追跡ファイルが存在。
  - `document/develop/coding_test_day_2_project_setup_steps.md`
  - `document/develop/coding_test_day_3_db_flyway_jooq_steps.md`
- 上記はユーザー提供の作業手順書として扱い、このStepでは変更・ステージングしない。
- `git pull origin develop` を実行。
- 結果: `Already up to date.`
- 最新化後の状態:
  - `develop` は `origin/develop` と同期済み。
  - Day 3作業実装に関する変更はまだなし。
  - 上記手順書2件のみ未追跡ファイルとして存在。

### Step 2 PostgreSQLコンテナ起動

- `docker compose up -d` を実行。
- `book-management-postgres` は既に起動中だったため、状態は `Running`。
- `docker compose ps` で以下を確認。
  - container: `book-management-postgres`
  - image: `postgres:16`
  - status: `Up`
  - port: `0.0.0.0:5432->5432/tcp`
- `docker compose logs --tail=40 postgres` で以下を確認。
  - PostgreSQL: `16.14`
  - `database system is ready to accept connections`

### Step 3 Flywayマイグレーションディレクトリ作成

- `src/main/resources/db/migration` の存在を確認。
- Spring Initializr生成時点でディレクトリは既に存在していた。
- ディレクトリ内にマイグレーションファイルはまだ存在しない。
- このStepでは追加作成なし。

### Step 4 V1__create_tables.sql作成

- `src/main/resources/db/migration/V1__create_tables.sql` を新規作成。
- Flyway命名規則どおり、`V1` の後ろはアンダースコア2つ `__` とした。
- DDL内容:
  - `authors` テーブル作成
  - `books` テーブル作成
  - `book_authors` テーブル作成
  - `idx_book_authors_author_id` インデックス作成
- 確認事項:
  - `authors.birth_date` は `NOT NULL`。
  - `authors.birth_date <= CURRENT_DATE` のCHECK制約は入れていない。
  - `books.price >= 0` のCHECK制約を設定。
  - `books.publication_status` は `UNPUBLISHED` / `PUBLISHED` のみ許可。
  - `book_authors` は `(book_id, author_id)` の複合主キー。
  - `book_authors.book_id` は `books.id` を参照し、`ON DELETE CASCADE`。
  - `book_authors.author_id` は `authors.id` を参照し、`ON DELETE RESTRICT`。

### Step 6 DDL内容確認

- `V1__create_tables.sql` の内容を確認。
- `authors`:
  - `id BIGSERIAL PRIMARY KEY`
  - `name VARCHAR(255) NOT NULL`
  - `birth_date DATE NOT NULL`
  - `created_at` / `updated_at` は `CURRENT_TIMESTAMP`
  - `CURRENT_DATE` を使ったCHECK制約なし
- `books`:
  - `id BIGSERIAL PRIMARY KEY`
  - `title VARCHAR(255) NOT NULL`
  - `price INTEGER NOT NULL`
  - `chk_books_price CHECK (price >= 0)`
  - `chk_books_publication_status CHECK (publication_status IN ('UNPUBLISHED', 'PUBLISHED'))`
- `book_authors`:
  - `(book_id, author_id)` の複合主キー
  - `fk_book_authors_book_id` は `books(id)` 参照、`ON DELETE CASCADE`
  - `fk_book_authors_author_id` は `authors(id)` 参照、`ON DELETE RESTRICT`
  - `idx_book_authors_author_id` を作成
- DDLは手順書・DB設計書の確認ポイントを満たしている。

### Step 7 Flywayマイグレーション実行

- `./gradlew bootRun` を実行。
- 初回実行では Gradle Wrapper が `~/.gradle` 配下へアクセスできず、ビルド本体に入る前に停止。
  - 原因: サンドボックス権限による `Operation not permitted`
- 権限付きで `./gradlew bootRun` を再実行。
- Flywayログで以下を確認。
  - `Successfully validated 1 migration`
  - `Current version of schema "public": << Empty Schema >>`
  - `Migrating schema "public" to version "1 - create tables"`
  - `Successfully applied 1 migration to schema "public", now at version v1`
- Spring Boot起動ログで以下を確認。
  - `Tomcat started on port 8080`
  - `Started BookManagementApiApplicationKt`
- 起動後、以下でヘルスチェックを確認。

```bash
curl -i http://localhost:8080/health
```

- 確認結果:
  - HTTP status: `200`
  - response body: `{"status":"UP"}`
- 確認後、起動中のJavaプロセスを停止。
- 停止によりGradle上は `:bootRun FAILED` / exit `143` となったが、これは確認後にプロセスを終了したためであり、アプリ起動失敗ではない。

### Step 8 DBテーブル・制約・インデックス確認

- `psql` で `book_management` に接続し、DB状態を確認。
- テーブル一覧:
  - `authors`
  - `book_authors`
  - `books`
  - `flyway_schema_history`
- Flyway履歴:
  - version: `1`
  - description: `create tables`
  - script: `V1__create_tables.sql`
  - success: `true`
- インデックス:
  - `authors_pkey`
  - `books_pkey`
  - `book_authors_pkey`
  - `idx_book_authors_author_id`
  - Flyway管理テーブル用インデックス
- 制約:
  - `authors_pkey`: `PRIMARY KEY (id)`
  - `books_pkey`: `PRIMARY KEY (id)`
  - `chk_books_price`: `CHECK ((price >= 0))`
  - `chk_books_publication_status`: `UNPUBLISHED` / `PUBLISHED`
  - `book_authors_pkey`: `PRIMARY KEY (book_id, author_id)`
  - `fk_book_authors_book_id`: `books(id)` 参照、`ON DELETE CASCADE`
  - `fk_book_authors_author_id`: `authors(id)` 参照、`ON DELETE RESTRICT`
- `authors.birth_date` は `NOT NULL` だが、`CURRENT_DATE` を使ったCHECK制約は存在しないことを確認。

### Step 9 DB初期化し直し

- Step 7 / Step 8 でFlywayマイグレーションとDB確認が成功しているため、DB初期化し直しは不要。
- ユーザー確認のうえ、このStepはスキップ。

### Step 10 jOOQコード生成方針確認

- 現在の `build.gradle.kts` には `spring-boot-starter-jooq` は設定済み。
- ただし、DBスキーマからjOOQコードを生成するためのGradle plugin / codegen設定は未追加。
- Day 3では以下の方針で進める。
  - Flywayで作成済みのPostgreSQLスキーマからjOOQコードを生成する。
  - 生成先は `build/generated-src/jooq/main`。
  - 生成パッケージは `com.example.bookmanagement.jooq`。
  - `flyway_schema_history` は生成対象から除外する。
  - 生成コードはGit管理しない。
- `.gitignore` に `build/` が含まれているため、生成コードは通常Git差分に出ない。

### Step 11 jOOQ Gradle plugin追加

- `build.gradle.kts` の `plugins` に以下を追加。

```kotlin
id("nu.studer.jooq") version "9.0"
```

- Java 17構成のため、手順書どおり `nu.studer.jooq` は `9.0` を採用。

### Step 12 jOOQ codegen用依存関係追加

- `build.gradle.kts` の `dependencies` に以下を追加。

```kotlin
jooqGenerator("org.postgresql:postgresql")
```

- jOOQコード生成時にPostgreSQLへ接続するためのJDBC Driver依存として追加。

### Step 13 jOOQ生成設定追加

- 当初の `org.jooq.meta.kotlin.*` / `jooqConfiguration { ... }` 形式ではGradleスクリプト解釈時に未解決エラーが発生。
- 手順書修正に従い、`org.jooq.meta.jaxb.Configuration` を使う形式へ変更。
- `jooqConfiguration.set(...)` 形式で `./gradlew tasks --all` を実行した結果、以下の理由で失敗。
  - `jooqConfiguration.set(...)` が未解決。
- 手順書再修正に従い、`jooqConfiguration = Configuration()...` の直接代入形式へ変更。
- 再度 `./gradlew tasks --all` を実行した結果、以下の理由で失敗。
  - `jooqConfiguration = Configuration()` が `Val cannot be reassigned`。
- 手順書再々修正に従い、`jooqConfiguration.apply { withJdbc(...); withGenerator(...) }` 形式へ変更。
- 不要になった `import org.jooq.meta.jaxb.Configuration` は削除。
- 再度 `./gradlew tasks --all` を実行し、成功。
- `JOOQ tasks` に `generateJooq` が表示されることを確認。
- Step 13のjOOQ生成設定はGradleスクリプトとして解釈可能な状態になった。

### Step 14 生成コードをKotlin sourceSetsに追加

- `build.gradle.kts` の既存 `kotlin { ... }` ブロックに `sourceSets` 設定を追加。
- 追加内容:

```kotlin
kotlin {
    sourceSets {
        main {
            kotlin.srcDir("build/generated-src/jooq/main")
        }
    }
}
```

- jOOQ生成コードを `src/main/kotlin` 配下ではなく、`build/generated-src/jooq/main` 配下の再生成可能な成果物として扱う方針。
- `./gradlew tasks --all` を実行し、Gradleスクリプト解釈が成功することを確認。

### Step 15 jOOQコード生成実行

- `docker compose ps` でPostgreSQLコンテナの起動状態を確認。
  - container: `book-management-postgres`
  - status: `Up`
  - port: `0.0.0.0:5432->5432/tcp`
- `./gradlew generateJooq` を実行。
- 結果: `BUILD SUCCESSFUL`
- jOOQ codegenログで以下を確認。
  - jOOQ version: `3.19.32`
  - database URL: `jdbc:postgresql://localhost:5432/book_management`
  - target dir: `build/generated-src/jooq/main`
  - target package: `com.example.bookmanagement.jooq`
  - excludes: `flyway_schema_history`
  - fetched tables: `4`
  - included tables: `3`
  - excluded tables: `1`
- 生成対象テーブル:
  - `authors`
  - `book_authors`
  - `books`
- 生成された主なファイル:
  - `Authors.kt`
  - `BookAuthors.kt`
  - `Books.kt`
  - `AuthorsRecord.kt`
  - `BookAuthorsRecord.kt`
  - `BooksRecord.kt`
- jOOQ生成コードの作成まで成功。

### Step 16 生成コード確認

- `rg --files build/generated-src/jooq/main` で生成ファイルを確認。
- 生成ファイル:
  - `com/example/bookmanagement/jooq/DefaultCatalog.kt`
  - `com/example/bookmanagement/jooq/Public.kt`
  - `com/example/bookmanagement/jooq/indexes/Indexes.kt`
  - `com/example/bookmanagement/jooq/keys/Keys.kt`
  - `com/example/bookmanagement/jooq/tables/Authors.kt`
  - `com/example/bookmanagement/jooq/tables/BookAuthors.kt`
  - `com/example/bookmanagement/jooq/tables/Books.kt`
  - `com/example/bookmanagement/jooq/tables/references/Tables.kt`
  - `com/example/bookmanagement/jooq/tables/records/AuthorsRecord.kt`
  - `com/example/bookmanagement/jooq/tables/records/BookAuthorsRecord.kt`
  - `com/example/bookmanagement/jooq/tables/records/BooksRecord.kt`
- `Authors.kt` / `Books.kt` / `BookAuthors.kt` のpackageが `com.example.bookmanagement.jooq.tables` であることを確認。
- `Tables.kt` のpackageが `com.example.bookmanagement.jooq.tables.references` であることを確認。
- `Tables.kt` に以下の参照定数が生成されていることを確認。
  - `AUTHORS`
  - `BOOK_AUTHORS`
  - `BOOKS`
- `AuthorRepository` で利用するimportは `com.example.bookmanagement.jooq.tables.references.AUTHORS` で問題ない見込み。

### Step 17 生成コードをGit管理対象から除外する確認

- `.gitignore` に `build/` が含まれていることを確認。
- `git status --short` を確認し、`build/generated-src/jooq` 配下がGit差分に表示されていないことを確認。
- `git check-ignore -v build/generated-src/jooq/main/com/example/bookmanagement/jooq/tables/Authors.kt` を実行。
- 結果:
  - `.gitignore:3:build/` により生成コードがignore対象になっている。
- 追加の `.gitignore` 修正は不要。

### Step 18 Repository試作コード作成

- `src/main/kotlin/com/example/bookmanagement/repository/AuthorRepository.kt` を新規作成。
- 実装内容:
  - `DSLContext` をコンストラクタインジェクションする。
  - jOOQ生成コード `com.example.bookmanagement.jooq.tables.references.AUTHORS` を参照する。
  - `existsById(id: Long): Boolean` を実装する。
- 事前確認:
  - 生成済み `Authors.kt` に `AUTHORS.ID` が存在することを確認。
  - `Tables.kt` に `com.example.bookmanagement.jooq.tables.references.AUTHORS` が生成されていることを確認。
- `./gradlew compileKotlin` を実行。
- 結果: `BUILD SUCCESSFUL`
- Repository試作コードからjOOQ生成コードを参照できることを確認。

### Step 19 ビルド確認

- `./gradlew clean generateJooq build` を実行。
- 初回は `generateJooq` 成功後、`compileKotlin` で失敗。
- 失敗理由:
  - `compileKotlin` が `build/generated-src/jooq/main` を参照しているが、`generateJooq` との明示的な依存関係がGradle上宣言されていない。
  - Gradleのimplicit dependency validationにより停止。
- 対応:
  - `build.gradle.kts` に以下を追加。

```kotlin
tasks.named("compileKotlin") {
    dependsOn(tasks.named("generateJooq"))
}
```

- 再度 `./gradlew clean generateJooq build` を実行。
- 結果: `BUILD SUCCESSFUL`
- `clean` 後にjOOQコードを再生成し、その生成コードを使ってKotlinコンパイル・テスト・buildまで成功することを確認。

### Step 20 アプリケーション起動確認

- `./gradlew bootRun` を実行。
- `generateJooq` が実行された後、Spring Bootアプリケーションが起動。
- 起動ログで以下を確認。
  - `Tomcat started on port 8080`
  - `Started BookManagementApiApplicationKt`
  - Flyway: `Current version of schema "public": 1`
  - Flyway: `Schema "public" is up to date. No migration necessary.`
- 起動後、以下でヘルスチェックを確認。

```bash
curl -i http://localhost:8080/health
```

- 確認結果:
  - HTTP status: `200`
  - response body: `{"status":"UP"}`
- 確認後、`Ctrl + C` で `bootRun` を停止。
- 停止時の終了コードは `130` だが、これは確認後に手動停止したためであり、起動失敗ではない。

### Step 21 テスト実行

- `bootRun` 停止後に `./gradlew test` を実行。
- `compileKotlin` が `generateJooq` に依存しているため、テスト実行時にも `generateJooq` が実行された。
- jOOQ codegenログで以下を確認。
  - target package: `com.example.bookmanagement.jooq`
  - excludes: `flyway_schema_history`
  - fetched tables: `4`
  - included tables: `3`
  - excluded tables: `1`
- `test` タスクまで実行され、結果は `BUILD SUCCESSFUL`。

### Step 22 Git差分確認

- `git status --short --untracked-files=all` を実行。
- 現在の差分:
  - `M build.gradle.kts`
  - `?? document/develop/coding_test_day_2_project_setup_steps.md`
  - `?? document/develop/coding_test_day_3_db_flyway_jooq_steps.md`
  - `?? document/develop/day3_db_flyway_jooq_log.md`
  - `?? src/main/kotlin/com/example/bookmanagement/repository/AuthorRepository.kt`
  - `?? src/main/resources/db/migration/V1__create_tables.sql`
- `build.gradle.kts` の主な変更:
  - `nu.studer.jooq` plugin追加
  - `jooqGenerator("org.postgresql:postgresql")` 追加
  - jOOQ生成コードの `sourceSets` 追加
  - `compileKotlin` から `generateJooq` への依存関係追加
  - jOOQ生成設定追加
- `V1__create_tables.sql` の内容を確認。
  - `authors`
  - `books`
  - `book_authors`
  - `idx_book_authors_author_id`
- `AuthorRepository.kt` の内容を確認。
  - `com.example.bookmanagement.jooq.tables.references.AUTHORS` を参照。
  - `existsById(id: Long)` を実装。
- `git check-ignore -v build/generated-src/jooq/main/com/example/bookmanagement/jooq/tables/Authors.kt` を実行。
- 生成コードは `.gitignore:3:build/` によりGit管理対象外であることを確認。

### Step 23 commit

- commit対象は今回のDay 3実装差分と作業メモに限定する。
- ユーザー提供の手順書2件は未追跡のままとし、このcommitには含めない。
- commit対象:
  - `build.gradle.kts`
  - `src/main/resources/db/migration/V1__create_tables.sql`
  - `src/main/kotlin/com/example/bookmanagement/repository/AuthorRepository.kt`
  - `document/develop/day3_db_flyway_jooq_log.md`
- 以下のcommitを作成。

```text
67b55dc chore: setup Flyway migrations and jOOQ code generation
```

### Step 24 push

- `git push origin develop` を実行。
- 結果:

```text
0c5c712..67b55dc  develop -> develop
```

- `git log -1 --oneline --decorate` で以下を確認。

```text
67b55dc (HEAD -> develop, origin/develop) chore: setup Flyway migrations and jOOQ code generation
```

- Day 3のDB / Flyway / jOOQ構築commitが `origin/develop` に反映済みであることを確認。
