#!/usr/bin/env bash
# 数据库初始化脚本：建库建表 + 演示数据
# 用法：
#   ./init-db.sh                      # 使用 .env 中的 DB_* 变量
#   DB_HOST=127.0.0.1 DB_PORT=3306 DB_USER=root DB_PASSWORD=xxx ./init-db.sh
set -euo pipefail

APP_HOME="/opt/campus-secondhand"
SQL_FILE="${APP_HOME}/sql/01-init.sql"

if [ -f "${APP_HOME}/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    source "${APP_HOME}/.env"
    set +a
fi

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:?请设置 DB_PASSWORD 环境变量或在 .env 中配置}"

if [ ! -f "${SQL_FILE}" ]; then
    echo "[ERROR] 未找到 ${SQL_FILE}"
    exit 1
fi

echo "[INFO] 正在初始化数据库 ${DB_HOST}:${DB_PORT} ..."
mysql --host="${DB_HOST}" --port="${DB_PORT}" --user="${DB_USER}" --password="${DB_PASSWORD}" \
      --default-character-set=utf8mb4 < "${SQL_FILE}"

echo "[OK] 初始化完成，默认账号：admin/admin123，seller01|seller02|student01~03/123456"