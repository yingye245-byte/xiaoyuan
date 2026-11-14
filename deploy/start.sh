#!/usr/bin/env bash
# 启动脚本：campus-secondhand 单机部署（非 systemd 方式，便于临时演示）
# 用法：./start.sh
set -euo pipefail

APP_HOME="/opt/campus-secondhand"
APP_JAR="${APP_HOME}/campus-secondhand.jar"
PID_FILE="${APP_HOME}/app.pid"
LOG_FILE="${APP_HOME}/logs/startup.log"
JAVA_BIN="${JAVA_BIN:-/usr/lib/jvm/jdk-17/bin/java}"

if [ ! -f "${APP_JAR}" ]; then
    echo "[ERROR] 未找到 ${APP_JAR}，请先执行 mvn clean package -DskipTests 并上传 jar"
    exit 1
fi

if [ ! -f "${APP_HOME}/.env" ]; then
    echo "[ERROR] 未找到 ${APP_HOME}/.env，请参考 .env.example 配置数据库等环境变量"
    exit 1
fi

if [ -f "${PID_FILE}" ] && kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; then
    echo "[WARN] 应用已在运行，PID=$(cat "${PID_FILE}")，如需重启请执行 ./restart.sh"
    exit 0
fi

mkdir -p "${APP_HOME}/logs"

# 载入环境变量（数据库密码、Redis、RabbitMQ、上传目录）
set -a
# shellcheck disable=SC1091
source "${APP_HOME}/.env"
set +a

export SPRING_PROFILES_ACTIVE=prod
export TZ=Asia/Shanghai

nohup "${JAVA_BIN}" \
    -Xms256m -Xmx512m \
    -XX:+UseG1GC \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai \
    -jar "${APP_JAR}" \
    --spring.profiles.active=prod \
    > "${LOG_FILE}" 2>&1 &

echo $! > "${PID_FILE}"
echo "[OK] 已启动，PID=$(cat "${PID_FILE}")，启动日志：${LOG_FILE}"
echo "     业务日志：${LOG_PATH:-${APP_HOME}/logs}/campus-secondhand.log"