import fs from 'node:fs';
import path from 'node:path';
import type { FullConfig } from '@playwright/test';

const jar = path.resolve(__dirname, '..', 'backend', 'target', 'globerbooks.jar');

/** Asserts the isolation rules (SC-33). Values are literals; nothing is read from the environment. */
export default async function globalSetup(config: FullConfig): Promise<void> {
  const base = new URL(config.projects[0].use.baseURL ?? '');
  if (base.hostname !== '127.0.0.1') throw new Error(`baseURL host must be 127.0.0.1, got ${base.hostname}`);
  if (base.port !== '18080') throw new Error(`baseURL port must be 18080, got ${base.port}`);
  if (!fs.existsSync(jar)) throw new Error(`application jar not found: ${jar}`);
}
