#!/usr/bin/env bash
# Runs the OTP-DROP-001 demo flow (docs/18-demo-interview-guide.md "Demo akışı").
# Prereqs: `docker compose up --build` already running, `jq` installed.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

step() { printf '\n=== %s ===\n' "$1"; }

step "1. Health check"
curl -sf "$BASE_URL/actuator/health"; echo

step "2. Start investigation: $(cat <<'Q'
Son 15 dakikada OTP teslimat oranı neden düştü?
Q
)"
RESPONSE=$(curl -sf -X POST "$BASE_URL/api/v1/investigations" \
  -H 'Content-Type: application/json' \
  -d '{
        "question": "Son 15 dakikada OTP teslimat oranı neden düştü?",
        "timeWindow": {"startAt": "2026-07-30T11:15:00Z", "endAt": "2026-07-30T11:30:00Z"},
        "locale": "tr-TR"
      }')
echo "$RESPONSE" | jq .
INV_ID=$(echo "$RESPONSE" | jq -r '.investigationId')

step "3. Fetch persisted result (GET, id=$INV_ID)"
curl -sf "$BASE_URL/api/v1/investigations/$INV_ID" | jq .

step "4. Preview incident draft (no persistence yet)"
curl -sf -X POST "$BASE_URL/api/v1/investigations/$INV_ID/incident-draft/preview" | jq .

step "5. Approve (creates the incident)"
IDEMPOTENCY_KEY=$(uuidgen 2>/dev/null || python3 -c 'import uuid;print(uuid.uuid4())')
curl -sf -X POST "$BASE_URL/api/v1/investigations/$INV_ID/incident-draft/decisions" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{"decision": "APPROVE", "reason": "Teknik ekip incelemesi için incident gerekli."}' | jq .

step "6. Replay the SAME Idempotency-Key (expect idempotentReplay=true, same incident)"
curl -sf -X POST "$BASE_URL/api/v1/investigations/$INV_ID/incident-draft/decisions" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{"decision": "APPROVE", "reason": "Teknik ekip incelemesi için incident gerekli."}' | jq .

printf '\nDemo complete.\n'
