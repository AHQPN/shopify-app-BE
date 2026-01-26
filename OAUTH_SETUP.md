# Shopify OAuth Setup Guide

## Bước 1: Expose localhost ra internet (Chọn 1 trong 2)

### Option A: Cloudflare Tunnel (Khuyên dùng - Free, stable)

1. **Download cloudflared:**
```powershell
# Tải về: https://github.com/cloudflare/cloudflared/releases
# Hoặc dùng winget:
winget install --id Cloudflare.cloudflared
```

2. **Chạy tunnel:**
```powershell
cloudflared tunnel --url http://localhost:8080
```

3. **Copy URL xuất hiện:** Ví dụ: `https://abc-def-123.trycloudflare.com`

### Option B: Ngrok (Cần tài khoản)

1. **Download ngrok:** https://ngrok.com/download

2. **Đăng ký tài khoản:** https://dashboard.ngrok.com/signup

3. **Lấy authtoken và config:**
```powershell
ngrok config add-authtoken YOUR_TOKEN
```

4. **Chạy ngrok:**
```powershell
ngrok http 8080
```

5. **Copy Forwarding URL:** Ví dụ: `https://abc123.ngrok.io`

---

## Bước 2: Config App trên Shopify

### Nếu dùng Custom App (Dev/Testing):

1. **Vào Shopify Admin:**
```
https://quickstart-f5f1b2e5.myshopify.com/admin/settings/apps
```

2. **Click "Develop apps" → "Create an app"**
   - App name: `Discount Calculator`

3. **Tab "Configuration":**
   - **App URL:** `https://YOUR_TUNNEL_URL` (từ bước 1)
   - **Allowed redirection URL(s):**
     ```
     https://YOUR_TUNNEL_URL/api/auth/callback
     ```

4. **Configure Admin API scopes:**
   - ✅ `read_products`
   - ✅ `write_products`
   - Click **Save**

5. **Tab "API credentials":**
   - Copy **API key** và **API secret key**

6. **Update application.properties:**
```properties
shopify.api.key=YOUR_API_KEY
shopify.api.secret=YOUR_API_SECRET
```

7. **Click "Install app"** → Chọn shop → Install

---

### Nếu dùng Public App (Production):

1. **Vào Shopify Partner Dashboard:**
```
https://partners.shopify.com/
```

2. **Apps → Create app → Create app manually**

3. **App setup:**
   - App name: `Discount Calculator`
   - App URL: `https://YOUR_TUNNEL_URL`
   - Allowed redirection URL(s):
     ```
     https://YOUR_TUNNEL_URL/api/auth/callback
     ```

4. **API access → Configure:**
   - Admin API access scopes:
     - ✅ `read_products`
     - ✅ `write_products`

5. **Save và copy API credentials**

6. **Test app:**
   - Install app vào dev store
   - Click vào app trong Admin

---

## Bước 3: Update Backend Config

**File:** `d:\Spring\custom-shopify\src\main\resources\application.properties`

```properties
# Replace with your values
shopify.api.key=YOUR_API_KEY_HERE
shopify.api.secret=YOUR_API_SECRET_HERE
shopify.api.version=2026-01
shopify.app.url=https://YOUR_TUNNEL_URL
```

---

## Bước 4: Restart Backend

```powershell
cd d:\Spring\custom-shopify

# Stop backend hiện tại (Ctrl+C)

# Chạy lại
mvn spring-boot:run
```

---

## Bước 5: Test OAuth Flow

### Test thủ công:

1. **Mở browser:**
```
https://YOUR_TUNNEL_URL/api/auth?shop=quickstart-f5f1b2e5.myshopify.com
```

2. **Sẽ redirect đến Shopify authorize page**

3. **Click "Install"**

4. **Shopify callback về `/api/auth/callback`**

5. **Check logs backend:**
```
============ OAUTH STEP 2: CALLBACK ============
✅ HMAC validation PASSED
✅ Successfully obtained access token!
✅ Session saved to database!
============ STEP 2 COMPLETED ============
```

6. **Verify token đã lưu:**
```
https://YOUR_TUNNEL_URL/api/test/sessions
```

### Test từ Shopify Admin:

1. **Install app (nếu chưa):**
   - Settings → Apps → Your app → Install

2. **Click vào app trong Admin**

3. **App sẽ load trong iframe**

---

## Bước 6: Test Discount Feature

1. **Truy cập:**
```
http://localhost:3000/discount-feature
```

2. **Toggle "Bật"**

3. **Check logs backend:**
```
Calculating discounts for shop: quickstart-f5f1b2e5.myshopify.com
Calling Shopify API: https://quickstart-f5f1b2e5.myshopify.com/admin/api/2026-01/graphql.json
Discount calculation complete: X updated, Y skipped
```

---

## Troubleshooting

### Lỗi: "Invalid HMAC"
- Check `shopify.api.secret` đúng chưa
- Đảm bảo không có space ở đầu/cuối

### Lỗi: "No session found"
- Token chưa được lưu vào DB
- Chạy lại OAuth flow
- Check H2 console: `SELECT * FROM SHOPIFY_SESSIONS`

### Lỗi: "Tunnel disconnected"
- Cloudflare tunnel tự reconnect
- Ngrok free có timeout 2h, cần restart

### Frontend không gọi được backend
- Check `vite.config.ts` proxy config
- Đảm bảo backend đang chạy port 8080

---

## Quick Commands

```powershell
# Start tunnel (Cloudflare)
cloudflared tunnel --url http://localhost:8080

# Start backend
cd d:\Spring\custom-shopify
mvn spring-boot:run

# Start frontend (terminal khác)
cd d:\theme\my-app
npm run dev

# Check sessions
# Browser: http://localhost:8080/api/test/sessions
# H2 Console: http://localhost:8080/h2-console
```

---

## URLs Checklist

- [ ] Tunnel URL: `https://_____.trycloudflare.com`
- [ ] Backend: `http://localhost:8080`
- [ ] Frontend: `http://localhost:3000`
- [ ] H2 Console: `http://localhost:8080/h2-console`
- [ ] Shopify Admin: `https://quickstart-f5f1b2e5.myshopify.com/admin`
- [ ] App URL configured: `https://YOUR_TUNNEL_URL`
- [ ] Redirect URL: `https://YOUR_TUNNEL_URL/api/auth/callback`
