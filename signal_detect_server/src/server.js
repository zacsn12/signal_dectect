const http = require("http");
const fs = require("fs");
const crypto = require("crypto");
const { URL } = require("url");

const config = require("./config");
const { createToken, hashPassword, verifyPassword, verifyToken } = require("./auth");

const adminSessions = new Map();
const DEFAULT_MAX_MACHINE_BINDINGS = 1;
const DEFAULT_LICENSE_FEATURES = ["bluetooth", "wifi", "cellular", "lan"];

function loadUsers() {
  return JSON.parse(fs.readFileSync(config.usersFile, "utf8"))
    .map(normalizeUser);
}

function saveUsers(users) {
  fs.writeFileSync(config.usersFile, `${JSON.stringify(users, null, 2)}\n`, "utf8");
}

function loadLicenses() {
  ensureLicensesFile();
  return JSON.parse(fs.readFileSync(config.licensesFile, "utf8"))
    .map(normalizeLicense);
}

function saveLicenses(licenses) {
  fs.writeFileSync(config.licensesFile, `${JSON.stringify(licenses, null, 2)}\n`, "utf8");
}

function ensureLicensesFile() {
  if (fs.existsSync(config.licensesFile)) {
    return;
  }
  fs.mkdirSync(require("path").dirname(config.licensesFile), { recursive: true });
  fs.writeFileSync(config.licensesFile, "[]\n", "utf8");
}

function sendJson(res, statusCode, body) {
  res.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": config.corsOrigin,
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization"
  });
  res.end(JSON.stringify(body));
}

function sendHtml(res, statusCode, html, extraHeaders = {}) {
  res.writeHead(statusCode, {
    "Content-Type": "text/html; charset=utf-8",
    "Cache-Control": "no-store",
    ...extraHeaders
  });
  res.end(html);
}

function redirect(res, location, extraHeaders = {}) {
  res.writeHead(302, {
    Location: location,
    "Cache-Control": "no-store",
    ...extraHeaders
  });
  res.end();
}

function redirectWithNotice(res, notice) {
  redirect(res, `/admin/licenses?notice=${encodeURIComponent(notice)}`);
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let rawBody = "";

    req.on("data", chunk => {
      rawBody += chunk;
      if (rawBody.length > 1024 * 1024) {
        req.destroy();
        reject(new Error("Request body is too large"));
      }
    });

    req.on("end", () => {
      if (!rawBody) {
        resolve({});
        return;
      }

      try {
        resolve(JSON.parse(rawBody));
      } catch (error) {
        reject(error);
      }
    });

    req.on("error", reject);
  });
}

async function readFormBody(req) {
  const body = await readRawBody(req);
  return Object.fromEntries(new URLSearchParams(body));
}

function readRawBody(req) {
  return new Promise((resolve, reject) => {
    let rawBody = "";

    req.on("data", chunk => {
      rawBody += chunk;
      if (rawBody.length > 1024 * 1024) {
        req.destroy();
        reject(new Error("Request body is too large"));
      }
    });

    req.on("end", () => resolve(rawBody));
    req.on("error", reject);
  });
}

function appResponse(code, message, data = null) {
  return {
    code,
    message,
    data
  };
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function parseCookies(req) {
  const header = req.headers.cookie || "";
  return Object.fromEntries(
    header.split(";")
      .map(part => part.trim())
      .filter(Boolean)
      .map(part => {
        const separatorIndex = part.indexOf("=");
        if (separatorIndex === -1) {
          return [part, ""];
        }
        return [
          decodeURIComponent(part.slice(0, separatorIndex)),
          decodeURIComponent(part.slice(separatorIndex + 1))
        ];
      })
  );
}

function isAdminAuthenticated(req) {
  const sessionId = parseCookies(req).admin_session;
  if (!sessionId || !adminSessions.has(sessionId)) {
    return false;
  }
  const session = adminSessions.get(sessionId);
  if (!session || session.expiresAt <= Date.now()) {
    adminSessions.delete(sessionId);
    return false;
  }
  return true;
}

function createAdminCookie() {
  const sessionId = crypto.randomBytes(32).toString("hex");
  const ttlMs = Math.max(300, config.adminSessionTtlSeconds) * 1000;
  adminSessions.set(sessionId, {
    expiresAt: Date.now() + ttlMs
  });
  return `admin_session=${encodeURIComponent(sessionId)}; HttpOnly; SameSite=Lax; Path=/admin; Max-Age=${Math.floor(ttlMs / 1000)}`;
}

function clearAdminCookie(req) {
  const sessionId = parseCookies(req).admin_session;
  if (sessionId) {
    adminSessions.delete(sessionId);
  }
  return "admin_session=; HttpOnly; SameSite=Lax; Path=/admin; Max-Age=0";
}

function isAdminConfigured() {
  return Boolean(config.adminUsername && config.adminPassword);
}

function renderLayout(title, content) {
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escapeHtml(title)} - Signal Detect Admin</title>
  <style>
    :root {
      color-scheme: light;
      --bg: #eef2f7;
      --panel: #ffffff;
      --text: #111827;
      --muted: #64748b;
      --line: #d7dde8;
      --primary: #2563eb;
      --primary-dark: #1d4ed8;
      --danger: #dc2626;
      --danger-bg: #fef2f2;
      --ok-bg: #ecfdf5;
      --ok-text: #047857;
      --warn-bg: #fffbeb;
      --warn-text: #b45309;
      --nav: #101827;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      background: var(--bg);
      color: var(--text);
      font-family: Arial, "Microsoft YaHei", sans-serif;
      line-height: 1.5;
    }
    a { color: var(--primary); text-decoration: none; }
    .shell { max-width: 1240px; margin: 0 auto; padding: 24px; }
    .admin-header {
      background: var(--nav);
      color: #fff;
      border-radius: 0 0 14px 14px;
      margin-bottom: 22px;
    }
    .admin-header .shell {
      padding-top: 18px;
      padding-bottom: 18px;
    }
    .nav-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
    }
    .nav-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }
    .nav-actions .btn {
      background: rgba(255,255,255,0.08);
      border-color: rgba(255,255,255,0.18);
      color: #fff;
    }
    .topbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 20px;
    }
    .brand { font-size: 24px; font-weight: 700; letter-spacing: 0; }
    .subtitle { margin-top: 4px; color: #b6c2d2; font-size: 14px; }
    .panel {
      background: var(--panel);
      border: 1px solid var(--line);
      border-radius: 8px;
      padding: 20px;
      box-shadow: 0 10px 24px rgba(17, 24, 39, 0.06);
    }
    .stats {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 12px;
      margin-bottom: 16px;
    }
    .stat {
      background: var(--panel);
      border: 1px solid var(--line);
      border-radius: 8px;
      padding: 16px;
    }
    .stat-label {
      color: var(--muted);
      font-size: 12px;
      margin-bottom: 6px;
    }
    .stat-value {
      color: var(--text);
      font-size: 24px;
      font-weight: 700;
    }
    .section-title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 14px;
      margin-bottom: 14px;
    }
    .section-title h2 { margin: 0; font-size: 18px; }
    .key {
      font-family: Consolas, Monaco, monospace;
      font-size: 13px;
      font-weight: 700;
      white-space: nowrap;
    }
    .machine-list {
      max-width: 220px;
      color: var(--muted);
      font-family: Consolas, Monaco, monospace;
      font-size: 12px;
      white-space: normal;
    }
    .login {
      max-width: 420px;
      margin: 12vh auto 0;
    }
    .grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 14px;
    }
    label {
      display: block;
      color: var(--muted);
      font-size: 13px;
      margin-bottom: 6px;
    }
    input {
      width: 100%;
      min-height: 42px;
      border: 1px solid var(--line);
      border-radius: 6px;
      color: var(--text);
      font-size: 15px;
      padding: 8px 10px;
      background: #fff;
    }
    input:focus {
      outline: 2px solid rgba(37, 99, 235, 0.18);
      border-color: var(--primary);
    }
    .actions {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 10px;
      margin-top: 16px;
    }
    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 38px;
      border: 1px solid var(--line);
      border-radius: 6px;
      padding: 8px 13px;
      background: #fff;
      color: var(--text);
      font-size: 14px;
      cursor: pointer;
    }
    .btn.primary {
      border-color: var(--primary);
      background: var(--primary);
      color: #fff;
    }
    .btn.primary:hover { background: var(--primary-dark); }
    .btn.danger {
      border-color: #fecaca;
      color: var(--danger);
      background: var(--danger-bg);
    }
    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 14px;
    }
    th, td {
      padding: 12px 10px;
      border-bottom: 1px solid var(--line);
      text-align: left;
      vertical-align: middle;
    }
    th {
      color: var(--muted);
      font-size: 13px;
      font-weight: 600;
      background: #f8fafc;
    }
    .inline-form {
      display: inline;
      margin-left: 8px;
    }
    .muted { color: var(--muted); }
    .notice {
      margin-bottom: 16px;
      border-radius: 6px;
      padding: 10px 12px;
      background: var(--ok-bg);
      color: var(--ok-text);
    }
    .error {
      margin-bottom: 16px;
      border-radius: 6px;
      padding: 10px 12px;
      background: var(--danger-bg);
      color: var(--danger);
    }
    .badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 24px;
      border-radius: 999px;
      padding: 2px 9px;
      font-size: 12px;
      font-weight: 600;
    }
    .badge.ok {
      background: var(--ok-bg);
      color: var(--ok-text);
    }
    .badge.danger {
      background: var(--danger-bg);
      color: var(--danger);
    }
    .badge.warn {
      background: var(--warn-bg);
      color: var(--warn-text);
    }
    @media (max-width: 720px) {
      .shell { padding: 16px; }
      .admin-header { border-radius: 0; }
      .topbar { align-items: flex-start; flex-direction: column; }
      .nav-row { align-items: flex-start; flex-direction: column; }
      .grid { grid-template-columns: 1fr; }
      .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      table { display: block; overflow-x: auto; white-space: nowrap; }
    }
  </style>
</head>
<body>
  ${content}
</body>
</html>`;
}

function renderAdminLogin(error = "") {
  const configError = !isAdminConfigured()
    ? "管理员账号未配置。请在服务器环境变量或 .env 中设置 ADMIN_USERNAME 和 ADMIN_PASSWORD 后重启服务。"
    : "";
  return renderLayout("后台登录", `
<main class="shell">
  <section class="panel login">
    <div class="brand">Signal Detect 许可证后台</div>
    <p class="muted">登录后可以管理许可证、绑定设备和授权期限。</p>
    ${configError ? `<div class="error">${escapeHtml(configError)}</div>` : ""}
    ${error ? `<div class="error">${escapeHtml(error)}</div>` : ""}
    <form method="post" action="/admin/login">
      <div>
        <label for="username">管理员账号</label>
        <input id="username" name="username" autocomplete="username" ${isAdminConfigured() ? "required" : "disabled"}>
      </div>
      <div style="margin-top: 12px;">
        <label for="password">管理员密码</label>
        <input id="password" name="password" type="password" autocomplete="current-password" ${isAdminConfigured() ? "required" : "disabled"}>
      </div>
      <div class="actions">
        <button class="btn primary" type="submit" ${isAdminConfigured() ? "" : "disabled"}>登录</button>
      </div>
    </form>
  </section>
</main>`);
}

function publicUser(user) {
  return {
    id: user.id,
    username: user.username,
    nickname: user.nickname,
    validUntil: user.validUntil,
    maxMachineBindings: user.maxMachineBindings,
    machineBindings: user.machineBindings
  };
}

function todayDateString() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function dateStringAfterDays(days) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function isValidDateString(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(String(value || ""))) {
    return false;
  }

  const date = new Date(`${value}T00:00:00.000Z`);
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

function isUserExpired(user) {
  return isValidDateString(user.validUntil) && user.validUntil < todayDateString();
}

function authPayloadFromRequest(req) {
  const authorization = req.headers.authorization || "";
  const match = authorization.match(/^Bearer\s+(.+)$/i);
  if (!match) {
    return null;
  }
  return verifyToken(match[1], config.tokenSecret);
}

function normalizeMachineCode(value) {
  return String(value || "").trim().toUpperCase();
}

function isValidMachineCode(value) {
  return /^[A-Z0-9_-]{8,64}$/.test(normalizeMachineCode(value));
}

function parseMaxMachineBindings(value) {
  const count = Number(value);
  if (!Number.isInteger(count) || count < 1 || count > 50) {
    return DEFAULT_MAX_MACHINE_BINDINGS;
  }
  return count;
}

function normalizeMachineBindings(user) {
  const bindings = [];
  const seen = new Set();

  if (Array.isArray(user.machineBindings)) {
    for (const binding of user.machineBindings) {
      const machineCode = normalizeMachineCode(binding && binding.machineCode);
      if (!isValidMachineCode(machineCode) || seen.has(machineCode)) {
        continue;
      }
      seen.add(machineCode);
      bindings.push({
        machineCode,
        boundAt: String(binding.boundAt || "")
      });
    }
  }

  const legacyMachineCode = normalizeMachineCode(user.machineCode);
  if (isValidMachineCode(legacyMachineCode) && !seen.has(legacyMachineCode)) {
    bindings.push({
      machineCode: legacyMachineCode,
      boundAt: String(user.machineBoundAt || "")
    });
  }

  return bindings;
}

function normalizeUser(user) {
  const normalized = {
    validUntil: "2099-12-31",
    maxMachineBindings: DEFAULT_MAX_MACHINE_BINDINGS,
    machineBindings: [],
    ...user
  };
  normalized.maxMachineBindings = parseMaxMachineBindings(normalized.maxMachineBindings);
  normalized.machineBindings = normalizeMachineBindings(normalized);
  delete normalized.machineCode;
  delete normalized.machineBoundAt;
  return normalized;
}

function normalizeLicenseKey(value) {
  return String(value || "").trim().toUpperCase();
}

function generateLicenseKey() {
  const random = crypto.randomBytes(8).toString("hex").toUpperCase();
  return `SD-${random.slice(0, 4)}-${random.slice(4, 8)}-${random.slice(8, 12)}-${random.slice(12, 16)}`;
}

function isValidLicenseKey(value) {
  return /^[A-Z0-9_-]{6,64}$/.test(normalizeLicenseKey(value));
}

function normalizeFeatures(value) {
  if (Array.isArray(value)) {
    const features = value
      .map(item => String(item || "").trim().toLowerCase())
      .filter(Boolean);
    return features.length ? Array.from(new Set(features)) : DEFAULT_LICENSE_FEATURES;
  }
  if (typeof value === "string") {
    const features = value
      .split(",")
      .map(item => item.trim().toLowerCase())
      .filter(Boolean);
    return features.length ? Array.from(new Set(features)) : DEFAULT_LICENSE_FEATURES;
  }
  return DEFAULT_LICENSE_FEATURES;
}

function parseMaxActivations(value) {
  const count = Number(value);
  if (!Number.isInteger(count) || count < 1 || count > 100) {
    return DEFAULT_MAX_MACHINE_BINDINGS;
  }
  return count;
}

function normalizeLicense(license) {
  const normalized = {
    id: "",
    licenseKey: "",
    customerName: "",
    status: "ACTIVE",
    validUntil: "2099-12-31",
    maxActivations: DEFAULT_MAX_MACHINE_BINDINGS,
    features: DEFAULT_LICENSE_FEATURES,
    machineBindings: [],
    ...license
  };
  normalized.licenseKey = normalizeLicenseKey(normalized.licenseKey);
  normalized.status = String(normalized.status || "ACTIVE").toUpperCase();
  normalized.maxActivations = parseMaxActivations(normalized.maxActivations);
  normalized.features = normalizeFeatures(normalized.features);
  normalized.machineBindings = normalizeMachineBindings(normalized);
  return normalized;
}

function isLicenseExpired(license) {
  return isValidDateString(license.validUntil) && license.validUntil < todayDateString();
}

function isLicenseUsable(license) {
  return license.status === "ACTIVE" && !isLicenseExpired(license);
}

function findLicenseBinding(license, machineCode) {
  const normalizedMachineCode = normalizeMachineCode(machineCode);
  return license.machineBindings.find(binding => binding.machineCode === normalizedMachineCode);
}

function bindLicenseMachineIfAllowed(license, machineCode) {
  const normalizedMachineCode = normalizeMachineCode(machineCode);
  const existing = findLicenseBinding(license, normalizedMachineCode);
  if (existing) {
    return { ok: true, binding: existing, added: false };
  }

  if (license.machineBindings.length >= license.maxActivations) {
    return { ok: false, reason: `许可证已绑定 ${license.machineBindings.length}/${license.maxActivations} 台设备，请联系管理员解绑或提高授权数量` };
  }

  const binding = {
    machineCode: normalizedMachineCode,
    boundAt: new Date().toISOString()
  };
  license.machineBindings.push(binding);
  return { ok: true, binding, added: true };
}

function licenseBindingsText(license) {
  if (!license.machineBindings.length) {
    return "<span class=\"muted\">未绑定</span>";
  }
  return license.machineBindings
    .map(binding => `<div>${escapeHtml(binding.machineCode)}</div>`)
    .join("");
}

function canonicalJson(value) {
  return JSON.stringify(value);
}

function signLicensePayload(payload) {
  return crypto
    .createSign("RSA-SHA256")
    .update(canonicalJson(payload))
    .end()
    .sign(config.licensePrivateKey, "base64");
}

function buildLicensePayload(license, machineCode) {
  return {
    licenseKey: license.licenseKey,
    customerName: license.customerName,
    productCode: "signal_detect",
    edition: "pro",
    machineCode: normalizeMachineCode(machineCode),
    validUntil: license.validUntil,
    issuedAt: new Date().toISOString(),
    features: license.features
  };
}

function licenseResponseData(license, machineCode) {
  const payload = buildLicensePayload(license, machineCode);
  const payloadJson = canonicalJson(payload);
  return {
    licenseKey: license.licenseKey,
    customerName: license.customerName,
    validUntil: license.validUntil,
    machineCode: normalizeMachineCode(machineCode),
    maxActivations: license.maxActivations,
    machineBindings: license.machineBindings,
    features: license.features,
    payload,
    payloadJson,
    signature: signLicensePayload(payload)
  };
}

function findMachineBinding(user, machineCode) {
  const normalizedMachineCode = normalizeMachineCode(machineCode);
  return user.machineBindings.find(binding => binding.machineCode === normalizedMachineCode);
}

function bindMachineIfAllowed(user, machineCode) {
  const normalizedMachineCode = normalizeMachineCode(machineCode);
  const existing = findMachineBinding(user, normalizedMachineCode);
  if (existing) {
    return { ok: true, binding: existing, added: false };
  }

  if (user.machineBindings.length >= user.maxMachineBindings) {
    return { ok: false, reason: `该账号已绑定 ${user.machineBindings.length}/${user.maxMachineBindings} 台设备，请联系管理员解绑或提高绑定数量` };
  }

  const binding = {
    machineCode: normalizedMachineCode,
    boundAt: new Date().toISOString()
  };
  user.machineBindings.push(binding);
  return { ok: true, binding, added: true };
}

function machineBindingsText(user) {
  if (!user.machineBindings.length) {
    return "<span class=\"muted\">未绑定</span>";
  }
  return user.machineBindings
    .map(binding => `<div>${escapeHtml(binding.machineCode)}</div>`)
    .join("");
}

function userResponseData(user, token = "", machineCode = "") {
  const currentMachineCode = normalizeMachineCode(machineCode);
  const currentBinding = findMachineBinding(user, currentMachineCode);
  return {
    token,
    userId: user.id,
    username: user.username,
    nickname: user.nickname,
    validUntil: user.validUntil,
    machineCode: currentMachineCode || "",
    machineBoundAt: currentBinding ? currentBinding.boundAt : "",
    maxMachineBindings: user.maxMachineBindings,
    machineBindings: user.machineBindings
  };
}

function remainingDays(user) {
  if (!isValidDateString(user.validUntil)) {
    return "";
  }

  const today = new Date(`${todayDateString()}T00:00:00.000Z`);
  const validUntil = new Date(`${user.validUntil}T00:00:00.000Z`);
  return Math.ceil((validUntil.getTime() - today.getTime()) / 86400000);
}

function renderUsersPage(req, error = "") {
  const url = new URL(req.url, `http://${req.headers.host || "localhost"}`);
  const notice = url.searchParams.get("notice") || "";
  const users = loadUsers();
  const rows = users.map(user => `
    <tr>
      <td>${escapeHtml(user.id)}</td>
      <td>${escapeHtml(user.username)}</td>
      <td>${escapeHtml(user.nickname)}</td>
      <td>${escapeHtml(user.validUntil)}</td>
      <td>${remainingDays(user) >= 0 ? `${remainingDays(user)} 天` : `已过期 ${Math.abs(remainingDays(user))} 天`}</td>
      <td>${isUserExpired(user) ? "<span class=\"badge danger\">已过期</span>" : "<span class=\"badge ok\">有效</span>"}</td>
      <td>${user.machineBindings.length}/${user.maxMachineBindings}</td>
      <td>${machineBindingsText(user)}</td>
      <td>
        <a class="btn" href="/admin/users/${encodeURIComponent(user.id)}/edit">编辑</a>
        <form class="inline-form" method="post" action="/admin/users/${encodeURIComponent(user.id)}/delete" onsubmit="return confirm('确认删除该用户？');">
          <button class="btn danger" type="submit">删除</button>
        </form>
      </td>
    </tr>`).join("");

  return renderLayout("用户管理", `
<main class="shell">
  <header class="topbar">
    <div>
      <div class="brand">App 用户管理</div>
      <div class="muted">当前共 ${users.length} 个用户，App 登录接口会直接使用这里的账号。</div>
    </div>
    <form method="post" action="/admin/logout">
      <button class="btn" type="submit">退出后台</button>
    </form>
  </header>

  ${notice ? `<div class="notice">${escapeHtml(notice)}</div>` : ""}
  ${error ? `<div class="error">${escapeHtml(error)}</div>` : ""}

  <section class="panel">
    <h2 style="margin-top: 0;">新增用户</h2>
    <form method="post" action="/admin/users">
      <div class="grid">
        <div>
          <label for="username">用户名</label>
          <input id="username" name="username" required>
        </div>
        <div>
          <label for="nickname">昵称</label>
          <input id="nickname" name="nickname" required>
        </div>
        <div>
          <label for="password">初始密码</label>
          <input id="password" name="password" type="password" minlength="6" required>
        </div>
        <div>
          <label for="validUntil">授权截止日期</label>
          <input id="validUntil" name="validUntil" type="date" value="${escapeHtml(dateStringAfterDays(365))}" required>
        </div>
        <div>
          <label for="maxMachineBindings">最大绑定设备数</label>
          <input id="maxMachineBindings" name="maxMachineBindings" type="number" min="1" max="50" value="${DEFAULT_MAX_MACHINE_BINDINGS}" required>
        </div>
      </div>
      <div class="actions">
        <button class="btn primary" type="submit">创建用户</button>
      </div>
    </form>
  </section>

  <section class="panel" style="margin-top: 18px;">
    <h2 style="margin-top: 0;">用户列表</h2>
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>用户名</th>
          <th>昵称</th>
          <th>授权截止</th>
          <th>剩余时长</th>
          <th>状态</th>
          <th>绑定数量</th>
          <th>绑定机器</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        ${rows || `<tr><td colspan="9" class="muted">暂无用户</td></tr>`}
      </tbody>
    </table>
  </section>
</main>`);
}

function renderLicensesPage(req, error = "") {
  const url = new URL(req.url, `http://${req.headers.host || "localhost"}`);
  const notice = url.searchParams.get("notice") || "";
  const licenses = loadLicenses();
  const activeCount = licenses.filter(isLicenseUsable).length;
  const expiredCount = licenses.filter(isLicenseExpired).length;
  const boundCount = licenses.reduce((sum, license) => sum + license.machineBindings.length, 0);
  const rows = licenses.map(license => `
    <tr>
      <td>${escapeHtml(license.id)}</td>
      <td class="key">${escapeHtml(license.licenseKey)}</td>
      <td>${escapeHtml(license.customerName)}</td>
      <td>${escapeHtml(license.validUntil)}</td>
      <td>${remainingDays(license) >= 0 ? `${remainingDays(license)} 天` : `已过期 ${Math.abs(remainingDays(license))} 天`}</td>
      <td>${isLicenseUsable(license) ? "<span class=\"badge ok\">有效</span>" : (isLicenseExpired(license) ? "<span class=\"badge warn\">已过期</span>" : "<span class=\"badge danger\">停用</span>")}</td>
      <td>${license.machineBindings.length}/${license.maxActivations}</td>
      <td>${escapeHtml(license.features.join(", "))}</td>
      <td><div class="machine-list">${licenseBindingsText(license)}</div></td>
      <td>
        <a class="btn" href="/admin/licenses/${encodeURIComponent(license.id)}/edit">编辑</a>
        <form class="inline-form" method="post" action="/admin/licenses/${encodeURIComponent(license.id)}/delete" onsubmit="return confirm('确认删除该许可证？');">
          <button class="btn danger" type="submit">删除</button>
        </form>
      </td>
    </tr>`).join("");

  return renderLayout("许可证管理", `
<header class="admin-header">
  <div class="shell">
    <div class="nav-row">
      <div>
        <div class="brand">Signal Detect 许可证控制台</div>
        <div class="subtitle">管理离线许可证、设备绑定、授权期限和功能权限</div>
      </div>
      <div class="nav-actions">
        <a class="btn" href="/admin/users">旧用户管理</a>
        <form method="post" action="/admin/logout">
          <button class="btn" type="submit">退出后台</button>
        </form>
      </div>
    </div>
  </div>
</header>

<main class="shell">
  <section class="stats">
    <div class="stat">
      <div class="stat-label">许可证总数</div>
      <div class="stat-value">${licenses.length}</div>
    </div>
    <div class="stat">
      <div class="stat-label">有效许可证</div>
      <div class="stat-value">${activeCount}</div>
    </div>
    <div class="stat">
      <div class="stat-label">已过期</div>
      <div class="stat-value">${expiredCount}</div>
    </div>
    <div class="stat">
      <div class="stat-label">已绑定设备</div>
      <div class="stat-value">${boundCount}</div>
    </div>
  </section>

  ${notice ? `<div class="notice">${escapeHtml(notice)}</div>` : ""}
  ${error ? `<div class="error">${escapeHtml(error)}</div>` : ""}

  <section class="panel">
    <div class="section-title">
      <h2>签发新许可证</h2>
      <span class="muted">许可证 Key 可直接发给客户用于首次激活</span>
    </div>
    <form method="post" action="/admin/licenses">
      <div class="grid">
        <div>
          <label for="licenseKey">许可证 Key</label>
          <input id="licenseKey" name="licenseKey" value="${escapeHtml(generateLicenseKey())}" required>
        </div>
        <div>
          <label for="customerName">客户名称</label>
          <input id="customerName" name="customerName" required>
        </div>
        <div>
          <label for="validUntil">授权截止日期</label>
          <input id="validUntil" name="validUntil" type="date" value="${escapeHtml(dateStringAfterDays(365))}" required>
        </div>
        <div>
          <label for="maxActivations">最大绑定设备数</label>
          <input id="maxActivations" name="maxActivations" type="number" min="1" max="100" value="${DEFAULT_MAX_MACHINE_BINDINGS}" required>
        </div>
        <div>
          <label for="features">功能权限</label>
          <input id="features" name="features" value="${escapeHtml(DEFAULT_LICENSE_FEATURES.join(","))}" required>
        </div>
      </div>
      <div class="actions">
        <button class="btn primary" type="submit">创建许可证</button>
      </div>
    </form>
  </section>

  <section class="panel" style="margin-top: 18px;">
    <div class="section-title">
      <h2>许可证列表</h2>
      <span class="muted">日常使用不联网，只有激活和刷新授权会请求服务端</span>
    </div>
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>许可证</th>
          <th>客户</th>
          <th>授权截止</th>
          <th>剩余时长</th>
          <th>状态</th>
          <th>绑定数量</th>
          <th>功能</th>
          <th>绑定机器</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        ${rows || `<tr><td colspan="10" class="muted">暂无许可证</td></tr>`}
      </tbody>
    </table>
  </section>
</main>`);
}

function renderEditLicensePage(license, error = "") {
  return renderLayout("编辑许可证", `
<main class="shell">
  <header class="topbar">
    <div>
      <div class="brand">编辑许可证</div>
      <div class="muted">${escapeHtml(license.licenseKey)}</div>
    </div>
    <a class="btn" href="/admin/licenses">返回列表</a>
  </header>

  ${error ? `<div class="error">${escapeHtml(error)}</div>` : ""}

  <section class="panel">
    <form method="post" action="/admin/licenses/${encodeURIComponent(license.id)}/edit">
      <div class="grid">
        <div>
          <label for="licenseKey">许可证 Key</label>
          <input id="licenseKey" name="licenseKey" value="${escapeHtml(license.licenseKey)}" required>
        </div>
        <div>
          <label for="customerName">客户名称</label>
          <input id="customerName" name="customerName" value="${escapeHtml(license.customerName)}" required>
        </div>
        <div>
          <label for="status">状态</label>
          <input id="status" name="status" value="${escapeHtml(license.status)}" required>
        </div>
        <div>
          <label for="validUntil">授权截止日期</label>
          <input id="validUntil" name="validUntil" type="date" value="${escapeHtml(license.validUntil)}" required>
        </div>
        <div>
          <label for="maxActivations">最大绑定设备数</label>
          <input id="maxActivations" name="maxActivations" type="number" min="1" max="100" value="${escapeHtml(license.maxActivations)}" required>
        </div>
        <div>
          <label for="features">功能权限</label>
          <input id="features" name="features" value="${escapeHtml(license.features.join(","))}" required>
        </div>
        <div>
          <label>当前绑定机器</label>
          <input value="${escapeHtml(license.machineBindings.map(binding => binding.machineCode).join(", ") || "未绑定")}" readonly>
        </div>
      </div>
      <div style="margin-top: 14px;">
        <label style="display: inline-flex; align-items: center; gap: 8px; color: var(--text);">
          <input name="unbindMachine" type="checkbox" value="1" style="width: auto; min-height: auto;">
          清空该许可证的所有机器绑定
        </label>
      </div>
      <div class="actions">
        <button class="btn primary" type="submit">保存修改</button>
      </div>
    </form>
  </section>
</main>`);
}

function renderEditUserPage(user, error = "") {
  return renderLayout("编辑用户", `
<main class="shell">
  <header class="topbar">
    <div>
      <div class="brand">编辑用户</div>
      <div class="muted">${escapeHtml(user.username)}</div>
    </div>
    <a class="btn" href="/admin/users">返回列表</a>
  </header>

  ${error ? `<div class="error">${escapeHtml(error)}</div>` : ""}

  <section class="panel">
    <form method="post" action="/admin/users/${encodeURIComponent(user.id)}/edit">
      <div class="grid">
        <div>
          <label for="username">用户名</label>
          <input id="username" name="username" value="${escapeHtml(user.username)}" required>
        </div>
        <div>
          <label for="nickname">昵称</label>
          <input id="nickname" name="nickname" value="${escapeHtml(user.nickname)}" required>
        </div>
        <div>
          <label for="password">重置密码</label>
          <input id="password" name="password" type="password" minlength="6" placeholder="留空则不修改">
        </div>
        <div>
          <label for="validUntil">授权截止日期</label>
          <input id="validUntil" name="validUntil" type="date" value="${escapeHtml(user.validUntil)}" required>
        </div>
        <div>
          <label for="maxMachineBindings">最大绑定设备数</label>
          <input id="maxMachineBindings" name="maxMachineBindings" type="number" min="1" max="50" value="${escapeHtml(user.maxMachineBindings)}" required>
        </div>
        <div>
          <label>当前绑定机器</label>
          <input value="${escapeHtml(user.machineBindings.map(binding => binding.machineCode).join(", ") || "未绑定")}" readonly>
        </div>
      </div>
      <div style="margin-top: 14px;">
        <label style="display: inline-flex; align-items: center; gap: 8px; color: var(--text);">
          <input name="unbindMachine" type="checkbox" value="1" style="width: auto; min-height: auto;">
          清空该账号的所有机器绑定
        </label>
        <div class="muted" style="font-size: 13px; margin-top: 4px;">清空后，该账号后续登录会按最大绑定设备数重新绑定。</div>
      </div>
      <div class="actions">
        <button class="btn primary" type="submit">保存修改</button>
      </div>
    </form>
  </section>
</main>`);
}

function nextUserId(users) {
  const maxId = users
    .map(user => Number(user.id))
    .filter(Number.isFinite)
    .reduce((max, id) => Math.max(max, id), 10000);
  return String(maxId + 1);
}

function validateUserInput({ username, nickname, password }, requirePassword) {
  const cleanUsername = String(username || "").trim();
  const cleanNickname = String(nickname || "").trim();
  const cleanPassword = String(password || "");

  if (!cleanUsername || !cleanNickname) {
    return "用户名和昵称不能为空";
  }
  if (!/^[A-Za-z0-9_.@-]{3,64}$/.test(cleanUsername)) {
    return "用户名只能包含字母、数字、下划线、点、@ 和横线，长度 3-64 位";
  }
  if (requirePassword && cleanPassword.length < 6) {
    return "密码长度不能少于 6 位";
  }
  if (!requirePassword && cleanPassword && cleanPassword.length < 6) {
    return "新密码长度不能少于 6 位";
  }
  return "";
}

function validateAuthorizationInput({ validUntil }) {
  if (!isValidDateString(validUntil)) {
    return "授权截止日期格式不正确";
  }
  return "";
}

function validateMachineBindingInput({ maxMachineBindings }) {
  const count = Number(maxMachineBindings);
  if (!Number.isInteger(count) || count < 1 || count > 50) {
    return "最大绑定设备数必须是 1-50 之间的整数";
  }
  return "";
}

function validateLicenseInput({ licenseKey, customerName, validUntil, maxActivations }) {
  if (!isValidLicenseKey(licenseKey)) {
    return "许可证 Key 只能包含字母、数字、下划线和横线，长度 6-64 位";
  }
  if (!String(customerName || "").trim()) {
    return "客户名称不能为空";
  }
  if (!isValidDateString(validUntil)) {
    return "授权截止日期格式不正确";
  }
  const count = Number(maxActivations);
  if (!Number.isInteger(count) || count < 1 || count > 100) {
    return "最大绑定设备数必须是 1-100 之间的整数";
  }
  return "";
}

async function handleLogin(req, res) {
  let body;
  try {
    body = await readJsonBody(req);
  } catch (error) {
    sendJson(res, 400, appResponse(400, "请求体不是合法 JSON"));
    return;
  }

  const username = typeof body.username === "string" ? body.username.trim() : "";
  const password = typeof body.password === "string" ? body.password : "";
  const machineCode = normalizeMachineCode(body.machineCode);

  if (!username || !password) {
    sendJson(res, 200, appResponse(400, "用户名或密码不能为空"));
    return;
  }
  if (!isValidMachineCode(machineCode)) {
    sendJson(res, 200, appResponse(400, "机器码无效，请重新打开 App 后再试"));
    return;
  }

  const users = loadUsers();
  const user = users.find(item => item.username === username);
  if (!user || !verifyPassword(password, user.passwordHash)) {
    sendJson(res, 200, appResponse(401, "账号或密码错误"));
    return;
  }

  if (isUserExpired(user)) {
    sendJson(res, 200, appResponse(403, "账号授权已过期，请联系管理员"));
    return;
  }

  const bindResult = bindMachineIfAllowed(user, machineCode);
  if (!bindResult.ok) {
    sendJson(res, 200, appResponse(403, bindResult.reason));
    return;
  }
  if (bindResult.added) {
    saveUsers(users);
  }

  const token = createToken(user, config.tokenSecret, machineCode);
  sendJson(res, 200, appResponse(200, "登录成功", userResponseData(user, token, machineCode)));
}

async function handleLicenseActivate(req, res) {
  let body;
  try {
    body = await readJsonBody(req);
  } catch (error) {
    sendJson(res, 400, appResponse(400, "请求体不是合法 JSON"));
    return;
  }

  const licenseKey = normalizeLicenseKey(body.licenseKey);
  const machineCode = normalizeMachineCode(body.machineCode);
  if (!isValidLicenseKey(licenseKey)) {
    sendJson(res, 200, appResponse(400, "许可证格式无效"));
    return;
  }
  if (!isValidMachineCode(machineCode)) {
    sendJson(res, 200, appResponse(400, "机器码无效，请重新打开 App 后再试"));
    return;
  }

  const licenses = loadLicenses();
  const license = licenses.find(item => item.licenseKey === licenseKey);
  if (!license) {
    sendJson(res, 200, appResponse(404, "许可证不存在"));
    return;
  }
  if (license.status !== "ACTIVE") {
    sendJson(res, 200, appResponse(403, "许可证已停用，请联系管理员"));
    return;
  }
  if (isLicenseExpired(license)) {
    sendJson(res, 200, appResponse(403, "许可证已过期，请联系管理员续期"));
    return;
  }

  const bindResult = bindLicenseMachineIfAllowed(license, machineCode);
  if (!bindResult.ok) {
    sendJson(res, 200, appResponse(403, bindResult.reason));
    return;
  }
  if (bindResult.added) {
    saveLicenses(licenses);
  }

  sendJson(res, 200, appResponse(200, "许可证激活成功", licenseResponseData(license, machineCode)));
}

async function handleLicenseRefresh(req, res) {
  let body;
  try {
    body = await readJsonBody(req);
  } catch (error) {
    sendJson(res, 400, appResponse(400, "请求体不是合法 JSON"));
    return;
  }

  const licenseKey = normalizeLicenseKey(body.licenseKey);
  const machineCode = normalizeMachineCode(body.machineCode);
  const licenses = loadLicenses();
  const license = licenses.find(item => item.licenseKey === licenseKey);
  if (!license) {
    sendJson(res, 200, appResponse(404, "许可证不存在"));
    return;
  }
  if (!findLicenseBinding(license, machineCode)) {
    sendJson(res, 200, appResponse(403, "当前设备未绑定该许可证，请重新激活或联系管理员"));
    return;
  }
  if (!isLicenseUsable(license)) {
    sendJson(res, 200, appResponse(403, "许可证不可用或已过期"));
    return;
  }

  sendJson(res, 200, appResponse(200, "许可证已刷新", licenseResponseData(license, machineCode)));
}

function handleCurrentUser(req, res) {
  const payload = authPayloadFromRequest(req);
  if (!payload || !payload.sub) {
    sendJson(res, 200, appResponse(401, "登录已失效，请重新登录"));
    return;
  }

  const user = loadUsers().find(item => item.id === payload.sub);
  if (!user) {
    sendJson(res, 200, appResponse(404, "账号不存在"));
    return;
  }
  if (!findMachineBinding(user, payload.machineCode)) {
    sendJson(res, 200, appResponse(403, "当前设备未绑定该账号，请重新登录或联系管理员解绑"));
    return;
  }

  if (isUserExpired(user)) {
    sendJson(res, 200, appResponse(403, "账号授权已过期，请联系管理员", userResponseData(user, "", payload.machineCode)));
    return;
  }

  sendJson(res, 200, appResponse(200, "ok", userResponseData(user, "", payload.machineCode)));
}

async function handleChangePassword(req, res) {
  const payload = authPayloadFromRequest(req);
  if (!payload || !payload.sub) {
    sendJson(res, 200, appResponse(401, "登录已失效，请重新登录"));
    return;
  }

  let body;
  try {
    body = await readJsonBody(req);
  } catch (error) {
    sendJson(res, 400, appResponse(400, "请求体不是合法 JSON"));
    return;
  }

  const oldPassword = typeof body.oldPassword === "string" ? body.oldPassword : "";
  const newPassword = typeof body.newPassword === "string" ? body.newPassword : "";

  if (!oldPassword || !newPassword) {
    sendJson(res, 200, appResponse(400, "当前密码和新密码不能为空"));
    return;
  }
  if (newPassword.length < 6) {
    sendJson(res, 200, appResponse(400, "新密码长度不能少于 6 位"));
    return;
  }
  if (oldPassword === newPassword) {
    sendJson(res, 200, appResponse(400, "新密码不能与当前密码相同"));
    return;
  }

  const users = loadUsers();
  const user = users.find(item => item.id === payload.sub);
  if (!user) {
    sendJson(res, 200, appResponse(404, "账号不存在"));
    return;
  }
  if (!findMachineBinding(user, payload.machineCode)) {
    sendJson(res, 200, appResponse(403, "当前设备未绑定该账号，请重新登录或联系管理员解绑"));
    return;
  }
  if (isUserExpired(user)) {
    sendJson(res, 200, appResponse(403, "账号授权已过期，请联系管理员"));
    return;
  }
  if (!verifyPassword(oldPassword, user.passwordHash)) {
    sendJson(res, 200, appResponse(401, "当前密码错误"));
    return;
  }

  user.passwordHash = hashPassword(newPassword);
  saveUsers(users);
  sendJson(res, 200, appResponse(200, "密码修改成功，请重新登录"));
}

async function handleAdminLogin(req, res) {
  if (!isAdminConfigured()) {
    sendHtml(res, 503, renderAdminLogin("后台管理未启用"));
    return;
  }

  let form;
  try {
    form = await readFormBody(req);
  } catch (error) {
    sendHtml(res, 400, renderAdminLogin("请求格式错误"));
    return;
  }

  if (form.username === config.adminUsername && form.password === config.adminPassword) {
    redirect(res, "/admin/licenses", {
      "Set-Cookie": createAdminCookie()
    });
    return;
  }

  sendHtml(res, 401, renderAdminLogin("管理员账号或密码错误"));
}

async function handleCreateUser(req, res) {
  const form = await readFormBody(req);
  const error = validateUserInput(form, true) || validateAuthorizationInput(form) || validateMachineBindingInput({
    maxMachineBindings: form.maxMachineBindings || DEFAULT_MAX_MACHINE_BINDINGS
  });
  if (error) {
    sendHtml(res, 400, renderUsersPage(req, error));
    return;
  }

  const users = loadUsers();
  const username = form.username.trim();
  if (users.some(user => user.username === username)) {
    sendHtml(res, 409, renderUsersPage(req, "用户名已存在"));
    return;
  }

  users.push({
    id: nextUserId(users),
    username,
    nickname: form.nickname.trim(),
    validUntil: form.validUntil,
    maxMachineBindings: parseMaxMachineBindings(form.maxMachineBindings || DEFAULT_MAX_MACHINE_BINDINGS),
    machineBindings: [],
    passwordHash: hashPassword(form.password)
  });
  saveUsers(users);

  redirectWithNotice(res, "用户已创建");
}

function findUserById(users, id) {
  return users.find(user => user.id === id);
}

async function handleEditUser(req, res, userId) {
  const users = loadUsers();
  const user = findUserById(users, userId);
  if (!user) {
    sendHtml(res, 404, renderLayout("用户不存在", "<main class=\"shell\"><div class=\"error\">用户不存在</div></main>"));
    return;
  }

  const form = await readFormBody(req);
  const error = validateUserInput(form, false) || validateAuthorizationInput(form) || validateMachineBindingInput(form);
  if (error) {
    sendHtml(res, 400, renderEditUserPage(user, error));
    return;
  }

  const username = form.username.trim();
  if (users.some(item => item.id !== userId && item.username === username)) {
    sendHtml(res, 409, renderEditUserPage(user, "用户名已存在"));
    return;
  }

  user.username = username;
  user.nickname = form.nickname.trim();
  user.validUntil = form.validUntil;
  user.maxMachineBindings = parseMaxMachineBindings(form.maxMachineBindings);
  if (user.machineBindings.length > user.maxMachineBindings) {
    user.machineBindings = user.machineBindings.slice(0, user.maxMachineBindings);
  }
  if (form.password) {
    user.passwordHash = hashPassword(form.password);
  }
  if (form.unbindMachine === "1") {
    user.machineBindings = [];
  }
  saveUsers(users);

  redirectWithNotice(res, "用户已更新");
}

function handleDeleteUser(res, userId) {
  const users = loadUsers();
  const nextUsers = users.filter(user => user.id !== userId);
  if (nextUsers.length === users.length) {
    redirectWithNotice(res, "用户不存在");
    return;
  }

  saveUsers(nextUsers);
  redirectWithNotice(res, "用户已删除");
}

function handleAdminApiUsers(req, res) {
  if (!isAdminAuthenticated(req)) {
    sendJson(res, 401, appResponse(401, "未登录后台"));
    return;
  }

  sendJson(res, 200, appResponse(200, "ok", loadUsers().map(publicUser)));
}

function nextLicenseId(licenses) {
  const maxId = licenses
    .map(license => Number(String(license.id || "").replace(/^L/, "")))
    .filter(Number.isFinite)
    .reduce((max, id) => Math.max(max, id), 10000);
  return `L${maxId + 1}`;
}

async function handleCreateLicense(req, res) {
  const form = await readFormBody(req);
  const error = validateLicenseInput(form);
  if (error) {
    sendHtml(res, 400, renderLicensesPage(req, error));
    return;
  }

  const licenses = loadLicenses();
  const licenseKey = normalizeLicenseKey(form.licenseKey);
  if (licenses.some(license => license.licenseKey === licenseKey)) {
    sendHtml(res, 409, renderLicensesPage(req, "许可证 Key 已存在"));
    return;
  }

  licenses.push(normalizeLicense({
    id: nextLicenseId(licenses),
    licenseKey,
    customerName: form.customerName.trim(),
    status: "ACTIVE",
    validUntil: form.validUntil,
    maxActivations: parseMaxActivations(form.maxActivations),
    features: normalizeFeatures(form.features),
    machineBindings: []
  }));
  saveLicenses(licenses);
  redirectWithNotice(res, "许可证已创建");
}

function findLicenseById(licenses, id) {
  return licenses.find(license => license.id === id);
}

async function handleEditLicense(req, res, licenseId) {
  const licenses = loadLicenses();
  const license = findLicenseById(licenses, licenseId);
  if (!license) {
    sendHtml(res, 404, renderLayout("许可证不存在", "<main class=\"shell\"><div class=\"error\">许可证不存在</div></main>"));
    return;
  }

  const form = await readFormBody(req);
  const error = validateLicenseInput(form);
  if (error) {
    sendHtml(res, 400, renderEditLicensePage(license, error));
    return;
  }

  const licenseKey = normalizeLicenseKey(form.licenseKey);
  if (licenses.some(item => item.id !== licenseId && item.licenseKey === licenseKey)) {
    sendHtml(res, 409, renderEditLicensePage(license, "许可证 Key 已存在"));
    return;
  }

  license.licenseKey = licenseKey;
  license.customerName = form.customerName.trim();
  license.status = String(form.status || "ACTIVE").trim().toUpperCase();
  license.validUntil = form.validUntil;
  license.maxActivations = parseMaxActivations(form.maxActivations);
  license.features = normalizeFeatures(form.features);
  if (license.machineBindings.length > license.maxActivations) {
    license.machineBindings = license.machineBindings.slice(0, license.maxActivations);
  }
  if (form.unbindMachine === "1") {
    license.machineBindings = [];
  }
  saveLicenses(licenses);
  redirectWithNotice(res, "许可证已更新");
}

function handleDeleteLicense(res, licenseId) {
  const licenses = loadLicenses();
  const nextLicenses = licenses.filter(license => license.id !== licenseId);
  if (nextLicenses.length === licenses.length) {
    redirectWithNotice(res, "许可证不存在");
    return;
  }
  saveLicenses(nextLicenses);
  redirectWithNotice(res, "许可证已删除");
}

async function route(req, res) {
  const url = new URL(req.url, `http://${req.headers.host || "localhost"}`);

  if (req.method === "OPTIONS") {
    sendJson(res, 204, null);
    return;
  }

  if (req.method === "GET" && url.pathname === "/health") {
    sendJson(res, 200, appResponse(200, "ok", {
      status: "up",
      licenseApi: true,
      endpoints: [
        "POST /api/license/activate",
        "POST /api/license/refresh"
      ]
    }));
    return;
  }

  if (req.method === "GET" && url.pathname === "/") {
    redirect(res, "/admin");
    return;
  }

  if (req.method === "GET" && url.pathname === "/admin") {
    if (isAdminAuthenticated(req)) {
      redirect(res, "/admin/licenses");
      return;
    }
    sendHtml(res, 200, renderAdminLogin());
    return;
  }

  if (req.method === "POST" && url.pathname === "/admin/login") {
    await handleAdminLogin(req, res);
    return;
  }

  if (req.method === "POST" && url.pathname === "/admin/logout") {
    redirect(res, "/admin", {
      "Set-Cookie": clearAdminCookie(req)
    });
    return;
  }

  if (url.pathname.startsWith("/admin") && !isAdminAuthenticated(req)) {
    redirect(res, "/admin");
    return;
  }

  if (req.method === "GET" && url.pathname === "/admin/licenses") {
    sendHtml(res, 200, renderLicensesPage(req));
    return;
  }

  if (req.method === "POST" && url.pathname === "/admin/licenses") {
    await handleCreateLicense(req, res);
    return;
  }

  const licenseEditMatch = url.pathname.match(/^\/admin\/licenses\/([^/]+)\/edit$/);
  if (licenseEditMatch) {
    const licenseId = decodeURIComponent(licenseEditMatch[1]);
    const licenses = loadLicenses();
    const license = findLicenseById(licenses, licenseId);
    if (!license) {
      sendHtml(res, 404, renderLayout("许可证不存在", "<main class=\"shell\"><div class=\"error\">许可证不存在</div></main>"));
      return;
    }

    if (req.method === "GET") {
      sendHtml(res, 200, renderEditLicensePage(license));
      return;
    }

    if (req.method === "POST") {
      await handleEditLicense(req, res, licenseId);
      return;
    }
  }

  const licenseDeleteMatch = url.pathname.match(/^\/admin\/licenses\/([^/]+)\/delete$/);
  if (req.method === "POST" && licenseDeleteMatch) {
    handleDeleteLicense(res, decodeURIComponent(licenseDeleteMatch[1]));
    return;
  }

  if (req.method === "GET" && url.pathname === "/admin/users") {
    sendHtml(res, 200, renderUsersPage(req));
    return;
  }

  if (req.method === "POST" && url.pathname === "/admin/users") {
    await handleCreateUser(req, res);
    return;
  }

  if (req.method === "GET" && url.pathname === "/admin/api/users") {
    handleAdminApiUsers(req, res);
    return;
  }

  const editMatch = url.pathname.match(/^\/admin\/users\/([^/]+)\/edit$/);
  if (editMatch) {
    const userId = decodeURIComponent(editMatch[1]);
    const users = loadUsers();
    const user = findUserById(users, userId);
    if (!user) {
      sendHtml(res, 404, renderLayout("用户不存在", "<main class=\"shell\"><div class=\"error\">用户不存在</div></main>"));
      return;
    }

    if (req.method === "GET") {
      sendHtml(res, 200, renderEditUserPage(user));
      return;
    }

    if (req.method === "POST") {
      await handleEditUser(req, res, userId);
      return;
    }
  }

  const deleteMatch = url.pathname.match(/^\/admin\/users\/([^/]+)\/delete$/);
  if (req.method === "POST" && deleteMatch) {
    handleDeleteUser(res, decodeURIComponent(deleteMatch[1]));
    return;
  }

  if (req.method === "POST" && url.pathname === "/api/license/activate") {
    await handleLicenseActivate(req, res);
    return;
  }

  if (req.method === "POST" && url.pathname === "/api/license/refresh") {
    await handleLicenseRefresh(req, res);
    return;
  }

  if (req.method === "POST" && url.pathname === "/api/auth/login") {
    await handleLogin(req, res);
    return;
  }

  if (req.method === "GET" && url.pathname === "/api/auth/me") {
    handleCurrentUser(req, res);
    return;
  }

  if (req.method === "POST" && url.pathname === "/api/auth/change-password") {
    await handleChangePassword(req, res);
    return;
  }

  sendJson(res, 404, appResponse(404, `接口不存在: ${req.method} ${url.pathname}`));
}

const server = http.createServer((req, res) => {
  route(req, res).catch(error => {
    console.error(error);
    sendJson(res, 500, appResponse(500, "服务器内部错误"));
  });
});

server.listen(config.port, config.host, () => {
  console.log(`Signal Detect server listening on http://${config.host}:${config.port}`);
});
