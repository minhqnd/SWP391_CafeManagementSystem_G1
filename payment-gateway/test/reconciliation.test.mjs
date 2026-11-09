import { test } from 'node:test';
import assert from 'node:assert/strict';
import { DatabaseSync } from 'node:sqlite';
import { readFileSync } from 'node:fs';
import { build } from 'esbuild';

const bundle = await build({ entryPoints: ['src/index.ts'], bundle: true, write: false, format: 'esm', platform: 'browser' });
const { default: app } = await import('data:text/javascript;base64,' + Buffer.from(bundle.outputFiles[0].contents).toString('base64'));

function fixture() {
    const sql = new DatabaseSync(':memory:');
    sql.exec(readFileSync('schema.sql', 'utf8'));
    const db = {
        prepare(query) {
            let args = [];
            return {
                bind(...values) { args = values; return this; },
                async first() { return sql.prepare(query).get(...args) || null; },
                async all() { return { results: sql.prepare(query).all(...args) }; },
                async run() { return { meta: sql.prepare(query).run(...args) }; }
            };
        }
    };
    const env = { DB: db, ALLOW_TEST_SIMULATION: 'true' };
    const register = (code = 'AURA-123', amount = 100000) => app.request('/api/orders/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ appId: 'aura_cafe', orderCode: code, amount, transactionCode: 'TX-' + code })
    }, env);
    const sendWebhook = (id, amount, content = 'Pay AURA-123', transferType = 'in') => app.request('/api/webhooks/sepay', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id, transferAmount: amount, content, accountNumber: '123456', transferType })
    }, env);
    const getStatus = async (code = 'AURA-123') => (await app.request(`/api/orders/${code}/status`, {}, env)).json();
    return { sql, env, register, sendWebhook, getStatus };
}

test('register order and confirm via SePay webhook without token', async () => {
    const f = fixture();
    assert.equal((await f.register('ORD-261109-1001', 120000)).status, 200);
    assert.equal((await f.getStatus('ORD-261109-1001')).status, 'PENDING');

    const res = await f.sendWebhook(1, 120000, 'Chuyen khoan ORD-261109-1001');
    assert.equal(res.status, 200);
    assert.equal((await f.getStatus('ORD-261109-1001')).status, 'SUCCESS');
});

test('underpayment keeps order PENDING', async () => {
    const f = fixture();
    await f.register('ORD-261109-1002', 120000);
    await f.sendWebhook(2, 50000, 'Chuyen khoan ORD-261109-1002');
    assert.equal((await f.getStatus('ORD-261109-1002')).status, 'PENDING');
});
