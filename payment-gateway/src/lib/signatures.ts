export async function computeHmacSha256Hex(secret: string, data: string): Promise<string> {
    const enc = new TextEncoder();
    const key = await crypto.subtle.importKey(
        'raw',
        enc.encode(secret),
        { name: 'HMAC', hash: 'SHA-256' },
        false,
        ['sign']
    );
    const signature = await crypto.subtle.sign('HMAC', key, enc.encode(data));
    const bytes = new Uint8Array(signature);
    let hex = '';
    for (let i = 0; i < bytes.length; i++) {
        hex += bytes[i].toString(16).padStart(2, '0');
    }
    return hex;
}

export function clean(val: unknown): string {
    return val === null || val === undefined ? '' : String(val).trim();
}

/** Matches canonical format in PaymentService.java: orderCode|transactionCode|amount|status */
export function buildCanonicalPayload(orderCode: string, transactionCode: string, amount: number | string, status: string): string {
    const formattedAmount = typeof amount === 'number'
        ? (Number.isInteger(amount) ? amount.toString() : amount.toString())
        : amount;
    return `${clean(orderCode)}|${clean(transactionCode)}|${clean(formattedAmount)}|${clean(status).toUpperCase()}`;
}
