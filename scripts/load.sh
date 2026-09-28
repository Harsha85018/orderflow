#!/usr/bin/env bash
# Usage: scripts/load.sh [orders] [parallel]
# Sends a mix of orders: 70% normal, 15% declined payment, 15% out of stock.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
N=${1:-1000}
P=${2:-5}

(cd "$ROOT" && docker compose exec -T inventory-db psql -U inventory -qc \
  "UPDATE products SET available_quantity = 100000 WHERE id = 'p-42';" < /dev/null)

echo "sending $N orders, $P at a time..."
START=$(date +%s)
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

# Let the last sagas finish and Prometheus scrape them, then summarize the run.
sleep 10
WINDOW=$(( $(date +%s) - START + 5 ))

prom() {
  curl -s -G localhost:9090/api/v1/query --data-urlencode "query=$1" | python3 -c '
import json, sys
r = json.load(sys.stdin)["data"]["result"]
print(float(r[0]["value"][1]) if r else "nan")
'
}

done_count=$(prom "sum(increase(orders_completed_total[${WINDOW}s]))")
echo "completed: $(printf '%.0f' "$done_count") orders in the last ${WINDOW}s"
for pair in 50:0.5 95:0.95 99:0.99; do
  label=${pair%%:*}
  q=${pair#*:}
  v=$(prom "histogram_quantile($q, sum by (le) (increase(saga_duration_seconds_bucket[${WINDOW}s])))")
  printf "  saga p%s: %.0f ms\n" "$label" "$(echo "$v * 1000" | bc -l)"
done
