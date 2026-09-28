#!/usr/bin/env bash
# Usage: scripts/load.sh [orders] [parallel]
# Sends a mix of orders: 70% normal, 15% declined payment, 15% out of stock.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
N=${1:-1000}
P=${2:-5}

(cd "$ROOT" && docker compose exec -T inventory-db psql -U inventory -qc \
  "UPDATE products SET available_quantity = 100000 WHERE id = 'p-42';")

echo "sending $N orders, $P at a time..."
# Each number from seq arrives as $1 inside the command, so xargs never has
# to rewrite the command text (macOS xargs limits that to 255 bytes).
seq 1 "$N" | xargs -P "$P" -n 1 bash -c '
  n=$1
  r=$((RANDOM % 100))
  if [ $r -lt 70 ]; then
    body="{\"customerId\":\"load-$n\",\"productId\":\"p-42\",\"quantity\":1,\"amountCents\":2500}"
  elif [ $r -lt 85 ]; then
    body="{\"customerId\":\"load-$n\",\"productId\":\"p-42\",\"quantity\":1,\"amountCents\":99900}"
  else
    body="{\"customerId\":\"load-$n\",\"productId\":\"p-7\",\"quantity\":999,\"amountCents\":2500}"
  fi
  curl -s -o /dev/null -w "%{http_code}\n" -X POST localhost:8081/orders \
    -H "Content-Type: application/json" -d "$body"
  sleep 0.2
' _ | sort | uniq -c
