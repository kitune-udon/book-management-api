# 2日目 初期構築 作業メモ

## 2026-05-20

### Step 4 現在のリポジトリ状態確認

- 作業ブランチは `develop`。
- `git status` では作業ツリーは clean。
- `document` 配下に以下の仕様書が存在することを確認。
  - `api_design.md`
  - `application_design.md`
  - `business_rules_design.md`
  - `db_design.md`
  - `error_design.md`
  - `test_design.md`
- リポジトリ直下には `README.md` が存在。
- ローカル `develop` が `origin/develop` より 9 commits behind だったため、作業前に最新化が必要と判断。

### develop 最新化

- `git pull origin develop` を実行。
- fast-forward により、設計書6件が更新された。
- 更新対象:
  - `document/api_design.md`
  - `document/application_design.md`
  - `document/business_rules_design.md`
  - `document/db_design.md`
  - `document/error_design.md`
  - `document/test_design.md`
- 最新化後の作業ツリーは clean。

### Step 5 Spring Bootプロジェクト作成

- Spring Initializr から Kotlin / Spring Boot / Gradle Kotlin DSL の雛形を取得。
- 指定内容:
  - Project: Gradle - Kotlin
  - Language: Kotlin
  - Java: 17
  - Group: `com.example`
  - Artifact: `book-management-api`
  - Package: `com.example.bookmanagement`
  - Packaging: Jar
- 依存関係:
  - Spring Web
  - Validation
  - jOOQ Access
  - Flyway Migration
  - PostgreSQL Driver
- Spring Initializr のデフォルトでは Spring Boot `4.0.6` が生成された。
- 手順書・前提条件は Spring Boot `3.x系` 指定のため、公式メタデータで利用可能な3系安定版を確認し、Spring Boot `3.5.14` 指定で再生成した。
- 生成ファイルをリポジトリ直下へ配置した。
- 主な追加ファイル:
  - `build.gradle.kts`
  - `settings.gradle.kts`
  - `gradlew`
  - `gradlew.bat`
  - `gradle/wrapper/gradle-wrapper.jar`
  - `gradle/wrapper/gradle-wrapper.properties`
  - `src/main/kotlin/com/example/bookmanagement/BookManagementApiApplication.kt`
  - `src/main/resources/application.properties`
  - `src/test/kotlin/com/example/bookmanagement/BookManagementApiApplicationTests.kt`
- `HELP.md` は Spring Initializr 生成物だが、生成 `.gitignore` によりGit管理対象外。

### Step 6 settings.gradle.kts確認

- `settings.gradle.kts` の内容を確認。
- `rootProject.name = "book-management-api"` となっており、手順書の期待値どおり。
- Spring Boot `3.5.14` の安定版を利用しているため、`spring.io/milestone` / `spring.io/snapshot` の追加リポジトリ設定は不要と判断。
- 現時点では `settings.gradle.kts` の修正なし。

### Step 7 build.gradle.kts確認・調整

- `build.gradle.kts` の内容を確認。
- Spring Boot は `3.5.14`。
- Kotlin plugin は `1.9.25`。
- Java toolchain は `JavaLanguageVersion.of(17)`。
- リポジトリは `mavenCentral()`。
- 手順書で必要とされている以下の依存関係が設定済み。
  - `org.springframework.boot:spring-boot-starter-web`
  - `org.springframework.boot:spring-boot-starter-validation`
  - `org.springframework.boot:spring-boot-starter-jooq`
  - `org.flywaydb:flyway-core`
  - `org.flywaydb:flyway-database-postgresql`
  - `com.fasterxml.jackson.module:jackson-module-kotlin`
  - `org.jetbrains.kotlin:kotlin-reflect`
  - `org.postgresql:postgresql`
  - `org.springframework.boot:spring-boot-starter-test`
  - `org.jetbrains.kotlin:kotlin-test-junit5`
  - `org.junit.platform:junit-platform-launcher`
- Kotlin compiler option として `-Xjsr305=strict` が設定済み。
- `tasks.withType<Test> { useJUnitPlatform() }` が設定済み。
- `gradlew` には実行権限が付与済み。
- 現時点では `build.gradle.kts` の修正なし。

### Step 8 .gitignore確認

- Spring Initializr 生成の `.gitignore` を確認。
- Gradle関連:
  - `.gradle`
  - `build/`
  - `!gradle/wrapper/gradle-wrapper.jar`
- IDE関連:
  - IntelliJ IDEA
  - STS
  - NetBeans
  - VS Code
- Kotlin関連:
  - `.kotlin`
- 手順書の推奨に合わせ、以下を追記。
  - `.DS_Store`
  - `*.log`
  - `.env`
- 既に存在していた `.DS_Store` は無視対象であり、Git管理対象には入っていないことを確認。

### Step 9 docker-compose.yml作成

- リポジトリ直下に `docker-compose.yml` を新規作成。
- PostgreSQLコンテナ定義:
  - image: `postgres:16`
  - container_name: `book-management-postgres`
  - port: `5432:5432`
  - database: `book_management`
  - user: `book_user`
  - password: `book_password`
  - volume: `postgres_data:/var/lib/postgresql/data`
- DB接続情報はローカル開発用の簡易設定として扱う。

### Step 10 application.yml作成

- Spring Initializr 生成時の `src/main/resources/application.properties` はアプリケーション名のみだったため削除。
- `src/main/resources/application.yml` を新規作成。
- 設定内容:
  - `spring.application.name`: `book-management-api`
  - datasource URL: `jdbc:postgresql://localhost:5432/book_management`
  - datasource username: `book_user`
  - datasource password: `book_password`
  - datasource driver: `org.postgresql.Driver`
  - Flyway enabled: `true`
  - Flyway locations: `classpath:db/migration`
  - jOOQ sql dialect: `postgres`
  - server port: `8080`
- Flywayは手順書の最終状態に合わせて `enabled: true` のままとした。

### Step 11 最小のController作成

- 起動確認用APIとして独自 `HealthController` を採用。
- Spring Boot Actuator での代替も検討したが、今回は2日目の目的が最小起動確認であり、運用監視要件はスコープ外のため依存関係を増やさない方針とした。
- `src/main/kotlin/com/example/bookmanagement/controller/HealthController.kt` を新規作成。
- `GET /health` で以下を返す。

```json
{
  "status": "UP"
}
```

### Step 12 PostgreSQL起動

- `docker --version` と `docker compose version` を確認。
  - Docker: `28.0.1`
  - Docker Compose: `v2.33.1-desktop.1`
- 初回の `docker compose up -d` はDocker daemon未起動のため失敗。
- Docker Desktopを起動後、再度 `docker compose up -d` を実行。
- `postgres:16` イメージを取得し、`book-management-postgres` コンテナを起動。
- `docker compose ps` で以下を確認。
  - container: `book-management-postgres`
  - image: `postgres:16`
  - status: `Up`
  - port: `0.0.0.0:5432->5432/tcp`
- `docker compose logs --tail=80 postgres` で `database system is ready to accept connections` を確認。

### Step 13 DB接続確認

- ローカルに `psql` が存在することを確認。
- PostgreSQLコンテナが `Up` であることを再確認。
- 以下の接続情報で `psql` から接続確認。
  - host: `localhost`
  - port: `5432`
  - user: `book_user`
  - database: `book_management`
- 接続確認コマンド:

```bash
PGPASSWORD=book_password psql -h localhost -p 5432 -U book_user -d book_management -c 'select current_database() as database, current_user as user_name, version();'
```

- 接続結果:
  - database: `book_management`
  - user: `book_user`
  - PostgreSQL: `16.14`
- DB接続確認は成功。

### Step 14 Gradleビルド確認

- `./gradlew clean build` を実行。
- 初回実行では Gradle Wrapper が `~/.gradle` 配下のロックファイルにアクセスできず、ビルド本体に入る前に停止。
  - 原因: サンドボックス権限による `Operation not permitted`
- 権限付きで `./gradlew clean build` を再実行。
- 実行された主なタスク:
  - `clean`
  - `compileKotlin`
  - `processResources`
  - `bootJar`
  - `compileTestKotlin`
  - `test`
  - `build`
- 結果: `BUILD SUCCESSFUL`
- 所要時間: `18s`
- 補足:
  - Gradle 9.0 互換性に関する deprecated warning が表示されたが、ビルドは成功。
  - 現時点ではSpring Initializr生成構成由来の警告として扱い、修正は行わない。

### Step 15 アプリケーション起動確認

- `./gradlew bootRun` を実行。
- 初回実行では Step 14 と同様に Gradle Wrapper が `~/.gradle` 配下へアクセスできず、ビルド本体に入る前に停止。
  - 原因: サンドボックス権限による `Operation not permitted`
- 権限付きで `./gradlew bootRun` を再実行。
- Spring Boot 起動ログで以下を確認。
  - Spring Boot: `3.5.14`
  - Tomcat port: `8080`
  - DB接続: `jdbc:postgresql://localhost:5432/book_management`
  - Flyway: `Successfully validated 0 migrations`
  - Flyway警告: `No migrations found. Are your locations set up correctly?`
  - 起動完了: `Started BookManagementApiApplicationKt`
- Flyway警告は、2日目時点でマイグレーションファイル未作成のため発生。3日目で `V1__create_tables.sql` を作成予定。
- 起動後、以下でヘルスチェックを確認。

```bash
curl -i http://localhost:8080/health
```

- 確認結果:
  - HTTP status: `200`
  - response body: `{"status":"UP"}`
- 確認後、起動中のJavaプロセスを停止。
- 停止によりGradle上は `:bootRun FAILED` / exit `143` となったが、これは確認後にプロセスを終了したためであり、アプリ起動失敗ではない。

### Step 16 テスト実行確認

- `./gradlew test` を実行。
- Spring Initializr 生成の `BookManagementApiApplicationTests#contextLoads` を含むテストを確認。
- 結果: `BUILD SUCCESSFUL`
- 所要時間: `2s`
- 補足:
  - Gradle 9.0 互換性に関する deprecated warning は Step 14 と同様に表示。
  - テスト自体は成功。

### Step 17 Git差分確認

- `git status` を確認。
- ブランチ状態:
  - `develop`
  - `origin/develop` と同期済み
- 未追跡ファイルとして、初期構築の新規ファイルが存在。
- 追跡予定の新規ファイル:
  - `.gitattributes`
  - `.gitignore`
  - `build.gradle.kts`
  - `docker-compose.yml`
  - `document/develop/day2_initial_setup_log.md`
  - `gradle/wrapper/gradle-wrapper.jar`
  - `gradle/wrapper/gradle-wrapper.properties`
  - `gradlew`
  - `gradlew.bat`
  - `settings.gradle.kts`
  - `src/main/kotlin/com/example/bookmanagement/BookManagementApiApplication.kt`
  - `src/main/kotlin/com/example/bookmanagement/controller/HealthController.kt`
  - `src/main/resources/application.yml`
  - `src/test/kotlin/com/example/bookmanagement/BookManagementApiApplicationTests.kt`
- `git diff` / `git diff --stat` は、現時点の差分がすべて未追跡ファイルのため出力なし。
- `.gradle/`、`build/`、`.DS_Store`、`HELP.md` は存在するが `.gitignore` により追跡対象外。

### Step 18 不要ファイル確認

- `git ls-files --others --exclude-standard` で追跡予定ファイルを確認。
- 追跡予定に以下が含まれていないことを確認。
  - `.env`
  - `.DS_Store`
  - `build/`
  - `.gradle/`
  - `.idea/`
  - `*.log`
- `git status --ignored --short` で以下が無視対象であることを確認。
  - `.DS_Store`
  - `.gradle/`
  - `HELP.md`
  - `build/`
  - `document/.DS_Store`
- ローカル絶対パスや個人ディレクトリ情報が追跡予定ファイルに含まれていないことを確認。
- `docker-compose.yml` と `application.yml` にはローカル開発用DB接続情報として `book_password` が含まれる。
  - 手順書どおりのローカル簡易設定であり、本番用機密情報ではない。
  - README作成時にローカル開発用であることを明記する。

### Step 19 commit

- 初期構築内容をステージングし、以下のコミットメッセージでコミットする。

```bash
git add .
git commit -m "chore: initialize Spring Boot Kotlin project"
```
