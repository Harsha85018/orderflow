#!/usr/bin/env bash
# Runs the three saga outcomes end to end and prints how each order finished.

now_ms() { python3 -c 'import time; print(int(time.time() * 1000))'; }

create_order() {
  curl -s -X POST localhost:8081/orders -H "Content-Type: application/json" -d "$1" \
    | sed -E 's/.*"id":"([^"]+)".*/\1/'
}

wait_final() {
  local id=$1 body
  for _ in $(seq 1 60); do
    body=$(curl -s "localhost:8081/orders/$id")
    case "$body" in
      *'"status":"CONFIRMED"'*|*'"status":"CANCELLED"'*) echo "$body"; return 0 ;;
    esac
    sleep 0.25
  done
  echo "TIMED OUT: $(curl -s "localhost:8081/orders/$id")"
}

stock() {
  curl -s "localhost:8083/products/$1" | sed -E 's/.*"availableQuantity":([0-9]+).*/\1/'
}

run() {
  local title=$1 json=$2 product=$3
  echo
  echo "== $title =="
  local before start id result elapsed
  before=$(stock "$product")
  start=$(now_ms)
  id=$(create_order "$json")
  result=$(wait_final "$id")
  elapsed=$(( $(now_ms) - start ))
  sleep 2
  echo "$result" | sed -E 's/.*"cancelReason":([^,]+).*"status":"([A-Z_]+)".*/  status=\2  reason=\1/'
  echo "  finished in ${elapsed} ms"
  echo "  $product stock: $before -> $(stock "$product")"
}

run "1. Happy path (expect CONFIRMED, stock -1)" \
  '{"customerId":"c-happy","productId":"p-42","quantity":1,"amountCents":2500}' p-42

run "2. Out of stock (expect CANCELLED/OUT_OF_STOCK, stock unchanged)" \
  '{"customerId":"c-nostock","productId":"p-7","quantity":999,"amountCents":2500}' p-7

run "3. Payment declined (expect CANCELLED/PAYMENT_DECLINED, stock back to start)" \
  '{"customerId":"c-declined","productId":"p-42","quantity":2,"amountCents":99900}' p-42
