# Book Management API

## 概要

書籍と著者を管理するためのREST APIです。

<details>
<summary>技術スタック</summary>

## 技術スタック

- Kotlin 1.9.25
- Java 17
- Spring Boot 3.5.14
- PostgreSQL 16
- Flyway
- jOOQ 3.19.32
- springdoc-openapi / Swagger UI
- Gradle Wrapper

</details>

<details>
<summary>アプリケーション構成</summary>

## アプリケーション構成

- `controller`: HTTPリクエストを受け取り、入力値検証後にServiceへ処理を委譲します。
- `service`: 業務ルール検証とトランザクション境界を担当します。
- `repository`: jOOQを使ったDBアクセスを担当します。
- `dto`: APIのリクエスト・レスポンス形式を定義します。
- `exception`: 共通エラーレスポンスへの変換を担当します。
- `src/main/resources/db/migration`: FlywayマイグレーションSQLを配置します。

</details>

<details>
<summary>セットアップ</summary>

## セットアップ

### 前提

- Java 17
- Docker / Docker Compose

Gradle Wrapperを同梱しているため、ローカルGradleの事前インストールは不要です。

### DB起動

```bash
docker compose up -d
```

PostgreSQLは以下の設定で起動します。

| 項目 | 値 |
|---|---|
| DB | `book_management` |
| User | `book_user` |
| Password | `book_password` |
| Port | `5432` |

起動確認:

```bash
docker compose ps
```

DBが接続可能になるまで待機:

```bash
until docker exec book-management-postgres pg_isready -U book_user -d book_management; do sleep 1; done
```

</details>

<details>
<summary>起動手順</summary>

## 起動手順

このプロジェクトでは、jOOQコード生成時にPostgreSQLへ接続します。
`generateJooq` / `build` / `test` / `bootRun` を実行する前に、`docker compose up -d` でDBを起動してください。
`generateJooq` は `flywayMigrate` に依存しているため、ビルド時にFlywayマイグレーションが自動適用されてからjOOQコード生成が実行されます。

### jOOQコード生成・ビルド

```bash
./gradlew clean generateJooq build
```

必要に応じてFlywayの状態確認や手動マイグレーションも実行できます。

```bash
./gradlew flywayInfo
./gradlew flywayMigrate
```

### アプリケーション起動

```bash
./gradlew bootRun
```

### ヘルスチェック

```bash
curl -i http://localhost:8080/health
```

期待レスポンス:

```json
{
  "status": "UP"
}
```

### セットアップから起動まで一括実行

以下のスクリプトで、DB起動、DB接続待機、ビルド・テスト、アプリケーション起動、ヘルスチェック確認までをまとめて実行できます。

```bash
./scripts/setup-and-run.sh
```

起動後はアプリケーションプロセスがフォアグラウンドで実行されます。
終了する場合は `Ctrl+C` を押してください。

</details>

<details>
<summary>テスト実行</summary>

## テスト実行

```bash
./gradlew test
```

### テスト内容

Controller統合テストを中心に、APIとしてのHTTPステータス、レスポンス形式、Validation、例外ハンドリング、業務ルールを確認しています。
テストで登録・更新したデータはテストメソッド終了時にrollbackされるため、ローカルDBにテストデータは残りません。

| テストクラス | 主な確認内容 |
|---|---|
| `AuthorControllerTest` | 著者登録・更新、著者別書籍取得、著者APIの入力不正・404制御 |
| `BookControllerTest` | 書籍登録・更新、著者関連の更新、多対多関連、出版状態の業務ルール、書籍APIの入力不正・404制御 |

主なテスト観点:

- 正常に登録・更新・取得できること
- Validationエラーが400として返ること
- 存在しない著者・書籍に対して404を返すこと
- 書籍と著者の多対多関連を正しく登録・更新できること
- 出版済み書籍を未出版へ戻せないこと
- JSON形式不正、日付形式不正、enum不正、型不一致、パスID型不一致が500にならず400として返ること

詳細なテストケースは [`document/test_design.md`](document/test_design.md) を参照してください。

</details>

<details>
<summary>Swagger UI / OpenAPI</summary>

## Swagger UI / OpenAPI

アプリケーション起動後、以下のURLからSwagger UIを確認できます。

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSONは以下から確認できます。

```text
http://localhost:8080/v3/api-docs
```

Swagger UIでは、実装済みAPIのパス、HTTPメソッド、リクエスト/レスポンス構造を確認できます。
詳細な業務ルールやエラー方針は、本READMEの「主な業務ルール」「エラーレスポンス」「実装上の判断」を参照してください。

</details>

<details>
<summary>API仕様</summary>

## API仕様

### API一覧

| メソッド | パス | 概要 |
|---|---|---|
| POST | `/authors` | 著者を登録する |
| PUT | `/authors/{authorId}` | 著者を更新する |
| POST | `/books` | 書籍を登録する |
| PUT | `/books/{bookId}` | 書籍を更新する |
| GET | `/authors/{authorId}/books` | 指定した著者に紐づく書籍一覧を取得する |

### 著者登録API

```bash
curl -i -X POST http://localhost:8080/authors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "夏目漱石",
    "birthDate": "1867-02-09"
  }'
```

正常時は `201 Created` を返します。

```json
{
  "id": 1,
  "name": "夏目漱石",
  "birthDate": "1867-02-09"
}
```

### 著者更新API

```bash
curl -i -X PUT http://localhost:8080/authors/{authorId} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "夏目漱石 更新",
    "birthDate": "1867-02-09"
  }'
```

`{authorId}` には実在する著者IDを指定してください。
正常時は `200 OK` を返します。

```json
{
  "id": 1,
  "name": "夏目漱石 更新",
  "birthDate": "1867-02-09"
}
```

### 書籍登録API

```bash
curl -i -X POST http://localhost:8080/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "吾輩は猫である",
    "price": 1200,
    "publicationStatus": "PUBLISHED",
    "authorIds": [<AUTHOR_ID>]
  }'
```

`authorIds` には実在する著者IDを指定してください。
正常時は `201 Created` を返します。

```json
{
  "id": 1,
  "title": "吾輩は猫である",
  "price": 1200,
  "publicationStatus": "PUBLISHED",
  "authors": [
    {
      "id": 1,
      "name": "夏目漱石",
      "birthDate": "1867-02-09"
    }
  ]
}
```

### 書籍更新API

```bash
curl -i -X PUT http://localhost:8080/books/{bookId} \
  -H "Content-Type: application/json" \
  -d '{
    "title": "吾輩は猫である 改訂版",
    "price": 1500,
    "publicationStatus": "PUBLISHED",
    "authorIds": [<AUTHOR_ID>]
  }'
```

`{bookId}` には実在する書籍IDを指定してください。
`authorIds` には実在する著者IDを指定してください。
正常時は `200 OK` を返します。

```json
{
  "id": 1,
  "title": "吾輩は猫である 改訂版",
  "price": 1500,
  "publicationStatus": "PUBLISHED",
  "authors": [
    {
      "id": 1,
      "name": "夏目漱石",
      "birthDate": "1867-02-09"
    }
  ]
}
```

### 著者別書籍取得API

```bash
curl -i http://localhost:8080/authors/{authorId}/books
```

`{authorId}` には実在する著者IDを指定してください。
正常時は `200 OK` を返します。
著者は存在するが紐づく書籍が0件の場合は、`200 OK` で空配列を返します。

```json
[
  {
    "id": 1,
    "title": "吾輩は猫である",
    "price": 1200,
    "publicationStatus": "PUBLISHED"
  }
]
```

</details>

<details>
<summary>主な業務ルール</summary>

## 主な業務ルール

- 著者の生年月日は現在日以前のみ許可します。
- 書籍価格は0以上のみ許可します。
- 書籍には1人以上の著者が必要です。
- 書籍に同じ著者を重複して紐づけることはできません。
- 存在しない著者IDを指定して書籍を登録・更新することはできません。
- 出版済みの書籍を未出版へ戻すことはできません。
- 著者が存在するが紐づく書籍が0件の場合、著者別書籍取得APIは空配列を返します。

</details>

<details>
<summary>エラーレスポンス</summary>

## エラーレスポンス

エラー時は以下の形式でレスポンスを返します。

```json
{
  "status": 400,
  "message": "Invalid request body"
}
```

代表例:

| ケース | ステータス |
|---|---:|
| 入力値のValidationエラー | 400 |
| JSON形式不正、日付形式不正、enum不正、型不一致 | 400 |
| パスパラメータ型不一致 | 400 |
| 業務ルール違反 | 400 |
| 更新対象の著者・書籍が存在しない | 404 |
| 著者別書籍取得で指定著者が存在しない | 404 |

</details>

<details>
<summary>実装上の判断</summary>

## 実装上の判断

- DBマイグレーションにはFlywayを利用しています。
- DBアクセスにはjOOQを利用しています。
- jOOQ生成コードは `build/generated-src/jooq/main` に出力し、Git管理対象外としています。
- DB状態や複数項目に依存する業務ルールはService層で検証しています。
- JSON形式不正、日付形式不正、enum不正、型不一致は `HttpMessageNotReadableException` として400に変換しています。
- パスパラメータ型不一致は `MethodArgumentTypeMismatchException` として400に変換しています。
- 書籍登録・更新と著者関連の登録・更新は同一トランザクションで処理しています。
- `book_authors.author_id` にインデックスを付与し、著者別書籍取得時の検索を考慮しています。
- 著者別書籍取得APIでは、一覧用途の `BookSummaryResponse` を使い、各書籍に著者一覧は含めません。
- Swagger UIはAPI確認性向上のために導入し、詳細な業務ルール・エラー仕様はREADMEと設計書で補足しています。

</details>

<details>
<summary>対象外としたこと</summary>

## 対象外としたこと

- 認証・認可
- フロントエンド
- 書籍削除API
- 著者削除API
- 書籍一覧API
- 書籍詳細API
- 著者詳細API
- ページング・検索条件指定

</details>

<details>
<summary>関連ドキュメント</summary>

## 関連ドキュメント

設計資料は `document/` 配下に格納しています。

| ファイル | 内容 |
|---|---|
| [`document/api_design.md`](document/api_design.md) | API仕様、リクエスト・レスポンス、エラー仕様 |
| [`document/application_design.md`](document/application_design.md) | アプリケーション構成、レイヤ構成、責務分担 |
| [`document/business_rules_design.md`](document/business_rules_design.md) | 業務ルール、Validation、制約方針 |
| [`document/db_design.md`](document/db_design.md) | テーブル設計、ER、インデックス方針 |
| [`document/error_design.md`](document/error_design.md) | エラーハンドリング、共通エラーレスポンス方針 |
| [`document/test_design.md`](document/test_design.md) | テスト方針、テストケース、確認観点 |

</details>
