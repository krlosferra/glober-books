// Launcher used as webServer.command (SC-33, SEC-F-070, SEC-F-080).
// Plain Node, no dependency. It never reads host, port or datasource from its environment
// and never opens the database file.
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import net from 'node:net';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const PREFIX = 'globerbooks-e2e-';
const HOST = '127.0.0.1';
const PORT = '18080';
const ENV_ALLOWLIST = ['PATH', 'HOME', 'JAVA_HOME', 'LANG', 'TZ'];
const here = path.dirname(fileURLToPath(import.meta.url));
const jar = path.resolve(here, '..', '..', 'backend', 'target', 'globerbooks.jar');

/** Throws unless dir is a real, non-symlink direct child of the real tmpdir with the prefix. */
function assertOwnDirectory(dir) {
  const st = fs.lstatSync(dir);
  if (st.isSymbolicLink() || !st.isDirectory()) throw new Error('temporary path is not a real directory');
  const real = fs.realpathSync(dir);
  if (real !== dir) throw new Error('temporary path is not canonical');
  if (path.dirname(real) !== fs.realpathSync(os.tmpdir())) throw new Error('temporary path is not a direct child of tmpdir');
  if (!path.basename(real).startsWith(PREFIX)) throw new Error('temporary path has the wrong prefix');
}

function allowlistedEnv() {
  const env = {};
  for (const key of ENV_ALLOWLIST) if (process.env[key] !== undefined) env[key] = process.env[key];
  return env;
}

/** Resolves true when anything accepts a TCP connection on the fixed address. */
function portIsBusy() {
  return new Promise((resolve) => {
    const socket = net.connect({ host: HOST, port: Number(PORT) });
    socket.setTimeout(2000, () => { socket.destroy(); resolve(true); });
    socket.on('connect', () => { socket.destroy(); resolve(true); });
    socket.on('error', () => resolve(false));
  });
}

async function main() {
  if (await portIsBusy()) {
    console.error(`port ${PORT} on ${HOST} is already in use; refusing to start`);
    process.exit(1);
  }
  if (process.argv.includes('--check-port')) process.exit(0);
  if (!fs.existsSync(jar)) throw new Error(`jar not found: ${jar}`);
  const dir = fs.mkdtempSync(path.join(fs.realpathSync(os.tmpdir()), PREFIX));
  fs.chmodSync(dir, 0o700);
  assertOwnDirectory(dir);
  const db = path.join(dir, 'library.db');
  if (path.dirname(db) !== dir) throw new Error('database path is outside the temporary directory');

  const args = ['-jar', jar, `--server.address=${HOST}`, `--server.port=${PORT}`,
    `--spring.datasource.url=jdbc:sqlite:${db}`];
  const child = spawn('java', args, { cwd: dir, env: allowlistedEnv(), stdio: 'inherit' });

  let cleaned = false;
  const cleanup = () => {
    if (cleaned) return;
    cleaned = true;
    try {
      assertOwnDirectory(dir);
      fs.rmSync(dir, { recursive: true, force: true });
    } catch (e) {
      console.error(`cleanup refused: ${e.message}`);
    }
  };
  for (const sig of ['SIGTERM', 'SIGINT']) process.on(sig, () => child.kill(sig));
  child.on('error', (e) => { console.error(`cannot start java: ${e.message}`); cleanup(); process.exit(1); });
  child.on('exit', (code, signal) => {
    cleanup();
    process.exit(code ?? (signal ? 128 + (os.constants.signals[signal] ?? 0) : 1));
  });
}

main().catch((e) => { console.error(e.message); process.exit(1); });
