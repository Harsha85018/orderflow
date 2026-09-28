#!/usr/bin/env bash
# Starts infrastructure and all four services in the background.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/logs"

(cd "$ROOT" && docker compose up -d)

start() {
  local name=$1 port=$2
  if lsof -ti tcp:"$port" >/dev/null 2>&1; then
    echo "$name already running on $port"
    return
  fi
  (cd "$ROOT/services/$name" && nohup ./mvnw -q spring-boot:run > "$ROOT/logs/$name.log" 2>&1 &)
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
wait_for order-service 8081

start payment-service 8082
start inventory-service 8083
start notification-service 8084
wait_for payment-service 8082
wait_for inventory-service 8083
wait_for notification-service 8084
