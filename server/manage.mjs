import { DatabaseSync } from 'node:sqlite';
import { resolve } from 'node:path';
import { existsSync } from 'node:fs';
const path = process.env.DATABASE_PATH || resolve('data/overthink.sqlite');
if (process.argv[2] !== 'backup') throw new Error('Usage: node manage.mjs backup [destination.sqlite]');
const destination = resolve(process.argv[3] || `overthink-backup-${Date.now()}.sqlite`);
if (existsSync(destination)) throw new Error('Backup destination already exists');
const db = new DatabaseSync(path); db.prepare('VACUUM INTO ?').run(destination); db.close();
process.stdout.write(`Database backup saved: ${destination}\n`);
