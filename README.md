# Order Service

Spring Boot order service for OrderNest.

## Features
- Create order (`POST /api/orders`)
- Get order by order id (`GET /api/orders/{orderId}`)
- Get orders by user id (`GET /api/orders/user/{userId}`)
- Validates inventory availability through inventory service before creating order

## Configuration
`src/main/resources/application.yml`

Environment variables:
- `DB_URL` (optional, has default)
- `DB_USERNAME`
- `DB_PASSWORD`
- `INVENTORY_API_BASE_URL` (optional, default `https://ordernest-inventory-service.onrender.com`)

## Create Order
`POST /api/orders`

Request:
```json
{
  "item": {
    "productId": "d641ef4b-d996-4580-8642-9666349e5f6d",
    "quantity": 4
  }
}
```

Response (`201`):
```json
{
  "orderId": "74f75b15-9d9f-4a68-a0d8-8f1f0dacc939"
}
```

## Get Order By Id
`GET /api/orders/{orderId}`

## Get Orders By User Id
`GET /api/orders/user/{userId}`
