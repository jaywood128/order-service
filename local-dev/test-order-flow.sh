#!/bin/bash

echo "=== Testing Order Flow with JWT Authentication ==="
echo ""

# Step 1: Register a new user
echo "1. Registering new user..."
REGISTER_RESPONSE=$(curl -s -X POST \
  http://localhost:8081/api/auth/register \
  -H 'Content-Type: application/json' \
  -d @sample-register.json)

echo "Registration response:"
echo "$REGISTER_RESPONSE" | jq '.'

# Extract JWT token from registration response
JWT_TOKEN=$(echo "$REGISTER_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$JWT_TOKEN" ]; then
    echo "Failed to get JWT token from registration"
    echo "User might already exist. Trying to login instead..."
    
    # Try to login instead
    echo ""
    echo "2. Logging in with existing user..."
    LOGIN_RESPONSE=$(curl -s -X POST \
      http://localhost:8081/api/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"username":"johndoe","password":"password123"}')
    
    echo "Login response:"
    echo "$LOGIN_RESPONSE" | jq '.'
    
    JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$JWT_TOKEN" ]; then
        echo "Failed to get JWT token from login"
        echo "Is the Spring Boot application running on port 8081?"
        exit 1
    fi
fi

echo ""
echo "Got JWT token: ${JWT_TOKEN:0:50}..."

# Step 2: Create an order using JWT authentication
echo ""
echo "3. Creating order with JWT authentication..."
echo "Order request payload:"
cat sample-order.json | jq '.'

ORDER_RESPONSE=$(curl -s -X POST \
  http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d @sample-order.json)

echo ""
echo "Order creation response:"
echo "$ORDER_RESPONSE" | jq '.'

ORDER_ID=$(echo "$ORDER_RESPONSE" | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ORDER_ID" ]; then
    echo "Failed to create order"
    exit 1
fi

echo ""
echo "Created order with ID: $ORDER_ID"

# Step 3: Get user's orders
echo ""
echo "4. Fetching user's orders..."
curl -s -X GET \
  http://localhost:8081/api/orders/my-orders \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.'

# Step 4: Read from Kafka topic
echo ""
echo "5. Reading Kafka topic (order.created) from beginning..."
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order.created \
  --from-beginning \
  --timeout-ms 10000

echo ""
echo "=== Test completed ==="
