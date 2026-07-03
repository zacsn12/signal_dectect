const fs = require("fs");
const path = require("path");

function loadEnvFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return;
  }

  const lines = fs.readFileSync(filePath, "utf8").split(/\r?\n/);
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      continue;
    }

    const separatorIndex = trimmed.indexOf("=");
    if (separatorIndex === -1) {
      continue;
    }

    const key = trimmed.slice(0, separatorIndex).trim();
    const value = trimmed.slice(separatorIndex + 1).trim();
    if (!process.env[key]) {
      process.env[key] = value;
    }
  }
}

const rootDir = path.resolve(__dirname, "..");
loadEnvFile(path.join(rootDir, ".env"));

const defaultLicensePrivateKey = [
  "-----BEGIN PRIVATE KEY-----",
  "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCzTGXQeYaxBfE+",
  "NrlAt7VfJwaCAsUEHhCOh2tDPC6sQ78u+VZa4/6efXNqL/adQGTixvQWxjLc9uFf",
  "LJOR66AnmjjGbQUIP8h/lWlYXoHZRIp3eJ4Tb4u/H2BPzVZSfk7y7eSpVGbP40hN",
  "Q+8BN6YOu31KekDftoYUettfsxdEDck9hMnup2r5r/RYgq6PXGH7+lIZqPYgmDMa",
  "t2wdKDEk3RGy1A9r/2cxFJliYGKLym291x+eyoerJeUpLk6Blt+Eg3r7+xg2f3lS",
  "0HahFaPWikfqc9zuhphsSenjh02sEDPWyZArn+1ABFQkO6fw4L8gE+ta94fcqchw",
  "1/wVlgCNAgMBAAECggEARUXQlnXFeNKTbNaGx37Sx9MTnBqG1Prqqa6fXgg7/hlk",
  "nbj+yLoKz2AnvdCPJx1QfR+iAcSMtTt7QqK91yRiqbpXki5fwdqm08g9vcMxxuhI",
  "2TTUWi0AIJT2SI7Mea3MQeZwsI1n9YzaC+QhzOYbtdhxZVuLggOKlRIMfNJ8PDGz",
  "vOPbF4O+XkDgPzVwpWhxqMDswnj4HI8eToDzje/7FRDcLJKFEP6aov2Sm+/pH84M",
  "ZJFE3WdwDxQ8yOCVzryJsFfFASxe1sEAtAJPEX0k5gycAc/sHuNFWL7w5lfMT8i1",
  "cDE0Ck26jHd7B2pBN1v70jNldlfA1xDu4NQaZlEObwKBgQDczl0sYff5rrjDBNT6",
  "f2J1S41tZzhFnKSC7/w2slV0iW5X2JdXQqznxdmm6S2aWHaVPjvBMtoo2Yz05iyV",
  "iLlZXTi74exXxvt+Bske6yww9uhYouOQkg1EOzy41B9yM5KEJGnUp04X+D1lt/wD",
  "+a6tJQJWTzez+Q5WZMQ5sCStswKBgQDP4GFHupaP5FZxT0DbPypjVSCzi+MRRDgB",
  "YJoTiyaaKR1vogGeIxuSXWHJArmEeAadku+zQtzJdTX1kEBawViAoR4TRsB4z/hg",
  "u/77BUgFvCzkKK6lMWeg1piXl36ZW6frkkwdsZYQhndaQEu/ELozHucaDkJzt0Pu",
  "Oa9xGsH4vwKBgQCh7SB9FNdinpRWSCvcDCDrPd3YdlLZEfe/IjlG718l2Ec4WtkZ",
  "oAbm7bwg4G2V+/vylLIDi8RbIrdxPX9CpgKvG2MovZSyBnWWRWmmp/Y/bkKUBdh2",
  "w/TAreRo8v6gpFt1VrLZxVvKzjxQZS2GyMslpLdjDjMeY631A263k4pCawKBgCxL",
  "ps8PDng48saQWcSeUBz8jcxUmZ0bSUfZ2tshQqeE7VXVUrzsxDpLFcQshXWZ0ecP",
  "4W30aHGxPT9Hbr8oBgDa1DrNT8QupMGQLcQ1muRF9dbkaFqhDXDzaW6cBIkPQ9fp",
  "w7hsGAsLbDFaFAnxuYudISenNDfOLHZ96kmSpxSnAoGBAL6sZYdycZB7r8tbjdHO",
  "g4WyACnCn9tW7iMyAfCN5PYXmbloIHcNm9gyidsD0DKv49WugqsxCQ5QvP3AvvKx",
  "1Lavrhvrgz8afHJ3+c0dCXuzAWB4HBkDiCPjNQESlk+R6IYeRHm0e1AYn/SB3bmS",
  "yx4VZ19rv6dWl32mKa6k/TFS",
  "-----END PRIVATE KEY-----"
].join("\n");

module.exports = {
  host: process.env.HOST || "0.0.0.0",
  port: Number(process.env.PORT || 1234),
  tokenSecret: process.env.TOKEN_SECRET || "dev-only-change-this-secret",
  corsOrigin: process.env.CORS_ORIGIN || "*",
  adminUsername: process.env.ADMIN_USERNAME || "admin",
  adminPassword: process.env.ADMIN_PASSWORD || "admin123",
  adminSessionTtlSeconds: Number(process.env.ADMIN_SESSION_TTL_SECONDS || 60 * 60 * 8),
  usersFile: path.join(rootDir, "data", "users.json"),
  licensesFile: path.join(rootDir, "data", "licenses.json"),
  licensePrivateKey: (process.env.LICENSE_PRIVATE_KEY || defaultLicensePrivateKey).replace(/\\n/g, "\n")
};
