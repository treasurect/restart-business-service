
#!/bin/bash

set -Eeuo pipefail

# ==================================
# 1. 基础配置
# ==================================

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SERVER_USER="treasure"
SERVER_HOST="124.222.46.122"
REMOTE="${SERVER_USER}@${SERVER_HOST}"

REMOTE_DIR="/opt/restart-business"
REMOTE_JAR="${REMOTE_DIR}/restart-business-service.jar"
REMOTE_TEMP="/tmp/restart-business-service.jar"

SERVICE_NAME="restart-business"

cd "$PROJECT_DIR"

echo "================================"
echo "开始发布 restart-business"
echo "================================"

# ==================================
# 2. Maven 打包
# ==================================

echo "开始 Maven 打包..."

if [[ -x "$PROJECT_DIR/mvnw" ]]; then
    "$PROJECT_DIR/mvnw" clean package -DskipTests
else
    mvn clean package -DskipTests
fi

# 查找可执行 JAR
JARS=()

for file in "$PROJECT_DIR"/target/restart-business-service*.jar; do
    [[ -f "$file" ]] || continue

    case "$file" in
        *-sources.jar|-javadoc.jar|-plain.jar|*.original)
            continue
            ;;
    esac

    JARS+=("$file")
done

if [[ ${#JARS[@]} -ne 1 ]]; then
    echo "错误：没有找到唯一的 restart-business-service JAR"
    printf '%s\n' "${JARS[@]}"
    exit 1
fi

JAR_FILE="${JARS[0]}"

echo "打包成功：$JAR_FILE"

# ==================================
# 3. 检查服务器目录和服务
# ==================================

echo "检查服务器部署环境..."

ssh "$REMOTE" "
    test -d '$REMOTE_DIR' &&
    sudo systemctl cat '$SERVICE_NAME' >/dev/null
"

# ==================================
# 4. 上传 JAR 到临时目录
# ==================================

echo "正在上传 JAR..."

scp "$JAR_FILE" "$REMOTE:$REMOTE_TEMP"

# ==================================
# 5. 备份旧 JAR 并替换
# ==================================

echo "备份旧版本并替换..."

ssh "$REMOTE" "
    set -e

    if [ -f '$REMOTE_JAR' ]; then
        sudo cp '$REMOTE_JAR' \
            '${REMOTE_JAR}.backup.'\$(date +%Y%m%d%H%M%S)
    fi

    sudo mv '$REMOTE_TEMP' '$REMOTE_JAR'
    sudo chown '$SERVER_USER:$SERVER_USER' '$REMOTE_JAR'
"

echo "JAR 替换成功"

# ==================================
# 6. 重启服务
# ==================================

echo "正在重启 $SERVICE_NAME..."

ssh "$REMOTE" "
    sudo systemctl restart '$SERVICE_NAME'
"

# ==================================
# 7. 检查启动状态
# ==================================

echo "等待服务启动..."
sleep 3

if ! ssh "$REMOTE" "
    sudo systemctl is-active --quiet '$SERVICE_NAME'
"; then

    echo "================================"
    echo "$SERVICE_NAME 启动失败"
    echo "================================"

    ssh "$REMOTE" "
        sudo systemctl status '$SERVICE_NAME' --no-pager || true
        sudo journalctl -u '$SERVICE_NAME' -n 80 --no-pager
    "

    exit 1
fi

# ==================================
# 8. 发布结果
# ==================================

echo "================================"
echo "$SERVICE_NAME 发布成功"
echo "================================"

ssh "$REMOTE" "
    sudo systemctl status '$SERVICE_NAME' --no-pager

#    echo '========== 最近启动日志 =========='
#
#    sudo journalctl -u '$SERVICE_NAME' -n 30 --no-pager
"
