# API設計書

<details>
<summary>1. 目的</summary>

本ドキュメントは、書籍管理システムのバックエンドAPIにおけるAPI仕様を定義する。

対象は以下の5APIとする。

| No | メソッド | パス | 概要 |
|---:|---|---|---|
| 1 | POST | `/authors` | 著者を登録する |
| 2 | PUT | `/authors/{authorId}` | 著者を更新する |
| 3 | POST | `/books` | 書籍を登録する |
| 4 | PUT | `/books/{bookId}` | 書籍を更新する |
| 5 | GET | `/authors/{authorId}/books` | 指定著者に紐づく書籍一覧を取得する |

</details>

<details>
<summary>2. 共通仕様</summary>

## 2.1 リクエスト形式

- Content-Type は `application/json` とする
- 日付は `yyyy-MM-dd` 形式とする
- IDは数値型として扱う

## 2.2 レスポンス形式

- 正常時はJSONを返す
- 異常時は共通エラーレスポンスを返す

### 共通エラーレスポンス

```json
{
  "status": 400,
  "message": "Error message"
}
```

## 2.3 リクエスト形式不正時の扱い

リクエストボディのJSON形式不正、日付形式不正、enum不正、型不一致は、すべて `400 Bad Request` として扱う。

これは、Spring/Jacksonの変換エラーが想定外エラーとして `500 Internal Server Error` にならないよう、共通例外ハンドラーで `HttpMessageNotReadableException` を必須対応するためである。

| ケース | 例 | ステータス |
|---|---|---|
| JSON構文不正 | `{ "name": "夏目漱石", }` | 400 |
| 日付形式不正 | `"birthDate": "invalid-date"` | 400 |
| enum不正 | `"publicationStatus": "DRAFT"` | 400 |
| 型不一致 | `"price": "abc"` | 400 |
| リクエストボディ未指定 | bodyなし | 400 |

エラーレスポンス例：

```json
{
  "status": 400,
  "message": "Invalid request body"
}
```

## 2.4 パスパラメータ不正時の扱い

パスパラメータとして数値IDを受け取るAPIで、数値に変換できない値が指定された場合は `400 Bad Request` とする。

例：

```text
PUT /authors/abc
GET /authors/abc/books
PUT /books/abc
```

エラーレスポンス例：

```json
{
  "status": 400,
  "message": "Invalid path parameter"
}
```

</details>

<details>
<summary>3. 著者登録API</summary>

## 3.1 エンドポイント

```text
POST /authors
```

## 3.2 概要

著者を新規登録する。

## 3.3 リクエスト

```json
{
  "name": "夏目漱石",
  "birthDate": "1867-02-09"
}
```

## 3.4 リクエスト項目

| 項目 | 型 | 必須 | 内容 | 制約 |
|---|---|---|---|---|
| name | string | 必須 | 著者名 | 空文字不可 |
| birthDate | string | 必須 | 生年月日 | `yyyy-MM-dd` 形式、現在日以前 |

## 3.5 レスポンス

```json
{
  "id": 1,
  "name": "夏目漱石",
  "birthDate": "1867-02-09"
}
```

## 3.6 正常時ステータス

```text
201 Created
```

## 3.7 異常系

| ケース | ステータス | 内容 |
|---|---|---|
| nameが空 | 400 | 著者名が不正 |
| birthDateが未来日 | 400 | 生年月日が不正 |
| birthDateの形式不正 | 400 | 日付形式が不正 |
| リクエストボディがJSONとして不正 | 400 | リクエスト形式が不正 |
| リクエストボディ未指定 | 400 | リクエスト形式が不正 |
| 必須項目が未指定またはnull | 400 | 入力値が不正 |

</details>

<details>
<summary>4. 著者更新API</summary>

## 4.1 エンドポイント

```text
PUT /authors/{authorId}
```

## 4.2 概要

指定した著者の情報を更新する。

## 4.3 パスパラメータ

| 項目 | 型 | 内容 |
|---|---|---|
| authorId | number | 著者ID |

## 4.4 リクエスト

```json
{
  "name": "夏目漱石",
  "birthDate": "1867-02-09"
}
```

## 4.5 リクエスト項目

| 項目 | 型 | 必須 | 内容 | 制約 |
|---|---|---|---|---|
| name | string | 必須 | 著者名 | 空文字不可 |
| birthDate | string | 必須 | 生年月日 | `yyyy-MM-dd` 形式、現在日以前 |

## 4.6 レスポンス

```json
{
  "id": 1,
  "name": "夏目漱石",
  "birthDate": "1867-02-09"
}
```

## 4.7 正常時ステータス

```text
200 OK
```

## 4.8 異常系

| ケース | ステータス | 内容 |
|---|---|---|
| 指定したauthorIdが存在しない | 404 | 著者が存在しない |
| authorIdが数値でない | 400 | パスパラメータが不正 |
| nameが空 | 400 | 著者名が不正 |
| birthDateが未来日 | 400 | 生年月日が不正 |
| birthDateの形式不正 | 400 | 日付形式が不正 |
| リクエストボディがJSONとして不正 | 400 | リクエスト形式が不正 |
| リクエストボディ未指定 | 400 | リクエスト形式が不正 |
| 必須項目が未指定またはnull | 400 | 入力値が不正 |

</details>

<details>
<summary>5. 書籍登録API</summary>

## 5.1 エンドポイント

```text
POST /books
```

## 5.2 概要

書籍を新規登録する。  
書籍には1人以上の著者を紐づける必要がある。

## 5.3 リクエスト

```json
{
  "title": "吾輩は猫である",
  "price": 1200,
  "publicationStatus": "PUBLISHED",
  "authorIds": [1]
}
```

## 5.4 リクエスト項目

| 項目 | 型 | 必須 | 内容 | 制約 |
|---|---|---|---|---|
| title | string | 必須 | 書籍名 | 空文字不可 |
| price | number | 必須 | 価格 | 0以上 |
| publicationStatus | string | 必須 | 出版状態 | `UNPUBLISHED` または `PUBLISHED` |
| authorIds | array[number] | 必須 | 著者ID一覧 | 1件以上、存在する著者IDのみ、重複不可 |

## 5.5 レスポンス

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

## 5.6 正常時ステータス

```text
201 Created
```

## 5.7 異常系

| ケース | ステータス | 内容 |
|---|---|---|
| titleが空 | 400 | 書籍名が不正 |
| priceがマイナス | 400 | 価格が不正 |
| priceの型が数値でない | 400 | 型が不正 |
| publicationStatusが不正 | 400 | 出版状態が不正 |
| authorIdsが空 | 400 | 書籍には1人以上の著者が必要 |
| authorIdsが未指定またはnull | 400 | 著者ID一覧が不正 |
| authorIdsに重複がある | 400 | 著者IDが重複している |
| 存在しないauthorIdを指定 | 400 | 指定された著者が存在しない |
| リクエストボディがJSONとして不正 | 400 | リクエスト形式が不正 |
| リクエストボディ未指定 | 400 | リクエスト形式が不正 |
| 必須項目が未指定またはnull | 400 | 入力値が不正 |

</details>

<details>
<summary>6. 書籍更新API</summary>

## 6.1 エンドポイント

```text
PUT /books/{bookId}
```

## 6.2 概要

指定した書籍の情報を更新する。  
著者の紐づけも更新対象とする。

## 6.3 パスパラメータ

| 項目 | 型 | 内容 |
|---|---|---|
| bookId | number | 書籍ID |

## 6.4 リクエスト

```json
{
  "title": "吾輩は猫である 改訂版",
  "price": 1500,
  "publicationStatus": "PUBLISHED",
  "authorIds": [1]
}
```

## 6.5 リクエスト項目

| 項目 | 型 | 必須 | 内容 | 制約 |
|---|---|---|---|---|
| title | string | 必須 | 書籍名 | 空文字不可 |
| price | number | 必須 | 価格 | 0以上 |
| publicationStatus | string | 必須 | 出版状態 | `UNPUBLISHED` または `PUBLISHED` |
| authorIds | array[number] | 必須 | 著者ID一覧 | 1件以上、存在する著者IDのみ、重複不可 |

## 6.6 レスポンス

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

## 6.7 正常時ステータス

```text
200 OK
```

## 6.8 異常系

| ケース | ステータス | 内容 |
|---|---|---|
| 指定したbookIdが存在しない | 404 | 書籍が存在しない |
| bookIdが数値でない | 400 | パスパラメータが不正 |
| titleが空 | 400 | 書籍名が不正 |
| priceがマイナス | 400 | 価格が不正 |
| priceの型が数値でない | 400 | 型が不正 |
| publicationStatusが不正 | 400 | 出版状態が不正 |
| authorIdsが空 | 400 | 書籍には1人以上の著者が必要 |
| authorIdsが未指定またはnull | 400 | 著者ID一覧が不正 |
| authorIdsに重複がある | 400 | 著者IDが重複している |
| 存在しないauthorIdを指定 | 400 | 指定された著者が存在しない |
| PUBLISHEDからUNPUBLISHEDへ変更 | 400 | 出版済み書籍は未出版に戻せない |
| リクエストボディがJSONとして不正 | 400 | リクエスト形式が不正 |
| リクエストボディ未指定 | 400 | リクエスト形式が不正 |
| 必須項目が未指定またはnull | 400 | 入力値が不正 |

</details>

<details>
<summary>7. 著者別書籍取得API</summary>

## 7.1 エンドポイント

```text
GET /authors/{authorId}/books
```

## 7.2 概要

指定した著者に紐づく書籍一覧を取得する。

## 7.3 パスパラメータ

| 項目 | 型 | 内容 |
|---|---|---|
| authorId | number | 著者ID |

## 7.4 レスポンス

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

## 7.5 書籍が0件の場合

```json
[]
```

## 7.6 正常時ステータス

```text
200 OK
```

## 7.7 異常系

| ケース | ステータス | 内容 |
|---|---|---|
| 指定したauthorIdが存在しない | 404 | 著者が存在しない |
| authorIdが数値でない | 400 | パスパラメータが不正 |

</details>

<details>
<summary>8. DTO設計</summary>

## 8.1 著者DTO

```kotlin
data class CreateAuthorRequest(
    val name: String,
    val birthDate: LocalDate
)

data class UpdateAuthorRequest(
    val name: String,
    val birthDate: LocalDate
)

data class AuthorResponse(
    val id: Long,
    val name: String,
    val birthDate: LocalDate
)
```

## 8.2 書籍DTO

```kotlin
data class CreateBookRequest(
    val title: String,
    val price: Int,
    val publicationStatus: PublicationStatus,
    val authorIds: List<Long>
)

data class UpdateBookRequest(
    val title: String,
    val price: Int,
    val publicationStatus: PublicationStatus,
    val authorIds: List<Long>
)

data class BookResponse(
    val id: Long,
    val title: String,
    val price: Int,
    val publicationStatus: PublicationStatus,
    val authors: List<AuthorResponse>
)

data class BookSummaryResponse(
    val id: Long,
    val title: String,
    val price: Int,
    val publicationStatus: PublicationStatus
)
```

</details>

<details>
<summary>9. 補足</summary>

- 書籍登録・更新では、レスポンスに紐づく著者情報を含める
- 著者別書籍取得では、著者情報は省略した書籍サマリーを返す
- 入力値検証と業務ルール違反は400を返す
- JSON形式不正・日付形式不正・enum不正・型不一致は `HttpMessageNotReadableException` として共通例外ハンドラーで400を返す
- パスパラメータの型不一致は共通例外ハンドラーで400を返す
- 存在しない更新対象は404を返す

</details>
