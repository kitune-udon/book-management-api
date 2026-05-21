# 業務ルール設計書

<details>
<summary>1. 目的</summary>

本ドキュメントは、書籍管理システムのバックエンドAPIで実装する業務ルールを定義する。

業務ルールは主にService層で実装し、DB制約と組み合わせてデータ不整合を防ぐ。

また、JSON形式不正・日付形式不正・enum不正・型不一致などのリクエスト形式不正は業務ルール違反とは区別し、共通例外ハンドラーで `400 Bad Request` として扱う。

</details>

<details>
<summary>2. 業務ルール一覧</summary>

| No | 区分 | ルール | エラー時ステータス | 実装場所 |
|---:|---|---|---|---|
| 1 | 著者 | 著者名は必須 | 400 | Bean Validation / Jackson |
| 2 | 著者 | 著者名は空文字不可 | 400 | Bean Validation |
| 3 | 著者 | 生年月日は必須 | 400 | Jackson |
| 4 | 著者 | 生年月日は現在日以前 | 400 | Service |
| 5 | 著者 | 存在しない著者IDは更新不可 | 404 | Service |
| 6 | 書籍 | 書籍名は必須 | 400 | Bean Validation / Jackson |
| 7 | 書籍 | 書籍名は空文字不可 | 400 | Bean Validation |
| 8 | 書籍 | 価格は必須 | 400 | Jackson |
| 9 | 書籍 | 価格は0以上 | 400 | Bean Validation / DB CHECK |
| 10 | 書籍 | 出版状態は必須 | 400 | Jackson |
| 11 | 書籍 | 出版状態は定義値のみ | 400 | enum / DB CHECK / HttpMessageNotReadableException |
| 12 | 書籍 | 書籍には1人以上の著者が必要 | 400 | Bean Validation / Service |
| 13 | 書籍 | 存在しない著者IDは指定不可 | 400 | Service / FK |
| 14 | 書籍 | authorIdsの重複は不可 | 400 | Service |
| 15 | 書籍 | 存在しない書籍IDは更新不可 | 404 | Service |
| 16 | 出版状態 | 出版済みから未出版へ戻せない | 400 | Service |

</details>

<details>
<summary>3. 入力形式不正・DTO基本検証・業務ルール検証の役割分担</summary>

## 3.1 基本方針

APIで発生するエラーは、以下のように分類する。

| 分類 | 内容 | 代表例 | 実装場所 | ステータス |
|---|---|---|---|---|
| リクエスト形式不正 | JSONや型の変換に失敗するもの | JSON構文不正、日付形式不正、enum不正、priceの型不一致 | 共通例外ハンドラー | 400 |
| パスパラメータ不正 | パスIDの型が不正なもの | `/books/abc` | 共通例外ハンドラー | 400 |
| DTO基本検証 | DTOとして受け取れたが基本制約に違反するもの | nameが空、priceがマイナス、authorIdsが空 | Bean Validation / Service | 400 |
| 業務ルール違反 | DB状態や複数項目に依存するもの | 存在しない著者ID、authorIds重複、出版状態遷移不正 | Service | 400 |
| 対象データなし | 更新・取得対象が存在しないもの | 存在しないbookId、authorId | Service | 404 |

## 3.2 `HttpMessageNotReadableException` の扱い

以下はSpring/Jacksonのリクエストボディ変換時に発生するため、業務ルールとしてService層で扱うのではなく、共通例外ハンドラーで `400 Bad Request` に変換する。

- JSON構文不正
- `birthDate` の日付形式不正
- `publicationStatus` のenum不正
- `price` などの型不一致
- リクエストボディ未指定

これらが `500 Internal Server Error` になると、APIとしての入力エラー制御が不十分に見えるため、必須対応とする。

## 3.3 DTO基本検証の方針

Kotlinの非null型だけでは、空文字・空配列などの値として受け取れる不正を表現しきれないため、Request DTOには必要に応じてBean Validationを利用する方針とする。
一方、`birthDate` や `publicationStatus` の未指定・null・形式不正・enum不正は、Kotlin非null型へのJackson変換時に発生する `HttpMessageNotReadableException` として扱い、共通例外ハンドラーで400に変換する。

| 項目 | 検証内容 | 主な実装方法 |
|---|---|---|
| name | 必須、空文字不可 | `@NotBlank` |
| birthDate | 必須、日付形式 | Kotlin非null型 / Jackson変換 |
| title | 必須、空文字不可 | `@NotBlank` |
| price | 必須、0以上 | `@Min(0)` |
| publicationStatus | 必須、定義値のみ | Kotlin非null型 / enum変換 |
| authorIds | 必須、1件以上 | `@NotEmpty` |

Bean Validationエラーは `MethodArgumentNotValidException` として共通例外ハンドラーで `400 Bad Request` に変換する。

## 3.4 Service層で扱う検証

以下はBean Validationではなく、Service層で明示的に検証する。

| ルール | Service層で扱う理由 |
|---|---|
| 生年月日は現在日以前 | 現在日との比較が必要なため |
| 存在しない著者IDは指定不可 | DB状態の確認が必要なため |
| authorIdsの重複不可 | リスト全体の整合性確認が必要なため |
| 出版済みから未出版へ戻せない | 更新前状態との比較が必要なため |
| 存在しない更新対象は404 | DB状態の確認が必要なため |

## 3.5 パスパラメータ型不一致の扱い

`authorId` や `bookId` に数値以外が指定された場合は、共通例外ハンドラーで `400 Bad Request` に変換する。

例：

```text
PUT /authors/abc
PUT /books/abc
GET /authors/abc/books
```

</details>

<details>
<summary>4. 著者に関するルール</summary>

## 4.1 著者名必須

著者登録・更新時、著者名は必須とする。

```json
{
  "name": "",
  "birthDate": "1867-02-09"
}
```

- `name.isBlank()` の場合はエラーにする
- 空文字・空白のみを不正とする
- `name` が未指定またはnullの場合も400とする

## 4.2 生年月日は現在日以前

著者の生年月日は現在日以前である必要がある。

```json
{
  "name": "未来太郎",
  "birthDate": "2999-01-01"
}
```

- `birthDate.isAfter(LocalDate.now())` の場合はエラーにする
- DB側では `birth_date NOT NULL` のみ担保し、`CHECK (birth_date <= CURRENT_DATE)` は設定しない
- `birthDate` の形式不正は `HttpMessageNotReadableException` として共通例外ハンドラーで400にする

## 4.3 存在しない著者IDの更新不可

存在しない著者IDを指定して更新しようとした場合は404を返す。

- 更新前にRepositoryで著者存在確認を行う
- 見つからない場合は `NotFoundException` を投げる

</details>

<details>
<summary>5. 書籍に関するルール</summary>

## 5.1 書籍名必須

- `title.isBlank()` の場合はエラーにする
- 空文字・空白のみを不正とする
- `title` が未指定またはnullの場合も400とする

## 5.2 価格は0以上

- `price < 0` の場合はエラーにする
- DB側でも `CHECK (price >= 0)` を設定する
- `price` の型不一致は `HttpMessageNotReadableException` として共通例外ハンドラーで400にする

## 5.3 出版状態は定義値のみ

出版状態は以下の2種類のみとする。

| 状態 | 意味 |
|---|---|
| UNPUBLISHED | 未出版 |
| PUBLISHED | 出版済み |

- Kotlinのenum `PublicationStatus` として扱う
- DB側でもCHECK制約を設定する
- enumとして変換できない値は `HttpMessageNotReadableException` として共通例外ハンドラーで400にする

## 5.4 書籍には1人以上の著者が必要

- `authorIds.isEmpty()` の場合はエラーにする
- `authorIds` が未指定またはnullの場合も400とする
- 中間テーブルを含むルールのため、Service層で実装する

## 5.5 存在しない著者IDは指定不可

- Service層で指定された著者ID一覧の存在確認を行う
- 指定数と存在数が一致しない場合はエラーにする
- DB側では外部キー制約でも不整合を防ぐ

## 5.6 authorIdsの重複不可

- `authorIds.size != authorIds.toSet().size` の場合はエラーにする
- DB側では `PRIMARY KEY (book_id, author_id)` でも重複を防ぐ
- 今回は黙って重複除去せず、入力不備として扱う

## 5.7 存在しない書籍IDの更新不可

- 更新前にRepositoryで書籍存在確認を行う
- 見つからない場合は `NotFoundException` を投げる

</details>

<details>
<summary>6. 出版状態の遷移ルール</summary>

## 6.1 状態一覧

| 状態 | 意味 |
|---|---|
| UNPUBLISHED | 未出版 |
| PUBLISHED | 出版済み |

## 6.2 遷移ルール

| 変更前 | 変更後 | 可否 | 理由 |
|---|---|---|---|
| UNPUBLISHED | UNPUBLISHED | OK | 未出版のまま更新できる |
| UNPUBLISHED | PUBLISHED | OK | 出版できる |
| PUBLISHED | PUBLISHED | OK | 出版済みのまま更新できる |
| PUBLISHED | UNPUBLISHED | NG | 一度出版済みになった書籍は未出版に戻せない |

## 6.3 実装方針

- 書籍更新時に現在の出版状態をDBから取得する
- 現在状態が `PUBLISHED` かつリクエストが `UNPUBLISHED` の場合はエラーにする
- 更新前状態との比較が必要なため、Service層で実装する

</details>

<details>
<summary>7. 書籍と著者の関連ルール</summary>

## 7.1 多対多関係

- 1冊の書籍に複数著者を紐づけられる
- 1人の著者に複数書籍を紐づけられる
- 中間テーブル `book_authors` で管理する

## 7.2 登録時のルール

- 書籍登録時は `books` に登録後、`book_authors` に著者関連を登録する
- 著者が1人も指定されていない場合は登録しない
- 存在しない著者IDが含まれる場合は登録しない

## 7.3 更新時のルール

- 書籍更新時は `books` を更新する
- 著者関連は既存の `book_authors` を削除し、リクエストの `authorIds` で再登録する
- 一連の処理は同一トランザクションで行う

</details>

<details>
<summary>8. トランザクションが必要な業務処理</summary>

| 処理 | トランザクションが必要な理由 |
|---|---|
| 書籍登録 | books登録とbook_authors登録を一体で扱うため |
| 書籍更新 | books更新とbook_authors削除・再登録を一体で扱うため |

</details>

<details>
<summary>9. 実装上の補足</summary>

- 業務ルール違反は `BusinessRuleViolationException` として扱う
- 対象データが存在しない場合は `NotFoundException` として扱う
- Bean Validationエラーは `MethodArgumentNotValidException` として共通例外ハンドラーで400にする
- JSON形式不正・日付形式不正・enum不正・型不一致は `HttpMessageNotReadableException` として共通例外ハンドラーで400にする
- パスパラメータ型不一致は共通例外ハンドラーで400にする
- DB制約違反に頼り切らず、Service層で先に検証する
- DB制約は最終的なデータ不整合防止として利用する

</details>
