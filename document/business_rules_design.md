# 業務ルール設計書

<details>
<summary>1. 目的</summary>

本ドキュメントは、書籍管理システムのバックエンドAPIで実装する業務ルールを定義する。

業務ルールは主にService層で実装し、DB制約と組み合わせてデータ不整合を防ぐ。


</details>



<details>
<summary>2. 業務ルール一覧</summary>

| No | 区分 | ルール | エラー時ステータス | 実装場所 |
|---:|---|---|---|---|
| 1 | 著者 | 著者名は必須 | 400 | Service / Validation |
| 2 | 著者 | 著者名は空文字不可 | 400 | Service / Validation |
| 3 | 著者 | 生年月日は必須 | 400 | Service / Validation |
| 4 | 著者 | 生年月日は現在日以前 | 400 | Service / DB CHECK |
| 5 | 著者 | 存在しない著者IDは更新不可 | 404 | Service |
| 6 | 書籍 | 書籍名は必須 | 400 | Service / Validation |
| 7 | 書籍 | 書籍名は空文字不可 | 400 | Service / Validation |
| 8 | 書籍 | 価格は必須 | 400 | Service / Validation |
| 9 | 書籍 | 価格は0以上 | 400 | Service / DB CHECK |
| 10 | 書籍 | 出版状態は必須 | 400 | Service / Validation |
| 11 | 書籍 | 出版状態は定義値のみ | 400 | enum / DB CHECK |
| 12 | 書籍 | 書籍には1人以上の著者が必要 | 400 | Service |
| 13 | 書籍 | 存在しない著者IDは指定不可 | 400 | Service / FK |
| 14 | 書籍 | authorIdsの重複は不可 | 400 | Service |
| 15 | 書籍 | 存在しない書籍IDは更新不可 | 404 | Service |
| 16 | 出版状態 | 出版済みから未出版へ戻せない | 400 | Service |


</details>



<details>
<summary>3. 著者に関するルール</summary>

## 3.1 著者名必須

### 内容

著者登録・更新時、著者名は必須とする。

### NG例

```json
{
  "name": "",
  "birthDate": "1867-02-09"
}
```

### エラー

```text
400 Bad Request
```

### 実装方針

- `name.isBlank()` の場合はエラーにする
- 空文字・空白のみを不正とする



## 3.2 生年月日は現在日以前

### 内容

著者の生年月日は現在日以前である必要がある。

### NG例

```json
{
  "name": "未来太郎",
  "birthDate": "2999-01-01"
}
```

### エラー

```text
400 Bad Request
```

### 実装方針

- `birthDate.isAfter(LocalDate.now())` の場合はエラーにする
- DB側でも `CHECK (birth_date <= CURRENT_DATE)` を設定する



## 3.3 存在しない著者IDの更新不可

### 内容

存在しない著者IDを指定して更新しようとした場合は404を返す。

### エラー

```text
404 Not Found
```

### 実装方針

- 更新前にRepositoryで著者存在確認を行う
- 見つからない場合は `NotFoundException` を投げる


</details>



<details>
<summary>4. 書籍に関するルール</summary>

## 4.1 書籍名必須

### 内容

書籍登録・更新時、書籍名は必須とする。

### NG例

```json
{
  "title": "",
  "price": 1200,
  "publicationStatus": "PUBLISHED",
  "authorIds": [1]
}
```

### エラー

```text
400 Bad Request
```

### 実装方針

- `title.isBlank()` の場合はエラーにする
- 空文字・空白のみを不正とする



## 4.2 価格は0以上

### 内容

書籍価格は0以上である必要がある。

### NG例

```json
{
  "title": "吾輩は猫である",
  "price": -1,
  "publicationStatus": "PUBLISHED",
  "authorIds": [1]
}
```

### エラー

```text
400 Bad Request
```

### 実装方針

- `price < 0` の場合はエラーにする
- DB側でも `CHECK (price >= 0)` を設定する



## 4.3 出版状態は定義値のみ

### 内容

出版状態は以下の2種類のみとする。

| 状態 | 意味 |
|---|---|
| UNPUBLISHED | 未出版 |
| PUBLISHED | 出版済み |

### NG例

```json
{
  "title": "吾輩は猫である",
  "price": 1200,
  "publicationStatus": "DRAFT",
  "authorIds": [1]
}
```

### エラー

```text
400 Bad Request
```

### 実装方針

- Kotlinのenum `PublicationStatus` として扱う
- DB側でもCHECK制約を設定する



## 4.4 書籍には1人以上の著者が必要

### 内容

書籍登録・更新時、書籍には1人以上の著者を紐づける必要がある。

### NG例

```json
{
  "title": "吾輩は猫である",
  "price": 1200,
  "publicationStatus": "PUBLISHED",
  "authorIds": []
}
```

### エラー

```text
400 Bad Request
```

### 実装方針

- `authorIds.isEmpty()` の場合はエラーにする
- 中間テーブルを含むルールのため、Service層で実装する



## 4.5 存在しない著者IDは指定不可

### 内容

書籍登録・更新時、存在しない著者IDを指定できない。

### NG例

```json
{
  "title": "吾輩は猫である",
  "price": 1200,
  "publicationStatus": "PUBLISHED",
  "authorIds": [9999]
}
```

### エラー

```text
400 Bad Request
```

### 実装方針

- Service層で指定された著者ID一覧の存在確認を行う
- 指定数と存在数が一致しない場合はエラーにする
- DB側では外部キー制約でも不整合を防ぐ



## 4.6 authorIdsの重複不可

### 内容

書籍登録・更新時、`authorIds` に同じ著者IDを重複指定できない。

### NG例

```json
{
  "title": "吾輩は猫である",
  "price": 1200,
  "publicationStatus": "PUBLISHED",
  "authorIds": [1, 1]
}
```

### エラー

```text
400 Bad Request
```

### 実装方針

- `authorIds.size != authorIds.toSet().size` の場合はエラーにする
- DB側では `PRIMARY KEY (book_id, author_id)` でも重複を防ぐ
- 今回は黙って重複除去せず、入力不備として扱う



## 4.7 存在しない書籍IDの更新不可

### 内容

存在しない書籍IDを指定して更新しようとした場合は404を返す。

### エラー

```text
404 Not Found
```

### 実装方針

- 更新前にRepositoryで書籍存在確認を行う
- 見つからない場合は `NotFoundException` を投げる


</details>



<details>
<summary>5. 出版状態の遷移ルール</summary>

## 5.1 状態一覧

| 状態 | 意味 |
|---|---|
| UNPUBLISHED | 未出版 |
| PUBLISHED | 出版済み |

## 5.2 遷移ルール

| 変更前 | 変更後 | 可否 | 理由 |
|---|---|---|---|
| UNPUBLISHED | UNPUBLISHED | OK | 未出版のまま更新できる |
| UNPUBLISHED | PUBLISHED | OK | 出版できる |
| PUBLISHED | PUBLISHED | OK | 出版済みのまま更新できる |
| PUBLISHED | UNPUBLISHED | NG | 一度出版済みになった書籍は未出版に戻せない |

## 5.3 実装方針

- 書籍更新時に現在の出版状態をDBから取得する
- 現在状態が `PUBLISHED` かつリクエストが `UNPUBLISHED` の場合はエラーにする
- 更新前状態との比較が必要なため、Service層で実装する


</details>



<details>
<summary>6. 書籍と著者の関連ルール</summary>

## 6.1 多対多関係

- 1冊の書籍に複数著者を紐づけられる
- 1人の著者に複数書籍を紐づけられる
- 中間テーブル `book_authors` で管理する

## 6.2 登録時のルール

- 書籍登録時は `books` に登録後、`book_authors` に著者関連を登録する
- 著者が1人も指定されていない場合は登録しない
- 存在しない著者IDが含まれる場合は登録しない

## 6.3 更新時のルール

- 書籍更新時は `books` を更新する
- 著者関連は既存の `book_authors` を削除し、リクエストの `authorIds` で再登録する
- 一連の処理は同一トランザクションで行う


</details>



<details>
<summary>7. トランザクションが必要な業務処理</summary>

| 処理 | トランザクションが必要な理由 |
|---|---|
| 書籍登録 | books登録とbook_authors登録を一体で扱うため |
| 書籍更新 | books更新とbook_authors削除・再登録を一体で扱うため |


</details>



<details>
<summary>8. 実装上の補足</summary>

- 業務ルール違反は `BusinessRuleViolationException` として扱う
- 対象データが存在しない場合は `NotFoundException` として扱う
- DB制約違反に頼り切らず、Service層で先に検証する
- DB制約は最終的なデータ不整合防止として利用する


</details>
