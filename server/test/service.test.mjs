import test from 'node:test';
import assert from 'node:assert/strict';
import { createApplication } from '../server.mjs';
import { DatabaseSync } from 'node:sqlite';

async function fixture(options = {}) {
  const app = createApplication({ dbPath: ':memory:', authLimit: 100, ...options });
  await new Promise(resolve => app.server.listen(0, '127.0.0.1', resolve));
  const url = `http://127.0.0.1:${app.server.address().port}`;
  const request = async (path, { token, method = 'GET', body } = {}) => {
    const r = await fetch(url + path, { method, headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), 'Content-Type': 'application/json' }, ...(body ? { body: JSON.stringify(body) } : {}) });
    return { status: r.status, data: await r.json() };
  };
  const register = async username => (await request('/auth/register', { method: 'POST', body: { username, password: 'correct horse battery staple' } })).data;
  return { ...app, request, register };
}
const workspace = () => ({ schema: 1, items: [{ id: 'doi:10.1000/test', title: 'A real record', kind: 'PAPER', summary: 'A supplied abstract.' }], projects: [{ id: 'project', title: 'Thesis', tasks: [{ id: 'task', title: 'Read the paper', status: 'TODO' }], paperIds: ['doi:10.1000/test'] }], notes: [{ id: 'note', itemId: 'doi:10.1000/test', text: 'My observation', page: 1 }], history: ['literature'] });

test('passwords are checked, usernames normalized, and sessions revoked', async t => {
  const app = await fixture(); t.after(() => app.close()); const user = await app.register('Researcher'); assert.ok(user.token);
  assert.equal((await app.request('/auth/login', { method: 'POST', body: { username: 'researcher', password: 'wrong password' } })).status, 401);
  const good = await app.request('/auth/login', { method: 'POST', body: { username: 'RESEARCHER', password: 'correct horse battery staple' } }); assert.equal(good.status, 200);
  assert.equal((await app.request('/me', { token: good.data.token })).data.user.username, 'researcher');
  await app.request('/auth/logout', { token: good.data.token, method: 'POST', body: {} }); assert.equal((await app.request('/me', { token: good.data.token })).status, 401);
  assert.notEqual(app.db.prepare('SELECT password FROM users').get().password, 'correct horse battery staple');
  assert.equal(app.db.prepare('SELECT count(*) AS n FROM sessions WHERE token_hash=?').get(user.token).n, 0);
});
test('accounts are isolated and optimistic revision conflicts preserve data', async t => {
  const app = await fixture(); t.after(() => app.close()); const a = await app.register('alice'); const b = await app.register('bruno');
  assert.equal((await app.request('/workspace', { token: a.token, method: 'PUT', body: { revision: 0, workspace: workspace() } })).status, 200);
  assert.equal((await app.request('/workspace', { token: b.token })).data.workspace.items.length, 0);
  assert.equal((await app.request('/workspace', { token: a.token, method: 'PUT', body: { revision: 0, workspace: { schema: 1, items: [], projects: [], notes: [], history: [] } } })).status, 409);
  const remote = await app.request('/workspace', { token: a.token }); assert.equal(remote.data.revision, 1); assert.equal(remote.data.workspace.notes[0].text, 'My observation');
  assert.equal((await app.request('/workspace')).status, 401);
  assert.equal((await app.request('/admin', { token: a.token })).status, 404);
});
test('malformed tasks, dangling references, and duplicate IDs are rejected', async t => {
  const app = await fixture(); t.after(() => app.close()); const a = await app.register('alice');
  for (const mutate of [w => { w.projects[0].tasks[0].status = 'FAKE'; }, w => { w.notes[0].itemId = 'missing'; }, w => { w.items.push(w.items[0]); }]) {
    const w = workspace(); mutate(w); assert.equal((await app.request('/workspace', { token: a.token, method: 'PUT', body: { revision: 0, workspace: w } })).status, 400);
  }
});
test('AI absence produces an honest unavailable state', async t => {
  const app = await fixture({ aiKey: '', aiModel: '' }); t.after(() => app.close()); const a = await app.register('alice');
  const result = await app.request('/assistant', { token: a.token, method: 'POST', body: {} }); assert.equal(result.status, 503); assert.match(result.data.error, /No answer was generated/);
});

test('source organization survives sync and malformed customization metadata is rejected', async t => {
  const app = await fixture(); t.after(() => app.close()); const a = await app.register('alice');
  const w = workspace(); Object.assign(w.items[0], { favorite: true, readingStatus: 'READING', tags: ['thesis', 'methods'] });
  assert.equal((await app.request('/workspace', { token: a.token, method: 'PUT', body: { revision: 0, workspace: w } })).status, 200);
  const saved = (await app.request('/workspace', { token: a.token })).data.workspace.items[0];
  assert.equal(saved.favorite, true); assert.equal(saved.readingStatus, 'READING'); assert.deepEqual(saved.tags, ['thesis', 'methods']);
  for (const patch of [{ readingStatus: 'MISSING' }, { favorite: 'true' }, { tags: ['x'.repeat(41)] }, { tags: 'tag' }]) {
    const bad = workspace(); Object.assign(bad.items[0], patch);
    assert.equal((await app.request('/workspace', { token: a.token, method: 'PUT', body: { revision: 1, workspace: bad } })).status, 400);
  }
  assert.equal((await app.request('/workspace', { token: a.token })).data.revision, 1);
});
test('AI validates output source identifiers and appends source references', async t => {
  let invalid = true;
  const app = await fixture({ aiKey: 'test-key', aiModel: 'test-model', fetchImpl: async () => ({ ok: true, json: async () => ({ candidates: [{ content: { parts: [{ text: JSON.stringify({ answer: 'The abstract describes a method [1].', sourceIds: [invalid ? 'invented' : 'source'] }) }] } }] }) }) });
  t.after(() => app.close()); const a = await app.register('alice'); const body = { question: 'What does it say?', sources: [{ id: 'source', title: 'Paper', text: 'The abstract describes a method.', url: 'https://example.org/paper' }] };
  assert.equal((await app.request('/assistant', { token: a.token, method: 'POST', body })).status, 502);
  invalid = false; const result = await app.request('/assistant', { token: a.token, method: 'POST', body }); assert.equal(result.status, 200); assert.match(result.data.answer, /abstracts only/); assert.match(result.data.answer, /https:\/\/example.org\/paper/);
});
test('deleting an account requires the password and cascades all private data', async t => {
  const app = await fixture(); t.after(() => app.close()); const a = await app.register('alice');
  assert.equal((await app.request('/account', { token: a.token, method: 'DELETE', body: { password: 'wrong' } })).status, 401);
  assert.equal((await app.request('/account', { token: a.token, method: 'DELETE', body: { password: 'correct horse battery staple' } })).status, 200);
  assert.equal(app.db.prepare('SELECT count(*) AS n FROM workspaces').get().n, 0); assert.equal((await app.request('/me', { token: a.token })).status, 401);
});
test('expired sessions fail and authentication is rate limited', async t => {
  let time = Date.now(); const app = await fixture({ now: () => time, authLimit: 2 }); t.after(() => app.close()); const a = await app.register('alice');
  time += 8 * 86400000; assert.equal((await app.request('/me', { token: a.token })).status, 401);
  const body = { username: 'alice', password: 'wrong' }; await app.request('/auth/login', { method: 'POST', body }); await app.request('/auth/login', { method: 'POST', body });
  assert.equal((await app.request('/auth/login', { method: 'POST', body })).status, 429);
});
test('a database backup restores complete research records', async t => {
  const app = await fixture(); t.after(() => app.close()); const a = await app.register('alice');
  await app.request('/workspace', { token: a.token, method: 'PUT', body: { revision: 0, workspace: workspace() } });
  const { mkdtempSync } = await import('node:fs'); const { tmpdir } = await import('node:os'); const { join } = await import('node:path');
  const directory = mkdtempSync(join(tmpdir(), 'overthink-backup-test-')); const file = join(directory, 'backup.sqlite');
  app.db.prepare('VACUUM INTO ?').run(file);
  const restored = new DatabaseSync(file); try { const row = restored.prepare('SELECT body FROM workspaces WHERE user_id=?').get(a.user.id); assert.equal(JSON.parse(row.body).notes[0].text, 'My observation'); } finally { restored.close(); }
});
