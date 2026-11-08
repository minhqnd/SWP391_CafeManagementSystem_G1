/** Bank events are immutable. A repeated webhook is not a second transfer. */
export interface BankEvent {
    id: number;
    gateway: string;
    transactionDate: string;
    accountNumber: string;
    code?: string | null;
    content: string;
    transferType: string;
    transferAmount: number;
    referenceCode?: string;
}

export interface RegisteredOrder {
    id: number;
    order_code: string;
    transaction_code: string;
    amount: number;
    status: string;
}

// Keep separators: AURA-123 must never match AURA-1234 or AURA123.
export function paymentCodes(code: string, content: string): string[] {
    return [...new Set([code.trim().replace(/^#/, '').toUpperCase(),
        ...(content.toUpperCase().match(/[A-Z0-9]+(?:[-_][A-Z0-9]+)*/g) || [])]
        .filter(Boolean))];
}

export function validBankEvent(event: BankEvent): boolean {
    return event !== null && typeof event === 'object'
        && Number.isSafeInteger(event.id) && event.id > 0
        && Number.isSafeInteger(event.transferAmount) && event.transferAmount > 0
        && typeof event.content === 'string' && typeof event.accountNumber === 'string'
        && typeof event.transferType === 'string'
        && (event.referenceCode == null || typeof event.referenceCode === 'string')
        && (event.code == null || typeof event.code === 'string');
}

/** The classification and order update run in one D1 transaction (batch).
 * CASE reads the order status inside the transaction, so racing transfers cannot
 * both pay the same order. Replayed event IDs leave the first record untouched.
 */
export async function reconcile(db: D1Database, event: BankEvent, raw: string,
                                order: RegisteredOrder | null, initialStatus: string,
                                signature: string): Promise<void> {
    const reference = event.referenceCode?.trim() || `SEPAY-${event.id}`;
    await db.batch([
        db.prepare(`INSERT INTO bank_transactions
            (sepay_id, order_id, gateway, account_number, transfer_type, received_amount,
             expected_amount, allocated_amount, excess_amount, reconciliation_status,
             content, reference_code, transaction_date, raw_webhook)
            SELECT ?, o.id, ?, ?, ?, ?, o.amount,
                CASE WHEN ? = 'MATCH' AND o.status = 'PENDING' AND ? >= o.amount THEN o.amount ELSE 0 END,
                CASE WHEN ? = 'MATCH' AND o.status = 'SUCCESS' THEN ?
                     WHEN ? = 'MATCH' AND o.status = 'PENDING' AND ? > o.amount THEN ? - o.amount ELSE 0 END,
                CASE WHEN ? != 'MATCH' THEN ?
                     WHEN o.status = 'SUCCESS' THEN 'DUPLICATE_PAYMENT'
                     WHEN o.status != 'PENDING' THEN 'NOT_PAYABLE'
                     WHEN ? < o.amount THEN 'UNDERPAID'
                     WHEN ? > o.amount THEN 'OVERPAID' ELSE 'MATCHED' END,
                ?, ?, ?, ?
            FROM (SELECT 1) seed LEFT JOIN orders o ON o.id = ?
            ON CONFLICT(sepay_id) DO NOTHING`)
            .bind(event.id, event.gateway || '', event.accountNumber, event.transferType,
                event.transferAmount, initialStatus, event.transferAmount,
                initialStatus, event.transferAmount, initialStatus, event.transferAmount, event.transferAmount,
                initialStatus, initialStatus, event.transferAmount, event.transferAmount,
                event.content, reference, event.transactionDate || '', raw, order?.id ?? null),
        db.prepare(`UPDATE orders SET status = 'SUCCESS', gateway_reference = ?, signature = ?,
                raw_webhook = ?, paid_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = 'PENDING' AND EXISTS
                (SELECT 1 FROM bank_transactions WHERE sepay_id = ? AND order_id = orders.id
                 AND reconciliation_status IN ('MATCHED', 'OVERPAID'))`)
            .bind(reference, signature, raw, order?.id ?? null, event.id)
    ]);
}
