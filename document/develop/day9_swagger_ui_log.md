# 9日目 Swagger UI追加 作業メモ

## 2026-05-21

### Step 1 developブランチ最新化

- `git checkout develop` を実行。
  - 結果: `Already on 'develop'`
- `git pull origin develop` を実行。
  - 結果: `Already up to date.`
- 作業開始時点の最新commit:
  - `411a54b (HEAD -> develop, origin/develop) docs: update final check log`
- 作業開始時点の差分:
  - `document/develop/作業手順/` 配下の作業手順書が未追跡ファイルとして存在。
  - これまでの運用どおり、作業手順書は提出用commitに含めない。

### Step 2 作業メモ作成

- `document/develop/day9_swagger_ui_log.md` を作成。

### Step 3 build.gradle.ktsに依存関係を追加

- `build.gradle.kts` の `dependencies` に以下を追加。
  - `implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")`

### Step 4 OpenApiConfig.ktを作成

- `src/main/kotlin/com/example/bookmanagement/config/OpenApiConfig.kt` を作成。
- OpenAPIのタイトル、バージョン、概要のみを定義。
  - title: `Book Management API`
  - version: `1.0.0`
  - description: `書籍と著者を管理するためのREST API`

### Step 5 Controllerへの詳細アノテーション追加判断

- 提出前の変更量を最小化するため、Controller / DTOへの詳細OpenAPIアノテーションは追加しない。
- API一覧・リクエスト/レスポンス構造はspringdocの自動生成に任せる。

### Step 6 build実行

- `docker compose up -d` を実行。
  - 結果: PostgreSQLコンテナを起動。
- `docker exec book-management-postgres pg_isready -U book_user -d book_management` を実行。
  - 結果: `/var/run/postgresql:5432 - accepting connections`
- `./gradlew clean generateJooq build` を実行。
  - 結果: `BUILD SUCCESSFUL`
- 補足:
  - 通常実行ではGradle wrapperのロックファイル権限により失敗したため、権限付きで再実行。
  - springdoc依存関係と `OpenApiConfig.kt` のimport解決に問題なし。

### Step 7 test実行

- `./gradlew test` を実行。
  - 結果: `BUILD SUCCESSFUL`

### Step 8 アプリケーション起動

- `./gradlew bootRun` を実行。
  - 結果: アプリケーションが8080番ポートで起動。

### Step 9 Swagger UI確認

- `curl` でSwagger UIを確認。
  - `http://localhost:8080/swagger-ui.html`: 302
  - `http://localhost:8080/swagger-ui/index.html`: 200
- `/swagger-ui.html` はSwagger UI本体へリダイレクトされることを確認。

### Step 10 OpenAPI JSON確認

- `curl` で `http://localhost:8080/v3/api-docs` を確認。
  - 結果: 200
- 確認内容:
  - `openapi`: `3.1.0`
  - `info.title`: `Book Management API`
  - 必須API5本のpathが含まれることを確認。
    - `/authors`
    - `/authors/{authorId}`
    - `/authors/{authorId}/books`
    - `/books`
    - `/books/{bookId}`
- 確認後、起動中のアプリケーションプロセスを停止。
  - 停止により `bootRun` はGradle上 `143` で終了したが、起動・Swagger確認は成功。

### Step 11 README更新

- `README.md` を更新。
- 技術スタックに `springdoc-openapi / Swagger UI` を追記。
- Swagger UI / OpenAPIの確認URLを追記。
  - `http://localhost:8080/swagger-ui.html`
  - `http://localhost:8080/v3/api-docs`
- 実装上の判断に、詳細な業務ルール・エラー仕様はREADMEと設計書で補足する方針を追記。

### Step 12 設計書・WBSの対象外記載確認

- 以下を確認。
  - `README.md`
  - `document/application_design.md`
  - `document/api_design.md`
  - `document/test_design.md`
- `Swagger` / `OpenAPI` の対象外記載は見つからなかったため、設計書の追加修正は行わない。

### Step 13 Git差分確認

- `git status --short --untracked-files=all` を実行。
  - Swagger追加の変更として、`README.md` / `build.gradle.kts` / `OpenApiConfig.kt` / 本作業メモを確認。
  - `document/develop/作業手順/` 配下の作業手順書は未追跡のまま残し、commit対象外とする。
- `git diff --check` を実行。
  - 結果: 指摘なし。
- `git ls-files | rg '(^build/|^\\.gradle/|^\\.idea/)'` を実行。
  - 結果: 該当なし。

### Step 14 commit

- 以下のcommitを作成。
  - `37d6c16 docs: add Swagger UI`
- commit対象:
  - `build.gradle.kts`
  - `README.md`
  - `src/main/kotlin/com/example/bookmanagement/config/OpenApiConfig.kt`
  - `document/develop/day9_swagger_ui_log.md`

### Step 15 push

- `git push origin develop` を実行。
  - 結果: `develop -> develop`
