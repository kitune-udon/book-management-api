# エラー設計書

<details>
<summary>1. 目的</summary>

本ドキュメントは、書籍管理システムのバックエンドAPIにおけるエラーハンドリング方針を定義する。

目的は以下である。

- API利用者に分かりやすいエラーを返す
- エラーレスポンス形式を統一する
- 入力値不正・業務ルール違反・対象データなしを適切に区別する
- Controllerごとの個別エラー処理を避け、共通例外ハンドラーで制御する


</details>



<details>
<summary>2. 共通エラーレスポンス</summary>

## 2.1 レスポンス形式

エラーレスポンスは以下の形式に統一する。

```json
{
  "status": 400,
  "message": "Book price must be greater than or equal to 0"
}
```

## 2.2 項目定義

| 項目 | 型 | 内容 |
|---|---|---|
| status | number | HTTPステータスコード |
| message | string | エラー内容 |

## 2.3 Kotlin DTO案

```kotlin
data class ErrorResponse(
    val status: Int,
    val message: String
)
```


</details>



<details>
<summary>3. HTTPステータス方針</summary>

| ケース | ステータス | 例 |
|---|---|---|
| 入力値不正 | 400 Bad Request | priceがマイナス、nameが空 |
| 業務ルール違反 | 400 Bad Request | 出版済みから未出版へ戻す |
| 関連リソース指定不正 | 400 Bad Request | 存在しないauthorIdを指定して書籍登録 |
| 対象データなし | 404 Not Found | 存在しないbookIdを更新 |
| 想定外エラー | 500 Internal Server Error | 未考慮の例外 |


</details>



<details>
<summary>4. 例外クラス設計</summary>

## 4.1 NotFoundException

### 用途

指定されたリソースが存在しない場合に利用する。

### 利用例

- 存在しない著者IDを指定して著者更新した
- 存在しない書籍IDを指定して書籍更新した
- 存在しない著者IDを指定して著者別書籍一覧を取得しようとした

### ステータス

```text
404 Not Found
```

### Kotlin案

```kotlin
class NotFoundException(message: String) : RuntimeException(message)
```



## 4.2 BusinessRuleViolationException

### 用途

入力値としては受け取れたが、業務ルールに違反している場合に利用する。

### 利用例

- 書籍価格がマイナス
- 著者が1人も指定されていない
- 出版済み書籍を未出版に戻そうとした
- 著者の生年月日が未来日
- authorIdsに重複がある
- 存在しないauthorIdを指定して書籍登録・更新しようとした

### ステータス

```text
400 Bad Request
```

### Kotlin案

```kotlin
class BusinessRuleViolationException(message: String) : RuntimeException(message)
```


</details>



<details>
<summary>5. 共通例外ハンドラー設計</summary>

## 5.1 ApiExceptionHandler

Spring Bootの `@RestControllerAdvice` を利用し、API全体の例外レスポンスを統一する。

### Kotlin案

```kotlin
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(BusinessRuleViolationException::class)
    fun handleBusinessRuleViolationException(
        exception: BusinessRuleViolationException
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    message = exception.message ?: "Bad request"
                )
            )
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(
        exception: NotFoundException
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    status = HttpStatus.NOT_FOUND.value(),
                    message = exception.message ?: "Resource not found"
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        exception: Exception
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    message = "Internal server error"
                )
            )
    }
}
```


</details>



<details>
<summary>6. エラーケース一覧</summary>

## 6.1 著者API

| API | ケース | ステータス | 例外 |
|---|---|---|---|
| POST /authors | nameが空 | 400 | BusinessRuleViolationException |
| POST /authors | birthDateが未来日 | 400 | BusinessRuleViolationException |
| PUT /authors/{authorId} | authorIdが存在しない | 404 | NotFoundException |
| PUT /authors/{authorId} | nameが空 | 400 | BusinessRuleViolationException |
| PUT /authors/{authorId} | birthDateが未来日 | 400 | BusinessRuleViolationException |



## 6.2 書籍API

| API | ケース | ステータス | 例外 |
|---|---|---|---|
| POST /books | titleが空 | 400 | BusinessRuleViolationException |
| POST /books | priceがマイナス | 400 | BusinessRuleViolationException |
| POST /books | authorIdsが空 | 400 | BusinessRuleViolationException |
| POST /books | authorIdsに重複がある | 400 | BusinessRuleViolationException |
| POST /books | 存在しないauthorIdを指定 | 400 | BusinessRuleViolationException |
| PUT /books/{bookId} | bookIdが存在しない | 404 | NotFoundException |
| PUT /books/{bookId} | titleが空 | 400 | BusinessRuleViolationException |
| PUT /books/{bookId} | priceがマイナス | 400 | BusinessRuleViolationException |
| PUT /books/{bookId} | authorIdsが空 | 400 | BusinessRuleViolationException |
| PUT /books/{bookId} | authorIdsに重複がある | 400 | BusinessRuleViolationException |
| PUT /books/{bookId} | 存在しないauthorIdを指定 | 400 | BusinessRuleViolationException |
| PUT /books/{bookId} | PUBLISHEDからUNPUBLISHEDへ変更 | 400 | BusinessRuleViolationException |



## 6.3 著者別書籍取得API

| API | ケース | ステータス | 例外 |
|---|---|---|---|
| GET /authors/{authorId}/books | authorIdが存在しない | 404 | NotFoundException |


</details>



<details>
<summary>7. 入力値形式不正の扱い</summary>

## 7.1 日付形式不正

例：

```json
{
  "name": "夏目漱石",
  "birthDate": "invalid-date"
}
```

Spring/Jacksonの変換エラーとして扱われる想定。  
余力があれば `HttpMessageNotReadableException` をハンドリングして400を返す。

## 7.2 enum形式不正

例：

```json
{
  "title": "吾輩は猫である",
  "price": 1200,
  "publicationStatus": "DRAFT",
  "authorIds": [1]
}
```

Spring/Jacksonの変換エラーとして扱われる想定。  
余力があれば `HttpMessageNotReadableException` をハンドリングして400を返す。


</details>



<details>
<summary>8. エラーメッセージ方針</summary>

## 8.1 基本方針

- 評価者が原因を理解しやすいメッセージにする
- 過度に詳細な内部情報は返さない
- DBエラーをそのまま返さない
- READMEの業務ルールと整合する表現にする

## 8.2 メッセージ例

| ケース | メッセージ例 |
|---|---|
| 著者名が空 | `Author name must not be blank` |
| 生年月日が未来日 | `Author birth date must be today or past date` |
| 著者が存在しない | `Author not found: id=1` |
| 書籍名が空 | `Book title must not be blank` |
| 価格がマイナス | `Book price must be greater than or equal to 0` |
| 著者なし | `Book must have at least one author` |
| authorIds重複 | `Author ids must not contain duplicates` |
| 著者ID不正 | `Specified author does not exist` |
| 書籍が存在しない | `Book not found: id=1` |
| 出版状態遷移不正 | `Published book cannot be changed to unpublished` |


</details>



<details>
<summary>9. 実装優先度</summary>

## 最優先

- `BusinessRuleViolationException`
- `NotFoundException`
- `ErrorResponse`
- `ApiExceptionHandler`
- 400 / 404 の制御

## 余力があれば対応

- `HttpMessageNotReadableException` の個別ハンドリング
- validation annotationの導入
- エラー項目名の詳細化
- エラーコードの追加

## 今回は実装しない

- 複雑なエラーコード体系
- 多言語対応
- フィールド単位の詳細エラー配列
- ログ出力設計の作り込み


</details>



<details>
<summary>10. 補足</summary>

今回のコーディングテストでは、過度に複雑なエラー設計よりも、以下を優先する。

- API全体でエラーレスポンス形式が揃っていること
- 業務ルール違反が分かりやすく返ること
- 存在しないリソースが404で返ること
- 想定外エラーで内部情報を露出しないこと


</details>
