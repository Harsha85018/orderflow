#!/usr/bin/env bash
# Usage: scripts/k8s-load.sh [orders] [parallel]
# Generates load from inside the cluster (no port-forward), then summarizes from Prometheus.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
N=${1:-600}
P=${2:-5}

(cd "$ROOT" && docker compose exec -T inventory-db psql -U inventory -qc \
  "UPDATE products SET available_quantity = 100000 WHERE id = 'p-42';" < /dev/null)

kubectl create configmap loadgen-script --from-file=worker.sh="$ROOT/scripts/loadgen-worker.sh" \
  --dry-run=client -o yaml | kubectl apply -f - >/dev/null
kubectl delete job loadgen --ignore-not-found >/dev/null

START=$(date +%s)
kubectl apply -f - >/dev/null <<EOF
apiVersion: batch/v1
kind: Job
metadata:
  name: loadgen
spec:
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: loadgen
          image: curlimages/curl:8.11.1
          command: ["sh", "/scripts/worker.sh", "$N", "$P"]
          volumeMounts:
            - name: script
              mountPath: /scripts
      volumes:
        - name: script
          configMap:
            name: loadgen-script
EOF

echo "sending $N orders from inside the cluster, $P at a time..."
kubectl wait --for=condition=complete job/loadgen --timeout=600s >/dev/null \
  || echo "load job did not complete - check: kubectl logs job/loadgen"
kubectl logs job/loadgen

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
