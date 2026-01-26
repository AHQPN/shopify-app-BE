# Shopify App Backend - Spring Boot

## 🚀 Cấu hình đã hoàn thành

### ✅ Tính năng đã implement:

1. **OAuth Flow**
   - `/api/auth` - Khởi tạo OAuth
   - `/api/auth/callback` - Xử lý callback từ Shopify
   - `/api/auth/session` - Get session info
   - HMAC validation

2. **Session Management**
   - JPA Entity: ShopifySession
   - Repository với H2 database
   - Session storage và retrieval

3. **Webhook Handlers**
   - `/webhooks/app/uninstalled` - Xử lý khi app bị gỡ
   - `/webhooks/app/scopes_update` - Xử lý khi scopes thay đổi
   - Webhook signature verification

4. **Security & CORS**
   - CORS configuration cho frontend
   - Security filter chain
   - Public endpoints cho auth và webhooks

## 🏃 Cách chạy

### 1. Cấu hình môi trường

Tạo file `.env` hoặc set environment variables:
```bash
SHOPIFY_API_KEY=your-api-key
SHOPIFY_API_SECRET=your-api-secret
SHOPIFY_APP_URL=http://localhost:8080
```

### 2. Chạy ứng dụng

```bash
# Build project
./mvnw clean install

# Run Spring Boot
./mvnw spring-boot:run
```

Hoặc trong IDE: Run `CustomShopifyApplication.java`

## 📡 API Endpoints

### Health Check
```
GET http://localhost:8080/api/health
```

### OAuth Flow
```
GET http://localhost:8080/api/auth?shop=yourshop.myshopify.com
```

### Get Session
```
GET http://localhost:8080/api/auth/session?shop=yourshop.myshopify.com
```

### Webhooks
```
POST http://localhost:8080/webhooks/app/uninstalled
POST http://localhost:8080/webhooks/app/scopes_update
```

## 🗄️ Database

**Development:** H2 in-memory database
- Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:shopifydb`
- Username: `sa`
- Password: (empty)

**Production:** Chuyển sang PostgreSQL trong `application.properties`

## 🔗 Kết nối với Frontend

Frontend React ở `http://localhost:3000` đã được cấu hình CORS và có thể gọi API này.

Update `.env` trong React app:
```
VITE_SPRING_API_URL=http://localhost:8080
```

## 📝 TODO - Nâng cao

- [ ] Implement real Shopify API calls (hiện tại dùng mock)
- [ ] Add JWT token authentication
- [ ] Implement GraphQL client cho Shopify Admin API
- [ ] Add caching layer (Redis)
- [ ] Add proper error handling
- [ ] Add API documentation (Swagger/OpenAPI)
- [ ] Add unit & integration tests
- [ ] Production database configuration
