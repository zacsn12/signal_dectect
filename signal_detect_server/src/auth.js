const crypto = require("crypto");

const HASH_ALGORITHM = "sha256";
const HASH_ITERATIONS = 120000;
const HASH_KEY_LENGTH = 32;
const TOKEN_TTL_SECONDS = 60 * 60 * 24 * 7;

function hashPassword(password, salt = crypto.randomBytes(16)) {
  const key = crypto.pbkdf2Sync(
    password,
    salt,
    HASH_ITERATIONS,
    HASH_KEY_LENGTH,
    HASH_ALGORITHM
  );

  return [
    "pbkdf2_sha256",
    HASH_ITERATIONS,
    salt.toString("base64"),
    key.toString("base64")
  ].join("$");
}

function verifyPassword(password, storedHash) {
  const parts = String(storedHash || "").split("$");
  if (parts.length !== 4 || parts[0] !== "pbkdf2_sha256") {
    return false;
  }

  const iterations = Number(parts[1]);
  const salt = Buffer.from(parts[2], "base64");
  const expected = Buffer.from(parts[3], "base64");
  const actual = crypto.pbkdf2Sync(
    password,
    salt,
    iterations,
    expected.length,
    HASH_ALGORITHM
  );

  return expected.length === actual.length && crypto.timingSafeEqual(expected, actual);
}

function base64UrlEncode(value) {
  return Buffer.from(JSON.stringify(value))
    .toString("base64url");
}

function base64UrlDecode(value) {
  return JSON.parse(Buffer.from(value, "base64url").toString("utf8"));
}

function sign(value, secret) {
  return crypto
    .createHmac("sha256", secret)
    .update(value)
    .digest("base64url");
}

function createToken(user, secret, machineCode = "") {
  const now = Math.floor(Date.now() / 1000);
  const header = {
    alg: "HS256",
    typ: "JWT"
  };
  const payload = {
    sub: user.id,
    username: user.username,
    nickname: user.nickname,
    machineCode,
    iat: now,
    exp: now + TOKEN_TTL_SECONDS
  };

  const encodedHeader = base64UrlEncode(header);
  const encodedPayload = base64UrlEncode(payload);
  const unsignedToken = `${encodedHeader}.${encodedPayload}`;

  return `${unsignedToken}.${sign(unsignedToken, secret)}`;
}

function verifyToken(token, secret) {
  const parts = String(token || "").split(".");
  if (parts.length !== 3) {
    return null;
  }

  const unsignedToken = `${parts[0]}.${parts[1]}`;
  const expectedSignature = sign(unsignedToken, secret);
  const actualSignature = parts[2];
  const expected = Buffer.from(expectedSignature);
  const actual = Buffer.from(actualSignature);

  if (expected.length !== actual.length || !crypto.timingSafeEqual(expected, actual)) {
    return null;
  }

  try {
    const payload = base64UrlDecode(parts[1]);
    const now = Math.floor(Date.now() / 1000);
    if (typeof payload.exp === "number" && payload.exp < now) {
      return null;
    }
    return payload;
  } catch (error) {
    return null;
  }
}

module.exports = {
  createToken,
  hashPassword,
  verifyPassword,
  verifyToken
};
