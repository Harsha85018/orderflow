#!/bin/sh
# Runs inside the cluster. Sends N orders, P at a time, to the order-service Service.
# Mix is deterministic: (n*37) % 100 cycles through 0..99, giving exactly
# 70% normal, 15% declined payment, 15% out of stock per 100 orders.
N=$1
P=$2
seq 1 "$N" | xargs -P "$P" -n 1 sh -c '
  n=$1
  r=$(( (n * 37) % 100 ))
  if [ $r -lt 70 ]; then
    body="{\"customerId\":\"load-$n\",\"productId\":\"p-42\",\"quantity\":1,\"amountCents\":2500}"
  elif [ $r -lt 85 ]; then
    body="{\"customerId\":\"load-$n\",\"productId\":\"p-42\",\"quantity\":1,\"amountCents\":99900}"
  else
    body="{\"customerId\":\"load-$n\",\"productId\":\"p-7\",\"quantity\":999,\"amountCents\":2500}"
  fi
  curl -s -o /dev/null -w "%{http_code}\n" -X POST http://order-service/orders \
    -H "Content-Type: application/json" -d "$body"
  sleep 0.2
' _ | sort | uniq -c
