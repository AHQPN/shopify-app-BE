# Token Validation & Auto-Refresh Flow

## Cách hoạt động:

### Mỗi request đến backend:

```
1. Frontend gọi: /api/settings?shop=xxx

2. Interceptor check:
   ├─ ✅ Shop có trong request?
   ├─ ✅ Token có trong DB?
   └─ ✅ Token còn valid? (gọi Shopify API)
       │
       ├─ ✅ Valid → Tiếp tục xử lý request
       │
       └─ ❌ Invalid/Expired/Revoked
           ├─ 🗑️ Xóa token cũ khỏi DB
           ├─ 🔄 Redirect OAuth để lấy token mới
           ├─ 📝 Lưu token mới vào DB
           └─ ✅ Request tiếp tục

3. Controller xử lý logic business
```

---

## Khi nào token bị invalid?

1. **User uninstall app** → Token revoked
2. **Admin revoke token** trong Shopify Admin
3. **Scopes thay đổi** → Phải reinstall
4. **Shop bị xóa/suspend**

---

## Flow chi tiết khi token invalid:

```
Request: GET /api/settings?shop=example.myshopify.com
  ↓
Interceptor: ✅ Token found in DB
  ↓
Validate: Call Shopify API /admin/api/2026-01/shop.json
  ↓
  ├─ 200 OK → ✅ Token valid → Continue
  │
  └─ 401/403 → ❌ Token invalid
      ↓
      Delete session from DB
      ↓
      Redirect: /api/auth?shop=example.myshopify.com
      ↓
      AuthController: Generate OAuth URL
      ↓
      Redirect: https://example.myshopify.com/admin/oauth/authorize?...
      ↓
      User clicks "Install" (hoặc tự động approve)
      ↓
      Shopify callback: /api/auth/callback?code=...
      ↓
      Exchange code → New access token
      ↓
      Save new token to DB (update if exists)
      ↓
      Redirect back to frontend
      ↓
      Frontend retry request
      ↓
      Interceptor: ✅ New token valid → Continue
```

---

## Logs khi token invalid:

```
========== REQUEST VALIDATION ==========
Path: /api/settings
Shop: example.myshopify.com
✅ Session found in DB
Validating access token...
❌ Access token is invalid or expired for shop: example.myshopify.com
🔄 Deleting old session and initiating OAuth to get new token...
✅ Old session deleted
→ Redirecting to: http://localhost:8080/api/auth?shop=example.myshopify.com

============ OAUTH STEP 1: INITIATE AUTH ============
Shop: example.myshopify.com
Generated auth URL: https://example.myshopify.com/admin/oauth/authorize?...
============ STEP 1 COMPLETED ============

[User approves]

============ OAUTH STEP 2: CALLBACK ============
Callback received for shop: example.myshopify.com
✅ HMAC validation PASSED
Exchanging code for access token...
✅ Successfully obtained access token!
Updating existing session ID: abc-123-xyz
✅ Session saved to database!
============ STEP 2 COMPLETED ============
```

---

## Performance Impact:

### Mỗi request:
- **+100-300ms** để validate token với Shopify API
- Trade-off: Chậm hơn nhưng đảm bảo token luôn hợp lệ

### Optimize:
Có thể cache validation result:
```java
// Cache: token valid trong 5 phút
if (lastValidated < 5 minutes ago) {
    return cached result;
} else {
    validate lại;
}
```

---

## Test Scenarios:

### 1. Token hợp lệ:
```bash
curl "http://localhost:8080/api/settings?shop=quickstart-f5f1b2e5.myshopify.com"
→ 200 OK
```

### 2. Token invalid (test bằng cách uninstall app):
```bash
# Uninstall app trong Shopify Admin
curl "http://localhost:8080/api/settings?shop=quickstart-f5f1b2e5.myshopify.com"
→ 302 Redirect to OAuth
→ User reinstall
→ 200 OK với token mới
```

### 3. Shop chưa install:
```bash
curl "http://localhost:8080/api/settings?shop=new-shop.myshopify.com"
→ 302 Redirect to OAuth
```

---

## Database:

**Before token invalid:**
```sql
SELECT * FROM SHOPIFY_SESSIONS;
id              | shop                              | access_token
abc-123         | example.myshopify.com             | shpat_old_token
```

**After auto-refresh:**
```sql
SELECT * FROM SHOPIFY_SESSIONS;
id              | shop                              | access_token
abc-123         | example.myshopify.com             | shpat_new_token
-- Cùng ID, token đã update
```

---

## Lưu ý:

1. **Token validation gọi Shopify API** → Có thể bị rate limit nếu traffic cao
2. **User phải approve OAuth lại** nếu token invalid
3. **Frontend cần handle redirect** khi token expired
4. **Session ID giữ nguyên** khi update token (không tạo mới)

---

## Production Recommendations:

1. **Cache validation result** (5-15 phút)
2. **Async validation** để không block request
3. **Webhook** `app/uninstalled` để xóa token ngay
4. **Monitor** token validation failures
5. **Retry logic** trong frontend khi gặp 302

---

Giờ token của bạn sẽ tự động refresh khi invalid! 🎉
