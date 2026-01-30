#!/bin/bash

API_URL="http://localhost:8080/api/v1"
API_KEY="nmi-admin-2024-11-18-default-key-change-in-production"

echo "=== Testing NI-Metro API ==="
echo ""

echo "1. Testing public endpoints..."
echo -n "  GET /maps: "
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_URL/maps")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)
if [ "$HTTP_CODE" = "200" ]; then
  COUNT=$(echo "$BODY" | jq '. | length' 2>/dev/null || echo "0")
  echo "✓ OK (${COUNT} maps)"
else
  echo "✗ FAILED (HTTP $HTTP_CODE)"
fi

echo -n "  GET /notifications: "
RESPONSE=$(curl -s -w "\n%{http_code}" "$API_URL/notifications")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ]; then
  echo "✓ OK"
else
  echo "✗ FAILED (HTTP $HTTP_CODE)"
fi

echo ""
echo "2. Testing protected endpoints..."
echo -n "  POST /maps (without API key): "
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/maps" \
  -H "Content-Type: application/json" \
  -d '{"name": "Test"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
  echo "✓ OK (correctly rejected)"
else
  echo "✗ FAILED (HTTP $HTTP_CODE, should be 401/403)"
fi

echo -n "  POST /maps (with API key): "
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/maps" \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Auto Test Map",
    "fileName": "auto_test_'$(date +%s)'",
    "data": {
      "info": {"name": "Auto Test"},
      "metro_map": {"lines": []}
    }
  }')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)
if [ "$HTTP_CODE" = "201" ]; then
  MAP_ID=$(echo "$BODY" | jq -r '.id' 2>/dev/null)
  echo "✓ OK (created map: $MAP_ID)"
  
  echo -n "  GET /maps/$MAP_ID: "
  GET_RESPONSE=$(curl -s -w "\n%{http_code}" "$API_URL/maps/$MAP_ID")
  GET_HTTP_CODE=$(echo "$GET_RESPONSE" | tail -n1)
  if [ "$GET_HTTP_CODE" = "200" ]; then
    echo "✓ OK"
  else
    echo "✗ FAILED (HTTP $GET_HTTP_CODE)"
  fi
  
  echo -n "  DELETE /maps/$MAP_ID: "
  DELETE_RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$API_URL/maps/$MAP_ID" \
    -H "X-API-Key: $API_KEY")
  DELETE_HTTP_CODE=$(echo "$DELETE_RESPONSE" | tail -n1)
  if [ "$DELETE_HTTP_CODE" = "204" ]; then
    echo "✓ OK"
  else
    echo "✗ FAILED (HTTP $DELETE_HTTP_CODE)"
  fi
else
  echo "✗ FAILED (HTTP $HTTP_CODE)"
  echo "$BODY"
fi

echo ""
echo "3. Testing notifications..."
echo -n "  POST /notifications: "
NOTIF_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/notifications" \
  -H "X-API-Key: $API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test_notif_'$(date +%s)'",
    "type": "normal",
    "triggerType": "once",
    "contentText": "Test notification"
  }')
NOTIF_HTTP_CODE=$(echo "$NOTIF_RESPONSE" | tail -n1)
if [ "$NOTIF_HTTP_CODE" = "201" ]; then
  echo "✓ OK"
else
  echo "✗ FAILED (HTTP $NOTIF_HTTP_CODE)"
fi

echo ""
echo "=== Test completed ==="

