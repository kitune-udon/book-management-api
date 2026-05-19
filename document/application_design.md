# アプリケーション構成設計書

<details>
<summary>1. 目的</summary>

本ドキュメントは、書籍管理システムのバックエンドAPIにおけるアプリケーション構成、レイヤー責務、パッケージ構成、トランザクション方針を定義する。


</details>



<details>
<summary>2. 基本方針</summary>

今回の実装では、過度に複雑なアーキテクチャを採用せず、以下のシンプルな3層構成を基本とする。

```text
Controller
  ↓
Service
  ↓
Repository
  ↓
jOOQ / Database
```

方針は以下とする。

- ControllerはHTTPリクエスト・レスポンスの制御に集中する
- Serviceは業務ルールとトランザクション制御を担当する
- RepositoryはjOOQを使ったDBアクセスに集中する
- DTOはAPIの入出力専用とする
- DB制約とService層のバリデーションを併用する
- 小規模課題のため、DDDの過度な導入は避ける


</details>



<details>
<summary>3. レイヤー責務</summary>

| レイヤー | 主な責務 | 実装例 |
|---|---|---|
| Controller | HTTPリクエスト受付、レスポンス返却 | AuthorController / BookController |
| Service | 業務ルール、ユースケース制御、トランザクション | AuthorService / BookService |
| Repository | DBアクセス、jOOQ DSL実行 | AuthorRepository / BookRepository |
| DTO | API入出力データ定義 | CreateBookRequest / BookResponse |
| Exception | 例外定義、エラーレスポンス統一 | ApiExceptionHandler |
| Model | 業務上の値定義 | PublicationStatus |


</details>



<details>
<summary>4. パッケージ構成</summary>

```text
src/main/kotlin/com/example/bookmanagement
  controller
    AuthorController.kt
    BookController.kt

  service
    AuthorService.kt
    BookService.kt

  repository
    AuthorRepository.kt
    BookRepository.kt

  dto
    author
      CreateAuthorRequest.kt
      UpdateAuthorRequest.kt
      AuthorResponse.kt
    book
      CreateBookRequest.kt
      UpdateBookRequest.kt
      BookResponse.kt
      BookSummaryResponse.kt

  exception
    ApiExceptionHandler.kt
    ErrorResponse.kt
    NotFoundException.kt
    BusinessRuleViolationException.kt

  model
    PublicationStatus.kt
```


</details>



<details>
<summary>5. Controller設計</summary>

## 5.1 AuthorController

### 責務

- 著者登録APIを提供する
- 著者更新APIを提供する
- 著者別書籍取得APIを提供する

### エンドポイント

| メソッド | パス | 呼び出し先Service |
|---|---|---|
| POST | `/authors` | AuthorService#create |
| PUT | `/authors/{authorId}` | AuthorService#update |
| GET | `/authors/{authorId}/books` | BookService#findBooksByAuthorId または AuthorService#findBooks |



## 5.2 BookController

### 責務

- 書籍登録APIを提供する
- 書籍更新APIを提供する

### エンドポイント

| メソッド | パス | 呼び出し先Service |
|---|---|---|
| POST | `/books` | BookService#create |
| PUT | `/books/{bookId}` | BookService#update |


</details>



<details>
<summary>6. Service設計</summary>

## 6.1 AuthorService

### 責務

- 著者登録処理
- 著者更新処理
- 著者存在チェック
- 生年月日バリデーション

### 主なメソッド案

```kotlin
fun create(request: CreateAuthorRequest): AuthorResponse

fun update(authorId: Long, request: UpdateAuthorRequest): AuthorResponse

fun validateAuthorExists(authorId: Long)
```



## 6.2 BookService

### 責務

- 書籍登録処理
- 書籍更新処理
- 書籍存在チェック
- 書籍と著者の関連制御
- 著者ID一覧の存在チェック
- 価格バリデーション
- 著者数バリデーション
- authorIds重複チェック
- 出版状態遷移チェック
- 著者別書籍取得

### 主なメソッド案

```kotlin
fun create(request: CreateBookRequest): BookResponse

fun update(bookId: Long, request: UpdateBookRequest): BookResponse

fun findBooksByAuthorId(authorId: Long): List<BookSummaryResponse>
```


</details>



<details>
<summary>7. Repository設計</summary>

## 7.1 AuthorRepository

### 責務

- 著者登録
- 著者更新
- 著者ID検索
- 著者ID一覧の存在確認

### 主なメソッド案

```kotlin
fun insert(name: String, birthDate: LocalDate): AuthorRecord

fun update(id: Long, name: String, birthDate: LocalDate): AuthorRecord?

fun findById(id: Long): AuthorRecord?

fun countByIds(ids: List<Long>): Int
```



## 7.2 BookRepository

### 責務

- 書籍登録
- 書籍更新
- 書籍ID検索
- 書籍と著者の関連登録
- 書籍と著者の関連削除
- 著者別書籍取得

### 主なメソッド案

```kotlin
fun insert(title: String, price: Int, publicationStatus: PublicationStatus): BookRecord

fun update(id: Long, title: String, price: Int, publicationStatus: PublicationStatus): BookRecord?

fun findById(id: Long): BookRecord?

fun insertBookAuthors(bookId: Long, authorIds: List<Long>)

fun deleteBookAuthors(bookId: Long)

fun findAuthorsByBookId(bookId: Long): List<AuthorRecord>

fun findBooksByAuthorId(authorId: Long): List<BookRecord>
```


</details>



<details>
<summary>8. jOOQ利用方針</summary>

## 8.1 基本方針

- Repository層で `DSLContext` を利用する
- Flywayで作成したDBスキーマから生成されたjOOQコードを利用する
- 生SQL文字列の多用は避ける
- select / insert / update / joinをjOOQ DSLで表現する

## 8.2 実装例

```kotlin
@Repository
class AuthorRepository(
    private val dsl: DSLContext
) {
    fun findById(id: Long): AuthorRecord? {
        return dsl.selectFrom(AUTHORS)
            .where(AUTHORS.ID.eq(id))
            .fetchOne()
    }
}
```


</details>



<details>
<summary>9. DTO設計方針</summary>

## 9.1 方針

- Request DTOとResponse DTOを分ける
- DBのRecordをControllerへ直接返さない
- APIレスポンスに必要な項目だけをDTOに詰め替える
- Kotlinのdata classを利用する
- 不要なnullableは避ける

## 9.2 DTO一覧

| DTO | 用途 |
|---|---|
| CreateAuthorRequest | 著者登録リクエスト |
| UpdateAuthorRequest | 著者更新リクエスト |
| AuthorResponse | 著者レスポンス |
| CreateBookRequest | 書籍登録リクエスト |
| UpdateBookRequest | 書籍更新リクエスト |
| BookResponse | 書籍詳細レスポンス |
| BookSummaryResponse | 書籍一覧用レスポンス |
| ErrorResponse | エラーレスポンス |


</details>



<details>
<summary>10. トランザクション設計</summary>

## 10.1 トランザクションが必要な処理

| 処理 | 理由 |
|---|---|
| 書籍登録 | books登録とbook_authors登録を一体で扱うため |
| 書籍更新 | books更新とbook_authors削除・再登録を一体で扱うため |

## 10.2 実装方針

- Service層の書籍登録・更新メソッドに `@Transactional` を付与する
- 著者登録・著者更新は単一テーブル更新だが、必要に応じて `@Transactional` を付与してもよい
- Repository層ではトランザクション境界を持たせない

## 10.3 書籍登録処理の流れ

```text
1. リクエスト検証
2. 著者ID一覧の存在確認
3. booksへ登録
4. book_authorsへ登録
5. 登録結果を取得してレスポンス生成
```

## 10.4 書籍更新処理の流れ

```text
1. 更新対象書籍の存在確認
2. リクエスト検証
3. 著者ID一覧の存在確認
4. 出版状態遷移チェック
5. booksを更新
6. book_authorsを削除
7. book_authorsを再登録
8. 更新結果を取得してレスポンス生成
```


</details>



<details>
<summary>11. PublicationStatus設計</summary>

## 11.1 enum定義

```kotlin
enum class PublicationStatus {
    UNPUBLISHED,
    PUBLISHED
}
```

## 11.2 利用箇所

- CreateBookRequest
- UpdateBookRequest
- BookResponse
- BookSummaryResponse
- BookServiceの出版状態遷移チェック
- RepositoryでDB保存時の文字列変換


</details>



<details>
<summary>12. 実装対象外</summary>

以下は今回のアプリケーション構成には含めない。

| 対象外 | 理由 |
|---|---|
| 認証・認可 | 課題の中心ではないため |
| フロントエンド | バックエンドAPI課題のため |
| DDDの厳密なレイヤー分離 | 小規模課題では過剰なため |
| CQRS | 課題規模に対して過剰なため |
| Event Sourcing | 課題規模に対して過剰なため |
| 本格的なログ設計 | 課題範囲外のため |
| 本格的な監視設計 | 課題範囲外のため |


</details>



<details>
<summary>13. 設計上の判断</summary>

## 13.1 シンプルな3層構成にする理由

今回の課題は小規模なバックエンドAPIであり、評価されるべきポイントは、仕様理解・DB設計・業務ルール実装・テストである。  
そのため、過度な設計パターンを導入せず、Controller / Service / Repository の明確な責務分離を優先する。

## 13.2 業務ルールをService層に集約する理由

業務ルールをControllerやRepositoryに分散させると、見通しが悪くなる。  
Service層に集約することで、テストしやすく、評価者にも実装意図が伝わりやすくなる。

## 13.3 DB Recordを直接返さない理由

jOOQのRecordはDB構造に依存しているため、APIレスポンスとして直接返すとDB設計とAPI設計が密結合になる。  
DTOへ変換することで、APIの入出力を明確にする。


</details>
