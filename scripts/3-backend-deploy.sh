#!/usr/bin/env bash
set -euo pipefail

GROUP="rg-vetsync"
APP_NAME="app-vetsync-rm563197"
JAVA_REPOSITORY="https://github.com/Challenge-2TDSPG-2026/vetSync-java.git"
JAVA_BRANCH="${JAVA_BRANCH:-main}"
TEMP_DIR=$(mktemp -d)
trap 'rm -rf -- "$TEMP_DIR"' EXIT

git clone --depth 1 --branch "$JAVA_BRANCH" "$JAVA_REPOSITORY" "$TEMP_DIR/vetsync-java"
cd "$TEMP_DIR/vetsync-java"

if [[ ! -x ./mvnw ]]; then
    chmod +x ./mvnw
fi

echo "Running backend build and tests from commit $(git rev-parse --short HEAD)..."
./mvnw --batch-mode clean verify

JAR_PATH=$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)
if [[ -z "$JAR_PATH" ]]; then
    echo "No executable JAR was found in target/." >&2
    exit 1
fi

echo "Deploying $JAR_PATH..."
az webapp deploy \
    --resource-group "$GROUP" \
    --name "$APP_NAME" \
    --src-path "$JAR_PATH" \
    --type jar \
    --clean true \
    --output none

HEALTH_URL="https://$APP_NAME.azurewebsites.net/actuator/health"
echo "Waiting for $HEALTH_URL ..."

for attempt in {1..18}; do
    if curl --fail --silent --show-error --max-time 20 "$HEALTH_URL"; then
        echo
        echo "Backend deployed from commit $(git rev-parse HEAD): https://$APP_NAME.azurewebsites.net"
        exit 0
    fi
    echo "Health check attempt $attempt/18 failed; retrying in 10 seconds..."
    sleep 10
done

echo "Deploy completed, but the public health check did not become healthy." >&2
echo "Inspect logs with: az webapp log tail --resource-group $GROUP --name $APP_NAME" >&2
exit 1
