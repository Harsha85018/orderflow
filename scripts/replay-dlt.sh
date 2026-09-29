#!/usr/bin/env bash
# Usage: scripts/replay-dlt.sh <topic>        e.g. scripts/replay-dlt.sh inventory-commands
# Copies every message in <topic>'s dead-letter topic back onto <topic>.
# Safe to run more than once: every handler is idempotent, so replayed
# duplicates are ignored. (A production tool would track what was already
# replayed with a consumer group.)
cd "$(dirname "$0")/.." || exit 1
TOPIC=$1
KAFKA="docker compose exec -T kafka /opt/kafka/bin"

DLT=$($KAFKA/kafka-topics.sh --bootstrap-server localhost:9092 --list < /dev/null \
  | grep -E "^${TOPIC}[.-](DLT|dlt)$" | head -1)
if [ -z "$DLT" ]; then
  echo "no dead-letter topic found for $TOPIC"
  exit 1
fi

echo "replaying $DLT -> $TOPIC"
$KAFKA/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic "$DLT" \
    --from-beginning --timeout-ms 5000 --property print.key=true --property key.separator='|' \
    < /dev/null 2>/dev/null \
  | tee /tmp/dlt-replay.txt \
  | $KAFKA/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic "$TOPIC" \
    --property parse.key=true --property key.separator='|'
echo "replayed $(wc -l < /tmp/dlt-replay.txt | tr -d ' ') messages"
