#!/usr/bin/env bash
# Usage: scripts/trace.sh <order-id>
# Prints every span in that order's trace, in time order.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ORDER_ID=$1

TRACE_ID=$(grep -h "$ORDER_ID" "$ROOT"/logs/*.log | grep -o '[0-9a-f]\{32\}' | head -1)
if [ -z "$TRACE_ID" ]; then
  echo "No trace ID found in the logs for order $ORDER_ID"
  exit 1
fi
echo "trace: $TRACE_ID"
echo "view:  http://localhost:16686/trace/$TRACE_ID"
echo

curl -s "localhost:16686/api/v3/traces/$TRACE_ID" | python3 -c '
import json, sys
text = sys.stdin.read()
try:
    data = json.loads(text)
except ValueError:
    print("Could not parse response:", text[:300])
    sys.exit(1)

root = data.get("result", data)
rows = []
for rs in root.get("resourceSpans", []):
    attrs = {a["key"]: a["value"].get("stringValue") for a in rs.get("resource", {}).get("attributes", [])}
    service = attrs.get("service.name", "?")
    for ss in rs.get("scopeSpans", []):
        for s in ss.get("spans", []):
            rows.append((int(s["startTimeUnixNano"]), int(s["endTimeUnixNano"]), service, s["name"]))

if not rows:
    print("No spans found. Start of response:", text[:300])
    sys.exit(1)

rows.sort()
t0 = rows[0][0]
print("  start(ms)  took(ms)  service                 operation")
for start, end, service, name in rows:
    print("%10.1f %9.1f  %-22s  %s" % ((start - t0) / 1e6, (end - start) / 1e6, service, name))
print()
print("%d spans across %d services" % (len(rows), len({r[2] for r in rows})))
'
