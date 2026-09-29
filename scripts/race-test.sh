#!/usr/bin/env bash
# Usage: scripts/race-test.sh [rounds] [concurrent-requests]
# Sends identical charge requests for the same order all at once, to expose
# check-then-act races. Correct behavior: one 201 (created), the rest 200.
export URL=${URL:-localhost:8082}
ROUNDS=${1:-5}
N=${2:-20}

for round in $(seq 1 "$ROUNDS"); do
  export ORDER
  ORDER=$(uuidgen)
  result=$(seq 1 "$N" | xargs -P "$N" -n 1 sh -c '
    curl -s -o /dev/null -w "%{http_code}\n" -X POST "$URL/payments" \
      -H "Content-Type: application/json" \
      -d "{\"orderId\":\"$ORDER\",\"amountCents\":1500}"
  ' _ | sort | uniq -c | tr -s " " | tr "\n" " ")
  echo "round $round:$result"
done
