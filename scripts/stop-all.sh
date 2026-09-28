#!/usr/bin/env bash
# Stops all four services and waits until they have actually exited.
# Add "--infra" to also stop the Docker containers.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PORTS="8081 8082 8083 8084"

for port in $PORTS; do
  pids=$(lsof -ti tcp:"$port" 2>/dev/null)
  [ -n "$pids" ] && kill $pids && echo "stopping service on $port"
done

for _ in $(seq 1 30); do
  busy=""
  for port in $PORTS; do
    lsof -ti tcp:"$port" >/dev/null 2>&1 && busy="$busy $port"
  done
  [ -z "$busy" ] && echo "all services stopped" && break
  sleep 1
done
[ -n "$busy" ] && echo "still running on:$busy (try again, or kill -9 those PIDs)"

if [ "$1" = "--infra" ]; then
  (cd "$ROOT" && docker compose down)
fi
