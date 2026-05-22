#!/usr/bin/env bash

# セットアップからアプリケーション起動までを一括で実行する補助スクリプト。
# DB起動、DB接続待機、ビルド・テスト、アプリ起動、ヘルスチェック確認までを行う。

set -euo pipefail

# どのディレクトリから実行しても、プロジェクトルートを基準に処理する。
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

APP_PID=""

# スクリプト終了時に、バックグラウンド起動したアプリケーションも停止する。
cleanup() {
	if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
		kill "$APP_PID"
	fi
}

trap cleanup EXIT INT TERM

# 実行に必要なコマンドが利用できることを事前に確認する。
require_command() {
	if ! command -v "$1" >/dev/null 2>&1; then
		echo "Required command is not found: $1" >&2
		exit 1
	fi
}

require_command docker
require_command curl

echo "Starting PostgreSQL..."
docker compose up -d

# jOOQコード生成やアプリ起動前に、PostgreSQLが接続可能になるまで待機する。
echo "Waiting for PostgreSQL..."
until docker exec book-management-postgres pg_isready -U book_user -d book_management >/dev/null 2>&1; do
	sleep 1
done

# `build` にはテスト実行も含まれるため、起動前に基本的な検証を済ませる。
echo "Building and testing application..."
./gradlew clean build

# bootRunはフォアグラウンドで待機するため、ヘルスチェック確認用に一度バックグラウンド起動する。
echo "Starting application..."
./gradlew bootRun &
APP_PID="$!"

# 起動直後はまだHTTP応答できないため、/health が成功するまで待機する。
echo "Waiting for application health check..."
until curl -fsS http://localhost:8080/health >/dev/null 2>&1; do
	if ! kill -0 "$APP_PID" 2>/dev/null; then
		echo "Application process stopped before becoming healthy." >&2
		wait "$APP_PID"
		exit 1
	fi
	sleep 1
done

echo "Application is running."
echo "Health: http://localhost:8080/health"
echo "Swagger UI: http://localhost:8080/swagger-ui.html"
echo "Press Ctrl+C to stop the application."

# 以降はアプリケーションプロセスに追従し、Ctrl+Cで終了できるようにする。
wait "$APP_PID"
