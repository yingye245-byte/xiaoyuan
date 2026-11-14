#!/usr/bin/env bash
# 停止脚本
set -euo pipefail

APP_HOME="/opt/campus-secondhand"
PID_FILE="${APP_HOME}/app.pid"

if [ ! -f "${PID_FILE}" ]; then
    echo "[INFO] 未找到 PID 文件，应用可能未运行"
    exit 0
fi

PID="$(cat "${PID_FILE}")"
if ! kill -0 "${PID}" 2>/dev/null; then
    echo "[INFO] 进程 ${PID} 不存在，清理 PID 文件"
    rm -f "${PID_FILE}"
    exit 0
fi

echo "[INFO] 正在停止 PID=${PID} ..."
kill "${PID}"

# 最多等待 30 秒优雅退出
for _ in $(seq 1 30); do
    if ! kill -0 "${PID}" 2>/dev/null; then
        rm -f "${PID_FILE}"
        echo "[OK] 已停止"
        exit 0
    fi
    sleep 1
done

echo "[WARN] 优雅退出超时，强制结束"
kill -9 "${PID}" 2>/dev/null || true
rm -f "${PID_FILE}"
echo "[OK] 已强制停止"