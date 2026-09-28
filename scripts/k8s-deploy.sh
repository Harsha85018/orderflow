#!/usr/bin/env bash
# Builds every service image, loads it into the kind cluster, and deploys with Helm.
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLUSTER=orderflow
TAG="${TAG:-$(date +%Y%m%d%H%M%S)}"
SERVICES="order-service payment-service inventory-service notification-service"

for svc in $SERVICES; do
  echo "== building $svc:$TAG"
  (cd "$ROOT/services/$svc" && ./mvnw -q -DskipTests package)
  docker build -q -t "orderflow/$svc:$TAG" "$ROOT/services/$svc" >/dev/null
  kind load docker-image "orderflow/$svc:$TAG" --name "$CLUSTER" >/dev/null
done

helm upgrade --install orderflow "$ROOT/charts/orderflow" --set image.tag="$TAG"

for svc in $SERVICES; do
  kubectl rollout status "deployment/$svc" --timeout=240s
done
