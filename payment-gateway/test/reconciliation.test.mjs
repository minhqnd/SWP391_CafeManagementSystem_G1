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
    let fail = false;
    const db = {
        prepare(query) {
            let args = [];
            return {
                bind(...values) { args = values; return this; },
                async first() { return sql.prepare(query).get(...args) || null; },
                async all() { return { results: sql.prepare(query).all(...args) }; },
                async run() { return { meta: sql.prepare(query).run(...args) }; }
            };
        },
        async batch(statements) {
            sql.exec('BEGIN');
            try {
                const results = [];
                for (let i = 0; i < statements.length; i++) {
                    results.push(await statements[i].run());
                    if (fail && i === 0) throw Error('Injected storage outage');
                }
                sql.exec('COMMIT');
                return results;
            } catch (e) { sql.exec('ROLLBACK'); throw e; }
        }
    };
    const env = { DB: db, SECRET_KEY: 'test-only-secret', SEPAY_API_KEY: 'test-only-key' };
    const register = (code = 'AURA-123', status = 'PENDING', appId = 'cafe') => sql.prepare(
        "INSERT INTO orders(app_id,order_code,transaction_code,amount,status) VALUES(?,?,?,100000,?)"
    ).run(appId, code, 'QR-' + code, status);
    const send = (id, amount, content = 'Pay AURA-123', extra = {}) => app.request('/api/webhooks/sepay', {
        method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: 'Apikey test-only-key' },
        body: JSON.stringify({ id, transferAmount: amount, content, accountNumber: '123456', transferType: 'in', ...extra })
    }, env);
    const order = () => sql.prepare('SELECT * FROM orders ORDER BY id LIMIT 1').get();
    const events = () => sql.prepare('SELECT * FROM bank_transactions ORDER BY id').all();
    return { sql, env, register, send, order, events, fail(value) { fail = value; } };
}

test('exact payment succeeds; callback retains expected amount and signature', async () => {
    const f = fixture(); f.register();
    assert.equal((await f.send(1, 100000)).status, 200);
    assert.equal(f.order().status, 'SUCCESS');
    assert.equal(f.events()[0].reconciliation_status, 'MATCHED');
    assert.equal(f.events()[0].allocated_amount, 100000);
    const res = await app.request('/api/orders/AURA-123/status', {}, f.env);
    const body = await res.json();
    assert.equal(body.amount, 100000); assert.match(body.signature, /^[a-f0-9]{64}$/);
});

test('same webhook retries once; two real transfers remain two separate events', async () => {
    const f = fixture(); f.register();
    await f.send(1, 100000); const signature = f.order().signature;
    await f.send(1, 100000); assert.equal(f.events().length, 1);
    await f.send(2, 100000);
    assert.equal(f.events().length, 2);
    assert.equal(f.events()[1].reconciliation_status, 'DUPLICATE_PAYMENT');
    assert.equal(f.events()[1].excess_amount, 100000);
    assert.equal(f.events()[1].allocated_amount, 0);
    assert.equal(f.order().signature, signature);
    assert.equal(f.order().gateway_reference, 'SEPAY-1');
});

test('overpayment records actual amount and excess separately', async () => {
    const f = fixture(); f.register(); await f.send(1, 120000);
    assert.equal(f.order().status, 'SUCCESS');
    const event = f.events()[0];
    assert.equal(event.reconciliation_status, 'OVERPAID');
    assert.equal(event.received_amount, 120000); assert.equal(event.allocated_amount, 100000);
    assert.equal(event.excess_amount, 20000);
});

test('underpayments are saved without automatic aggregation', async () => {
    const f = fixture(); f.register(); await f.send(1, 60000); await f.send(2, 40000);
    assert.equal(f.order().status, 'PENDING');
    assert.deepEqual(f.events().map(e => e.reconciliation_status), ['UNDERPAID', 'UNDERPAID']);
    assert.ok(f.events().every(e => e.allocated_amount === 0));
});

test('unknown AURA code never auto-creates a successful order', async () => {
    const f = fixture(); await f.send(1, 100000, 'AURA-UNKNOWN');
    assert.equal(f.order(), undefined); assert.equal(f.events()[0].reconciliation_status, 'UNMATCHED');
});

test('prefixes and removed separators cannot match another order', async () => {
    for (const content of ['AURA-1234', 'AURA123', 'XAURA-123', 'wrong narration']) {
        const f = fixture(); f.register(); await f.send(1, 100000, content);
        assert.equal(f.order().status, 'PENDING', content);
        assert.equal(f.events()[0].reconciliation_status, 'UNMATCHED');
    }
});

test('case-insensitive exact token still matches', async () => {
    const f = fixture(); f.register(); await f.send(1, 100000, 'Thanh toan #aura-123.');
    assert.equal(f.order().status, 'SUCCESS');
});

test('multiple order codes or same code across apps require manual matching', async () => {
    for (const same of [false, true]) {
        const f = fixture(); f.register(); f.register(same ? 'AURA-123' : 'AURA-456', 'PENDING', 'other');
        await f.send(1, 100000, 'AURA-123 AURA-456');
        assert.equal(f.events()[0].reconciliation_status, 'AMBIGUOUS');
        assert.equal(f.order().status, 'PENDING');
    }
});

test('non-payable order remains unchanged and is flagged for review', async () => {
    const f = fixture(); f.register('AURA-123', 'FAILED');
    await f.send(1, 100000);
    assert.equal(f.order().status, 'FAILED');
    assert.equal(f.events()[0].reconciliation_status, 'NOT_PAYABLE');
});

test('only incoming transfers enter the payment ledger', async () => {
    const f = fixture(); f.register();
    assert.equal((await f.send(1, 100000, 'AURA-123', {transferType:'out'})).status, 200);
    assert.equal(f.events().length, 0);
    assert.equal(f.order().status, 'PENDING');
});

test('storage failure rolls back ledger and order; retry recovers', async () => {
    const f = fixture(); f.register(); f.fail(true);
    assert.equal((await f.send(1, 100000)).status, 503);
    assert.equal(f.events().length, 0); assert.equal(f.order().status, 'PENDING');
    f.fail(false); assert.equal((await f.send(1, 100000)).status, 200);
    assert.equal(f.events().length, 1); assert.equal(f.order().status, 'SUCCESS');
});

test('invalid event IDs and amounts are rejected without writes', async () => {
    for (const [id, amount] of [[0,100000],[1,0],[1,-1],[1,100.5],[1,'100000']]) {
        const f = fixture(); f.register();
        assert.equal((await f.send(id, amount)).status, 400); assert.equal(f.events().length, 0);
    }
});

test('registration replay cannot reset a paid order; changed details conflict', async () => {
    const f = fixture(); f.register(); await f.send(1, 100000);
    const register = amount => app.request('/api/orders/register', { method:'POST',
        headers: {'Content-Type':'application/json'},
        body:JSON.stringify({appId:'cafe',orderCode:'AURA-123',transactionCode:'QR-AURA-123',amount}) }, f.env);
    assert.equal((await register(100000)).status, 200); assert.equal(f.order().status, 'SUCCESS');
    assert.equal((await register(200000)).status, 409); assert.equal(f.order().amount, 100000);
});

test('webhook API key rejection leaves the ledger empty', async () => {
    const f = fixture();
    const res = await app.request('/api/webhooks/sepay', {method:'POST',body:'{}'}, f.env);
    assert.equal(res.status, 401); assert.equal(f.events().length, 0);
});

test('reconciliation feed requires server credential and paginates actual events', async () => {
    const f = fixture(); f.register(); await f.send(1, 100000); await f.send(2, 100000);
    assert.equal((await app.request('/api/reconciliation/events', {}, f.env)).status, 401);
    f.env.RECONCILIATION_API_KEY = 'server-only-test-key';
    const headers = {Authorization:'Bearer server-only-test-key'};
    const res = await app.request('/api/reconciliation/events?afterId=1', {headers}, f.env);
    const body = await res.json();
    assert.equal(body.events.length, 1); assert.equal(body.nextCursor, 2);
    assert.equal(body.events[0].reconciliation_status, 'DUPLICATE_PAYMENT');
    assert.equal((await app.request('/api/reconciliation/events?afterId=-1', {headers}, f.env)).status, 400);
});

test('replayed ID with changed amount cannot rewrite an underpayment', async () => {
    const f = fixture(); f.register(); await f.send(1, 50000); await f.send(1, 100000);
    assert.equal(f.order().status, 'PENDING');
    assert.equal(f.events().length, 1); assert.equal(f.events()[0].received_amount, 50000);
});

test('two stale order snapshots cannot both allocate payment', async () => {
    const module = await build({entryPoints:['src/services/reconciliation.ts'],bundle:true,write:false,format:'esm'});
    const {reconcile} = await import('data:text/javascript;base64,' + Buffer.from(module.outputFiles[0].contents).toString('base64'));
    const f = fixture(); f.register(); const staleOrder = f.order();
    // Two handlers may read PENDING before either batch starts. The CASE must
    // re-read status in the write transaction rather than trust that snapshot.
    for (const id of [1,2]) {
        const event = {id,gateway:'test',transactionDate:'2026-11-04',accountNumber:'123456',
            content:'AURA-123',transferType:'in',transferAmount:100000};
        await reconcile(f.env.DB, event, JSON.stringify(event), staleOrder, 'MATCH', 'test-signature');
    }
    assert.deepEqual(f.events().map(e=>e.reconciliation_status), ['MATCHED','DUPLICATE_PAYMENT']);
    assert.equal(f.events().reduce((n,e)=>n+e.allocated_amount,0),100000);
});

test('health, dashboard and history routes remain mounted after splitting modules', async () => {
    const f = fixture();
    const health = await app.request('/api/health', {}, f.env);
    assert.equal(health.status, 200); assert.equal((await health.json()).service, 'payment-gateway');
    const dashboard = await app.request('/', {}, f.env);
    assert.equal(dashboard.status, 200);
    assert.match(await dashboard.text(), /fetch\('\/api\/transactions'\)/);
    const history = await app.request('/api/transactions', {}, f.env);
    assert.deepEqual(await history.json(), {orders:[],webhooks:[]});
});

test('simulation route still requires test mode and returns signed status when enabled', async () => {
    const f = fixture(); f.register();
    assert.equal((await app.request('/api/orders/AURA-123/simulate-pay', {method:'POST'}, f.env)).status, 403);
    f.env.ALLOW_TEST_SIMULATION = 'true';
    const res = await app.request('/api/orders/AURA-123/simulate-pay', {method:'POST'}, f.env);
    assert.equal(res.status, 200); const body = await res.json();
    assert.equal(body.status, 'SUCCESS'); assert.match(body.signature, /^[a-f0-9]{64}$/);
});

test('narration with over 100 distinct tokens still matches the complete order code', async () => {
    const f = fixture(); f.register();
    const content = Array.from({length: 120}, (_, i) => `NOTE${i}`).join(' ') + ' AURA-123';
    assert.equal((await f.send(1, 100000, content)).status, 200);
    assert.equal(f.events()[0].reconciliation_status, 'MATCHED');
});
