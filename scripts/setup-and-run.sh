#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

APP_PID=""

cleanup() {
	if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
		kill "$APP_PID"
	fi
}

trap cleanup EXIT INT TERM

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

echo "Waiting for PostgreSQL..."
until docker exec book-management-postgres pg_isready -U book_user -d book_management >/dev/null 2>&1; do
	sleep 1
done

echo "Building and testing application..."
./gradlew clean build

echo "Starting application..."
./gradlew bootRun &
APP_PID="$!"

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

wait "$APP_PID"
