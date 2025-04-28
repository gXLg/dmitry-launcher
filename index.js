const express = require("express");
const fs = require("fs");

const app = express();

app.get("/:secret/i", (req, res) => {
  const s = req.params.secret;
  if (!s.match(/^[0-9a-fA-F\-]+$/)) return res.end("");
  try {
    res.end(fs.readFileSync("./tunnels/" + s + "/i", "utf8"));
  } catch {
    res.end("");
  }
});

app.get("/:secret/t", (req, res) => {
  const s = req.params.secret;
  if (!s.match(/^[0-9a-fA-F\-]+$/)) return res.end("");
  try {
    res.end(fs.readFileSync("./tunnels/" + s + "/t", "utf8"));
  } catch {
    res.end("");
  }
});

const server = app.listen(process.argv[2] ? parseInt(process.argv[2]) : 8080);

process.on("SIGINT", () => server.close());

