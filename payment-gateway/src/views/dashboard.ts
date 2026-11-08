// Static gateway dashboard. Client fetches /api/transactions.
export const dashboardHtml = `
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Universal Payment Gateway Hub</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #0f172a; color: #e2e8f0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        .card-custom { background: #1e293b; border: 1px solid #334155; border-radius: 12px; }
        .badge-success { background: #10b981; color: #fff; }
        .badge-pending { background: #f59e0b; color: #fff; }
        code { color: #38bdf8; background: #0f172a; padding: 2px 6px; border-radius: 4px; }
    </style>
</head>
<body class="p-4">
    <div class="container" style="max-w: 960px;">
        <div class="d-flex justify-content-between align-items-center mb-4 pb-3 border-bottom border-secondary">
            <div>
                <h1 class="h3 fw-bold mb-1 text-light">⚡ Payment Gateway & Webhook Hub</h1>
                <p class="text-secondary small mb-0">Hệ thống Cổng thanh toán & Điều phối Webhook SePay trên Cloudflare Workers</p>
            </div>
            <span class="badge bg-success px-3 py-2">Operational (Active)</span>
        </div>

        <div class="row g-3 mb-4">
            <div class="col-md-4">
                <div class="card-custom p-3">
                    <div class="text-secondary small">SePay Webhook URL</div>
                    <code class="d-block mt-1 text-truncate">POST /api/webhooks/sepay</code>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card-custom p-3">
                    <div class="text-secondary small">Register Order API</div>
                    <code class="d-block mt-1 text-truncate">POST /api/orders/register</code>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card-custom p-3">
                    <div class="text-secondary small">Polling Status API</div>
                    <code class="d-block mt-1 text-truncate">GET /api/orders/:code/status</code>
                </div>
            </div>
        </div>

        <div class="card-custom p-4">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h2 class="h5 fw-bold mb-0">Danh sách đơn hàng gần đây (Cloudflare D1)</h2>
                <button class="btn btn-sm btn-outline-info" onclick="location.reload()">Làm mới</button>
            </div>
            <div class="table-responsive">
                <table class="table table-dark table-hover mb-0" style="background: transparent;">
                    <thead>
                        <tr class="text-secondary">
                            <th>App</th>
                            <th>Mã đơn</th>
                            <th>Số tiền</th>
                            <th>Trạng thái</th>
                            <th>Mã tham chiếu</th>
                            <th>Thời gian</th>
                        </tr>
                    </thead>
                    <tbody id="ordersBody">
                        <tr><td colspan="6" class="text-center text-secondary py-3">Đang tải dữ liệu từ D1...</td></tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <script>
        fetch('/api/transactions')
            .then(res => res.json())
            .then(data => {
                const tbody = document.getElementById('ordersBody');
                if (!data.orders || data.orders.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="6" class="text-center text-secondary py-3">Chưa có giao dịch nào</td></tr>';
                    return;
                }
                tbody.innerHTML = data.orders.map(o => \`
                    <tr>
                        <td><span class="badge bg-secondary">\${o.app_id || 'default'}</span></td>
                        <td class="fw-bold text-info">\${o.order_code}</td>
                        <td>\${Number(o.amount).toLocaleString('vi-VN')} đ</td>
                        <td>
                            <span class="badge \${o.status === 'SUCCESS' ? 'badge-success' : 'badge-pending'}">
                                \${o.status}
                            </span>
                        </td>
                        <td><small class="text-secondary">\${o.gateway_reference || '-'}</small></td>
                        <td><small class="text-secondary">\${o.created_at}</small></td>
                    </tr>
                \`).join('');
            })
            .catch(() => {
                document.getElementById('ordersBody').innerHTML = '<tr><td colspan="6" class="text-center text-danger py-3">Lỗi tải dữ liệu</td></tr>';
            });
    </script>
</body>
</html>
    `;
