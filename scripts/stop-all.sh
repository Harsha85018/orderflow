#!/usr/bin/env bash
# Stops all four services by port. Add "--infra" to also stop Docker containers.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
for port in 8081 8082 8083 8084; do
  pids=$(lsof -ti tcp:"$port" 2>/dev/null)
  if [ -n "$pids" ]; then
    kill $pids && echo "stopped service on $port"
  fi
done
if [ "$1" = "--infra" ]; then
  (cd "$ROOT" && docker compose down)
fi
