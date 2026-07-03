# Signal Detect Server

This is a small Node.js authentication server for the Android app in `signal_dectect`.

It implements the app's current login contract:

```http
POST /api/auth/login
Content-Type: application/json
```

Request:

```json
{
  "username": "admin",
  "password": "123456"
}
```

Success response:

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "...",
    "userId": "10001",
    "nickname": "管理员",
    "validUntil": "2099-12-31"
  }
}
```

## Run

```powershell
cd E:\project\ida_analysis\ctf_reverse\signal_detect_server
Copy-Item .env.example .env
node src/server.js
```

Open the admin web console:

```text
http://127.0.0.1:1234/admin
```

Default admin console account:

- username: `admin`
- password: `admin123`

For production, create `signal_detect_server/.env` or set environment variables to override the default:

```env
ADMIN_USERNAME=your-admin-name
ADMIN_PASSWORD=use-a-long-random-password
ADMIN_SESSION_TTL_SECONDS=28800
```

If `ADMIN_USERNAME` or `ADMIN_PASSWORD` is explicitly empty, `/admin` will show a configuration error and all management actions remain disabled.

Default account:

- username: `admin`
- password: `123456`

The admin console manages the same users stored in `data/users.json`. Accounts created in the web console can immediately log in to the Android app through `/api/auth/login`.

Each user has a `validUntil` authorization date. Expired accounts are rejected by `/api/auth/login` with:

```json
{
  "code": 403,
  "message": "账号授权已过期，请联系管理员",
  "data": null
}
```

The app can refresh the latest authorization state after login:

```http
GET /api/auth/me
Authorization: Bearer <token>
```

This endpoint always reads the current user record from `data/users.json`, so changes made in the admin console are visible to the app on the next refresh.

## Android Client

Change the app Retrofit base URL from:

```java
.baseUrl("https://api.yourdomain.com/")
```

to your real server URL. For local emulator testing against this machine:

```java
.baseUrl("http://10.0.2.2:1234/")
```

For a physical Android device, use the LAN IP of this machine, for example:

```java
.baseUrl("http://192.168.1.10:1234/")
```

If you use HTTP instead of HTTPS on Android 9+, configure cleartext traffic in the app or use HTTPS in production.

Also disable or remove the checked-by-default test mode checkbox in `activity_login.xml`; otherwise the app will keep using local mock login.

## Production Notes

- Replace `TOKEN_SECRET` in `.env` with a long random value.
- Replace `ADMIN_USERNAME` and `ADMIN_PASSWORD` in `.env`.
- Put this service behind HTTPS, usually with Nginx or another reverse proxy.
- Store users in a real database before production use.
- Keep passwords as hashes. The sample user uses PBKDF2-SHA256.
