const os = require("os");
const fs = require("fs");
const { spawnSync } = require("child_process");
const crypto = require("crypto");
const axios = require("axios");

const config = require("./config.json");

const cf = "cloudflared" + (os.platform() == "win32" ? ".exe" : "");
const host = process.argv[2];
const name = host + " - for DL";

(async () => {

  const secret = crypto.randomUUID();
  const path = "./tunnels/" + secret + "/";
  fs.mkdirSync(path);
  console.log("Secret:", secret);

  const out = spawnSync(
    cf,
    ["tunnel", "create", "--cred-file", path + "t", name]
  );

  const uuid = JSON.parse(fs.readFileSync(path + "t")).TunnelID;

  fs.writeFileSync(path + "i", `tunnel: ${uuid}
  credentials-file: ./tunnel.json

  ingress:
    - hostname: ${host}
      service: tcp://127.0.0.1:25565
    - service: http_status:404
  `);

  const base = "https://api.cloudflare.com/client/v4/";
  const headers = {
    "Content-Type": "application/json",
    "X-Auth-Key": config.api_key,
    "X-Auth-Email": config.email
  };

  const domain = host.split(".").slice(-2).join(".");
  const zones = {};
  const rz = await axios.get(base + "zones", { headers });
  for (const entry of rz.data.result) {
    zones[entry.name] = entry.id;
  }
  if (!(domain in zones)) {
    console.error(
      "The domain '" + domain + "' does not belong to you!"
    );
    process.exit(1);
  }
  const zone = zones[domain];
  const crecords = {};
  const trecords = {};
  const rr = await axios.get(base + "zones/" + zone + "/dns_records", { headers });
  for (const entry of rr.data.result) {
    if (entry.type == "CNAME") crecords[entry.name] = entry.id;
    if (entry.type == "TXT") trecords[entry.name] = entry.id;
  }

  const csettings = {
    "type": "CNAME",
    "name": host,
    "content": uuid + ".cfargotunnel.com",
    "proxied": true,
    "comment": "For Dmitry Launcher"
  };

  if (!(host in crecords)) {
    await axios.post(
      base + "zones/" + zone + "/dns_records",
      csettings,
      { headers }
    );
  } else {
    await axios.put(
      base + "zones/" + zone + "/dns_records/" + crecords[host],
      csettings,
      { headers }
    );
  }

  const tsettings = {
    "type": "TXT",
    "name": host,
    "content": "\"cloudflared-use-tunnel\""
  };
  if (!(host in trecords)) {
    await axios.post(
      base + "zones/" + zone + "/dns_records",
      tsettings,
      { headers }
    );
  } else {
    await axios.put(
      base + "zones/" + zone + "/dns_records/" + trecords[host],
      tsettings,
      { headers }
    );
  }

})();
