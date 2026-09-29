#!/usr/bin/env bash
# Usage: scripts/capacity.sh [rate ...]        e.g. scripts/capacity.sh 25 50 100 200
# For each arrival rate: 60 s of load from k6, then completion and saga latency
# from Prometheus. A rate "holds" if nearly every order completes, saga p95
# stays under 1 s, no requests fail, and k6 kept up with the target rate.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RATES=${*:-25 50 100 200}
DURATION=${DURATION:-60}

(cd "$ROOT" && docker compose exec -T inventory-db psql -U inventory -qc \
  "UPDATE products SET available_quantity = 10000000 WHERE id = 'p-42';" < /dev/null)

prom() {
  curl -s -G localhost:9090/api/v1/query --data-urlencode "query=$1" | python3 -c '
import json, sys
r = json.load(sys.stdin)["data"]["result"]
print(float(r[0]["value"][1]) if r else float("nan"))
'
}

printf "%6s | %6s %7s %7s | %8s %8s %8s | %6s %8s | %s\n" \
  "rate/s" "sent" "placed" "done%" "saga p50" "saga p95" "saga p99" "errors" "http p95" "result"

for rate in $RATES; do
  START=$(date +%s)
  k6 run -q -e RATE="$rate" -e DURATION="${DURATION}s" -e OUT="/tmp/k6-$rate.json" \
    "$ROOT/scripts/capacity.js" > "/tmp/k6-$rate.log" 2>&1
  sleep 15   # let in-flight sagas finish and Prometheus scrape them
  W="$(( $(date +%s) - START + 5 ))s"

  placed=$(prom "sum(increase(orders_placed_total[$W]))")
  done_=$(prom "sum(increase(orders_completed_total[$W]))")
  p50=$(prom "histogram_quantile(0.50, sum by (le) (increase(saga_duration_seconds_bucket[$W])))")
  p95=$(prom "histogram_quantile(0.95, sum by (le) (increase(saga_duration_seconds_bucket[$W])))")
  p99=$(prom "histogram_quantile(0.99, sum by (le) (increase(saga_duration_seconds_bucket[$W])))")

  python3 - "$rate" "$placed" "$done_" "$p50" "$p95" "$p99" "/tmp/k6-$rate.json" <<'PY'
import json, sys
rate, placed, done, p50, p95, p99, path = sys.argv[1:]
placed, done, p50, p95, p99 = map(float, (placed, done, p50, p95, p99))
k6 = json.load(open(path))
done_pct = 100 * done / placed if placed > 0 else 0
kept_up = k6["dropped"] == 0
holds = done_pct >= 98 and p95 < 1.0 and k6["http_failed_rate"] == 0 and kept_up
note = "HOLDS" if holds else "falls behind"
if not kept_up:
    note += " (k6 dropped %d)" % k6["dropped"]
print("%6s | %6d %7.0f %6.1f%% | %6.0fms %6.0fms %6.0fms | %5.1f%% %6.0fms | %s" % (
    rate, k6["sent"], placed, done_pct, p50 * 1000, p95 * 1000, p99 * 1000,
    100 * k6["http_failed_rate"], k6["http_p95_ms"], note))
PY
  sleep 20   # cool down before the next step
done
