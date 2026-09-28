#!/usr/bin/env bash
# Builds and starts infrastructure plus all four services in the background.
# Optional: SPRING_ARGS="--some.property=value" ./scripts/start-all.sh
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/logs"
HEAP="${HEAP:--Xmx384m}"

if ! docker info >/dev/null 2>&1; then
  echo "Docker isn't running. Start Docker Desktop (open -a Docker) and try again."
  exit 1
fi
(cd "$ROOT" && docker compose up -d) || exit 1

SERVICES="order-service payment-service inventory-service notification-service"

echo "building..."
for name in $SERVICES; do
  (cd "$ROOT/services/$name" && ./mvnw -q -DskipTests package) || { echo "build failed: $name"; exit 1; }
done

start() {
  local name=$1 port=$2
  if lsof -ti tcp:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "$name already running on $port"
    return
  fi
  local jar
  jar=$(ls "$ROOT/services/$name"/target/"$name"-*.jar | head -1)
  nohup java $HEAP -jar "$jar" $SPRING_ARGS > "$ROOT/logs/$name.log" 2>&1 &
  echo "starting $name (log: logs/$name.log)"
}

wait_for() {
  local name=$1 port=$2
  for _ in $(seq 1 120); do
    if curl -sf "localhost:$port/actuator/health" >/dev/null; then
      echo "  $name UP on $port"
      return 0
    fi
    sleep 1
  done
  echo "  $name did not start - check logs/$name.log"
  return 1
}

# Order service first: it creates the Kafka topics the others use.
start order-service 8081
wait_for order-service 8081 || exit 1

start payment-service 8082
start inventory-service 8083
start notification-service 8084
wait_for payment-service 8082
wait_for inventory-service 8083
wait_for notification-service 8084
