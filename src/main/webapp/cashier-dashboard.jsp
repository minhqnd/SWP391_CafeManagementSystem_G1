<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:if test="${empty sessionScope.user}">
    <c:redirect url="/?login=true" />
</c:if>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Aura Café - Thu Ngân POS</title>
    <!-- Fonts & Icons -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    
    <style>
        :root {
            --aura-primary: #900b1a;
            --aura-primary-hover: #750915;
            --aura-primary-light: #fff1f2;
            --aura-primary-border: #fecdd3;
            --slate-50: #f8fafc;
            --slate-100: #f1f5f9;
            --slate-200: #e2e8f0;
            --slate-300: #cbd5e1;
            --slate-400: #94a3b8;
            --slate-600: #475569;
            --slate-700: #334155;
            --slate-800: #1e293b;
            --slate-900: #0f172a;
            --success-color: #059669;
            --success-bg: #ecfdf5;
            --warning-color: #d97706;
            --warning-bg: #fffbeb;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
            background-color: var(--slate-100);
            color: var(--slate-800);
            font-size: 14px;
            height: 100vh;
            overflow: hidden;
            display: flex;
            flex-direction: column;
        }

        .material-symbols-outlined {
            font-size: 20px;
            line-height: 1;
            vertical-align: middle;
        }

        /* Top Header */
        .pos-header {
            height: 56px;
            background: #ffffff;
            border-bottom: 1px solid var(--slate-200);
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 16px;
            flex-shrink: 0;
            z-index: 1000;
        }

        .brand-area {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .brand-title {
            font-size: 17px;
            font-weight: 800;
            letter-spacing: -0.3px;
            color: var(--aura-primary);
            text-decoration: none;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .pos-badge {
            background-color: var(--aura-primary-light);
            color: var(--aura-primary);
            font-size: 11px;
            font-weight: 700;
            padding: 3px 8px;
            border-radius: 6px;
            border: 1px solid var(--aura-primary-border);
            text-transform: uppercase;
        }

        .nav-tabs-pos {
            display: flex;
            gap: 4px;
            background: var(--slate-100);
            padding: 3px;
            border-radius: 8px;
            border: 1px solid var(--slate-200);
        }

        .nav-tab-btn {
            border: none;
            background: transparent;
            padding: 6px 14px;
            border-radius: 6px;
            font-weight: 600;
            font-size: 13px;
            color: var(--slate-600);
            display: flex;
            align-items: center;
            gap: 6px;
            cursor: pointer;
            transition: all 0.15s ease;
        }

        .nav-tab-btn:hover {
            color: var(--slate-900);
        }

        .nav-tab-btn.active {
            background: #ffffff;
            color: var(--aura-primary);
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
        }

        .header-meta {
            display: flex;
            align-items: center;
            gap: 16px;
        }

        .shift-info {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 13px;
            color: var(--slate-600);
        }

        .live-clock {
            font-weight: 700;
            color: var(--slate-800);
            font-variant-numeric: tabular-nums;
        }

        .user-pill {
            display: flex;
            align-items: center;
            gap: 6px;
            background: var(--slate-50);
            padding: 5px 12px;
            border-radius: 20px;
            border: 1px solid var(--slate-200);
            font-weight: 600;
            font-size: 13px;
        }

        .btn-link-action {
            display: flex;
            align-items: center;
            gap: 4px;
            color: var(--slate-600);
            text-decoration: none;
            font-weight: 600;
            font-size: 13px;
            padding: 6px 10px;
            border-radius: 6px;
            transition: background 0.15s;
        }

        .btn-link-action:hover {
            background: var(--slate-100);
            color: var(--aura-primary);
        }

        /* Main Workspace */
        .pos-workspace {
            display: flex;
            height: calc(100vh - 56px);
            overflow: hidden;
        }

        /* Left Side: Catalog */
        .catalog-panel {
            flex: 1;
            display: flex;
            flex-direction: column;
            background: var(--slate-50);
            border-right: 1px solid var(--slate-200);
            overflow: hidden;
        }

        .catalog-toolbar {
            padding: 12px 16px;
            background: #ffffff;
            border-bottom: 1px solid var(--slate-200);
            display: flex;
            flex-direction: column;
            gap: 10px;
            flex-shrink: 0;
        }

        .search-row {
            display: flex;
            gap: 10px;
        }

        .search-box-wrapper {
            position: relative;
            flex: 1;
        }

        .search-box-wrapper .material-symbols-outlined {
            position: absolute;
            left: 12px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--slate-400);
        }

        .search-input {
            width: 100%;
            padding: 9px 12px 9px 38px;
            border-radius: 8px;
            border: 1px solid var(--slate-200);
            background: var(--slate-50);
            font-size: 13.5px;
            transition: all 0.2s;
        }

        .search-input:focus {
            outline: none;
            border-color: var(--aura-primary);
            background: #ffffff;
            box-shadow: 0 0 0 3px rgba(144, 11, 26, 0.1);
        }

        .category-scroller {
            display: flex;
            gap: 8px;
            overflow-x: auto;
            padding-bottom: 4px;
            scrollbar-width: thin;
        }

        .category-scroller::-webkit-scrollbar {
            height: 4px;
        }

        .category-scroller::-webkit-scrollbar-thumb {
            background: var(--slate-300);
            border-radius: 4px;
        }

        .category-chip {
            white-space: nowrap;
            padding: 6px 14px;
            border-radius: 20px;
            background: var(--slate-100);
            border: 1px solid var(--slate-200);
            color: var(--slate-700);
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.15s;
            user-select: none;
        }

        .category-chip:hover {
            background: var(--slate-200);
        }

        .category-chip.active {
            background: var(--aura-primary);
            color: #ffffff;
            border-color: var(--aura-primary);
        }

        .products-grid-container {
            flex: 1;
            padding: 16px;
            overflow-y: auto;
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
            grid-auto-rows: max-content;
            gap: 14px;
        }

        .product-card {
            background: #ffffff;
            border: 1px solid var(--slate-200);
            border-radius: 10px;
            padding: 10px;
            cursor: pointer;
            transition: transform 0.15s, box-shadow 0.15s, border-color 0.15s;
            display: flex;
            flex-direction: column;
            user-select: none;
            position: relative;
        }

        .product-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 12px rgba(0, 0, 0, 0.05);
            border-color: var(--aura-primary);
        }

        .product-img-box {
            position: relative;
            width: 100%;
            height: 110px;
            border-radius: 6px;
            overflow: hidden;
            background: var(--slate-100);
            margin-bottom: 8px;
        }

        .product-img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        .product-tag {
            position: absolute;
            top: 6px;
            left: 6px;
            background: rgba(15, 23, 42, 0.75);
            color: #ffffff;
            font-size: 10px;
            font-weight: 700;
            padding: 2px 6px;
            border-radius: 4px;
            backdrop-filter: blur(4px);
        }

        .product-name {
            font-size: 13.5px;
            font-weight: 700;
            color: var(--slate-900);
            line-height: 1.3;
            margin-bottom: 4px;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            min-height: 35px;
        }

        .product-category-sub {
            font-size: 11.5px;
            color: var(--slate-400);
            margin-bottom: 6px;
        }

        .product-price-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-top: auto;
            padding-top: 4px;
        }

        .product-price {
            font-weight: 800;
            color: var(--aura-primary);
            font-size: 14.5px;
        }

        .btn-add-quick {
            width: 28px;
            height: 28px;
            border-radius: 6px;
            background: var(--aura-primary-light);
            color: var(--aura-primary);
            border: 1px solid var(--aura-primary-border);
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.15s;
        }

        .product-card:hover .btn-add-quick {
            background: var(--aura-primary);
            color: #ffffff;
        }

        /* Right Side: Order Ticket / Cart */
        .cart-panel {
            width: 380px;
            background: #ffffff;
            display: flex;
            flex-direction: column;
            flex-shrink: 0;
            overflow: hidden;
        }

        .cart-header {
            padding: 12px 16px;
            border-bottom: 1px solid var(--slate-200);
            display: flex;
            flex-direction: column;
            gap: 10px;
            background: #ffffff;
        }

        .order-type-switch {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 6px;
            background: var(--slate-100);
            padding: 3px;
            border-radius: 8px;
        }

        .order-type-btn {
            border: none;
            background: transparent;
            padding: 7px;
            font-size: 12.5px;
            font-weight: 700;
            color: var(--slate-600);
            border-radius: 6px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            transition: all 0.15s;
        }

        .order-type-btn.active {
            background: #ffffff;
            color: var(--aura-primary);
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
        }

        .table-select-row {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .table-select-label {
            font-size: 12.5px;
            font-weight: 600;
            color: var(--slate-600);
            white-space: nowrap;
        }

        .table-select-box {
            flex: 1;
            padding: 6px 10px;
            border-radius: 6px;
            border: 1px solid var(--slate-200);
            font-size: 13px;
            font-weight: 600;
            color: var(--slate-800);
            background: var(--slate-50);
        }

        .table-select-box:focus {
            outline: none;
            border-color: var(--aura-primary);
        }

        /* Customer Search Row */
        .customer-loyalty-section {
            background: var(--slate-50);
            border: 1px dashed var(--slate-300);
            border-radius: 8px;
            padding: 8px 10px;
            display: flex;
            flex-direction: column;
            gap: 6px;
        }

        .cust-input-group {
            display: flex;
            gap: 6px;
        }

        .cust-input {
            flex: 1;
            padding: 5px 8px;
            font-size: 12.5px;
            border-radius: 6px;
            border: 1px solid var(--slate-200);
            background: #ffffff;
        }

        .cust-btn-search {
            background: var(--slate-800);
            color: #ffffff;
            border: none;
            border-radius: 6px;
            padding: 0 10px;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
        }

        .cust-btn-search:hover {
            background: var(--slate-900);
        }

        .cust-info-badge {
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 12px;
            background: #ffffff;
            padding: 4px 8px;
            border-radius: 4px;
            border: 1px solid var(--slate-200);
        }

        .cust-points {
            color: var(--aura-primary);
            font-weight: 700;
        }

        .cust-remove-btn {
            background: none;
            border: none;
            color: var(--slate-400);
            cursor: pointer;
            padding: 0;
            line-height: 1;
        }

        .cust-remove-btn:hover {
            color: var(--aura-primary);
        }

        .points-redeem-row {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 12px;
            color: var(--slate-700);
        }

        /* Cart Items List */
        .cart-items-container {
            flex: 1;
            overflow-y: auto;
            padding: 10px 14px;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .empty-cart-state {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            height: 100%;
            color: var(--slate-400);
            gap: 8px;
            text-align: center;
            padding: 20px;
        }

        .empty-cart-icon {
            font-size: 48px;
            color: var(--slate-300);
        }

        .cart-item-card {
            background: var(--slate-50);
            border: 1px solid var(--slate-200);
            border-radius: 8px;
            padding: 8px 10px;
            display: flex;
            flex-direction: column;
            gap: 6px;
            transition: border-color 0.15s;
        }

        .cart-item-card:hover {
            border-color: var(--slate-300);
        }

        .cart-item-main {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
        }

        .cart-item-title {
            font-weight: 700;
            font-size: 13.5px;
            color: var(--slate-900);
        }

        .cart-item-price {
            font-weight: 700;
            font-size: 13.5px;
            color: var(--slate-900);
            white-space: nowrap;
        }

        .cart-item-customization {
            font-size: 11.5px;
            color: var(--slate-600);
            background: #ffffff;
            padding: 2px 6px;
            border-radius: 4px;
            border: 1px solid var(--slate-200);
            width: fit-content;
        }

        .cart-item-controls {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding-top: 4px;
            border-top: 1px dashed var(--slate-200);
        }

        .qty-stepper {
            display: flex;
            align-items: center;
            background: #ffffff;
            border: 1px solid var(--slate-200);
            border-radius: 6px;
            overflow: hidden;
        }

        .btn-stepper {
            width: 26px;
            height: 24px;
            background: transparent;
            border: none;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            color: var(--slate-600);
        }

        .btn-stepper:hover {
            background: var(--slate-100);
            color: var(--slate-900);
        }

        .qty-input {
            width: 32px;
            text-align: center;
            font-weight: 700;
            font-size: 12.5px;
            border: none;
            outline: none;
            background: transparent;
        }

        .btn-remove-item {
            border: none;
            background: transparent;
            color: var(--slate-400);
            cursor: pointer;
            padding: 3px;
            border-radius: 4px;
            transition: color 0.15s;
        }

        .btn-remove-item:hover {
            color: #e11d48;
        }

        /* Order Note */
        .cart-note-box {
            padding: 0 14px 8px 14px;
        }

        .cart-note-input {
            width: 100%;
            padding: 6px 10px;
            font-size: 12px;
            border: 1px solid var(--slate-200);
            border-radius: 6px;
            background: var(--slate-50);
        }

        .cart-note-input:focus {
            outline: none;
            border-color: var(--aura-primary);
            background: #ffffff;
        }

        /* Cart Summary & Actions */
        .cart-footer {
            border-top: 1px solid var(--slate-200);
            padding: 12px 14px;
            background: #ffffff;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .summary-row {
            display: flex;
            justify-content: space-between;
            font-size: 12.5px;
            color: var(--slate-600);
        }

        .summary-row.total-row {
            font-size: 15px;
            font-weight: 800;
            color: var(--slate-900);
            border-top: 1px solid var(--slate-200);
            padding-top: 8px;
            margin-top: 2px;
        }

        .summary-total-price {
            color: var(--aura-primary);
            font-size: 18px;
        }

        .cart-action-buttons {
            display: grid;
            grid-template-columns: 1fr 2fr;
            gap: 8px;
        }

        .btn-reset-cart {
            border: 1px solid var(--slate-300);
            background: #ffffff;
            color: var(--slate-700);
            font-weight: 600;
            padding: 11px;
            border-radius: 8px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 4px;
            font-size: 13px;
            transition: all 0.15s;
        }

        .btn-reset-cart:hover {
            background: var(--slate-100);
        }

        .btn-checkout-primary {
            background: var(--aura-primary);
            border: 1px solid var(--aura-primary);
            color: #ffffff;
            font-weight: 700;
            padding: 11px;
            border-radius: 8px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            font-size: 14px;
            transition: all 0.15s;
        }

        .btn-checkout-primary:hover {
            background: var(--aura-primary-hover);
        }

        .btn-checkout-primary:disabled {
            background: var(--slate-300);
            border-color: var(--slate-300);
            cursor: not-allowed;
        }

        /* Tab 2: Orders History View */
        .orders-panel {
            flex: 1;
            background: #ffffff;
            display: none;
            flex-direction: column;
            overflow: hidden;
        }

        .orders-panel.active {
            display: flex;
        }

        .orders-toolbar {
            padding: 14px 20px;
            border-bottom: 1px solid var(--slate-200);
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: var(--slate-50);
        }

        .orders-table-wrapper {
            flex: 1;
            overflow-y: auto;
            padding: 16px 20px;
        }

        .pos-table {
            width: 100%;
            border-collapse: separate;
            border-spacing: 0;
            font-size: 13.5px;
        }

        .pos-table th {
            background: var(--slate-100);
            color: var(--slate-700);
            font-weight: 700;
            padding: 10px 14px;
            border-bottom: 1px solid var(--slate-200);
            text-align: left;
            position: sticky;
            top: 0;
            z-index: 10;
        }

        .pos-table td {
            padding: 12px 14px;
            border-bottom: 1px solid var(--slate-200);
            vertical-align: middle;
        }

        .pos-table tr:hover td {
            background: var(--slate-50);
        }

        /* Status Badges */
        .badge-status {
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 11.5px;
            font-weight: 700;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }

        .badge-status.confirmed {
            background: #ecfdf5;
            color: #065f46;
            border: 1px solid #a7f3d0;
        }

        .badge-status.preparing {
            background: #fffbeb;
            color: #92400e;
            border: 1px solid #fde68a;
        }

        .badge-status.ready {
            background: #eff6ff;
            color: #1e40af;
            border: 1px solid #bfdbfe;
        }

        .badge-status.completed {
            background: #f1f5f9;
            color: #334155;
            border: 1px solid #cbd5e1;
        }

        .badge-status.cancelled {
            background: #fff1f2;
            color: #9f1239;
            border: 1px solid #fecdd3;
        }

        /* Modal Styles */
        .modal-content {
            border: none;
            border-radius: 12px;
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
        }

        .modal-header {
            padding: 14px 18px;
            border-bottom: 1px solid var(--slate-200);
        }

        .modal-title {
            font-size: 16px;
            font-weight: 700;
            color: var(--slate-900);
        }

        .modal-body {
            padding: 18px;
        }

        .modal-footer {
            padding: 12px 18px;
            border-top: 1px solid var(--slate-200);
        }

        /* Option Group in Customization Modal */
        .opt-group-title {
            font-size: 12.5px;
            font-weight: 700;
            color: var(--slate-700);
            margin-bottom: 8px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .opt-chips-grid {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-bottom: 16px;
        }

        .opt-radio-label {
            cursor: pointer;
            padding: 6px 14px;
            background: var(--slate-100);
            border: 1px solid var(--slate-200);
            border-radius: 6px;
            font-size: 13px;
            font-weight: 600;
            color: var(--slate-700);
            transition: all 0.15s;
        }

        input[type="radio"]:checked + .opt-radio-label,
        input[type="checkbox"]:checked + .opt-radio-label {
            background: var(--aura-primary);
            border-color: var(--aura-primary);
            color: #ffffff;
        }

        /* Payment Modal Specifics */
        .pay-total-banner {
            background: var(--slate-900);
            color: #ffffff;
            border-radius: 8px;
            padding: 14px 18px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 18px;
        }

        .pay-total-value {
            font-size: 22px;
            font-weight: 800;
            color: #fde047;
        }

        .pay-method-tabs {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px;
            margin-bottom: 16px;
        }

        .pay-method-btn {
            padding: 10px;
            border-radius: 8px;
            border: 2px solid var(--slate-200);
            background: #ffffff;
            font-weight: 700;
            font-size: 13.5px;
            color: var(--slate-700);
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            transition: all 0.15s;
        }

        .pay-method-btn.active {
            border-color: var(--aura-primary);
            color: var(--aura-primary);
            background: var(--aura-primary-light);
        }

        .quick-cash-row {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 6px;
            margin-top: 8px;
        }

        .btn-quick-cash {
            border: 1px solid var(--slate-200);
            background: var(--slate-50);
            padding: 7px;
            border-radius: 6px;
            font-size: 12.5px;
            font-weight: 700;
            cursor: pointer;
            transition: all 0.15s;
        }

        .btn-quick-cash:hover {
            background: var(--slate-200);
        }

        /* Toast Container */
        #toastContainer {
            position: fixed;
            bottom: 20px;
            right: 20px;
            z-index: 2000;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .pos-toast {
            background: var(--slate-900);
            color: #ffffff;
            padding: 10px 16px;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.2);
            font-size: 13.5px;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 8px;
            animation: slideIn 0.2s ease forwards;
        }

        @keyframes slideIn {
            from { transform: translateY(20px); opacity: 0; }
            to { transform: translateY(0); opacity: 1; }
        }
    </style>
</head>
<body>

    <!-- Top Header -->
    <header class="pos-header">
        <div class="brand-area">
            <a href="${pageContext.request.contextPath}/cashier" class="brand-title">
                <span class="material-symbols-outlined">coffee</span>
                <span>AURA CAFÉ</span>
            </a>
            <span class="pos-badge">POS THU NGÂN</span>
        </div>

        <!-- Center Tabs Switcher -->
        <div class="nav-tabs-pos">
            <button class="nav-tab-btn active" id="tabBtnPos" onclick="switchMainTab('pos')">
                <span class="material-symbols-outlined">point_of_sale</span>
                <span>Tạo đơn tại quầy</span>
            </button>
            <button class="nav-tab-btn" id="tabBtnOrders" onclick="switchMainTab('orders')">
                <span class="material-symbols-outlined">receipt_long</span>
                <span>Đơn hôm nay (<span id="todayOrderCount">${counterOrders.size()}</span>)</span>
            </button>
        </div>

        <!-- Right User Actions -->
        <div class="header-meta">
            <div class="shift-info">
                <span class="material-symbols-outlined text-muted">schedule</span>
                <span class="live-clock" id="clockDisplay">${currentTime}</span>
                <span class="text-muted">|</span>
                <span>${currentDate}</span>
            </div>
            <div class="user-pill">
                <span class="material-symbols-outlined text-muted">account_circle</span>
                <span>${sessionScope.user.fullName}</span>
            </div>
            <a href="${pageContext.request.contextPath}/barista" target="_blank" class="btn-link-action" title="Mở màn hình Pha chế Barista">
                <span class="material-symbols-outlined">local_cafe</span>
                <span>Màn KDS</span>
            </a>
            <a href="${pageContext.request.contextPath}/login?action=logout" class="btn-link-action" title="Đăng xuất khỏi hệ thống">
                <span class="material-symbols-outlined">logout</span>
                <span>Đăng xuất</span>
            </a>
        </div>
    </header>

    <!-- Main Workspace -->
    <div class="pos-workspace">
        
        <!-- ================= VIEW 1: POS TERMINAL ================= -->
        <div class="catalog-panel" id="viewPosCatalog">
            <!-- Search & Categories -->
            <div class="catalog-toolbar">
                <div class="search-row">
                    <div class="search-box-wrapper">
                        <span class="material-symbols-outlined">search</span>
                        <input type="text" id="searchInput" class="search-input" placeholder="Tìm theo tên món hoặc mã SKU..." oninput="handleSearch(this.value)">
                    </div>
                </div>
                
                <div class="category-scroller">
                    <button class="category-chip active" data-cat-id="all" onclick="filterCategory('all', this)">
                        Tất cả (${products.size()})
                    </button>
                    <c:forEach var="cat" items="${categories}">
                        <button class="category-chip" data-cat-id="${cat.id}" onclick="filterCategory('${cat.id}', this)">
                            ${cat.name} (${cat.productCount})
                        </button>
                    </c:forEach>
                </div>
            </div>

            <!-- Products Grid -->
            <div class="products-grid-container" id="productsGrid">
                <c:forEach var="p" items="${products}">
                    <div class="product-card" 
                         data-id="${p.id}" 
                         data-name="<c:out value="${p.name}"/>" 
                         data-price="${p.basePrice}" 
                         data-cat-id="${p.categoryId}"
                         data-image="${p.imageUrl}"
                         onclick="openProductCustomization(${p.id}, '<c:out value="${p.name}"/>', ${p.basePrice}, '${p.imageUrl}')">
                        <div class="product-img-box">
                            <img src="${p.imageUrl}" alt="${p.name}" class="product-img" onerror="this.onerror=null; this.src='https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=300&q=80';">
                            <c:if test="${not empty p.badge}">
                                <span class="product-tag">${p.badge}</span>
                            </c:if>
                        </div>
                        <div class="product-name" title="${p.name}">${p.name}</div>
                        <div class="product-category-sub">${p.categoryName}</div>
                        <div class="product-price-row">
                            <span class="product-price">${p.formattedPrice}</span>
                            <div class="btn-add-quick" title="Thêm món">
                                <span class="material-symbols-outlined">add</span>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>

        <!-- Right Side: Order Ticket / Cart -->
        <div class="cart-panel" id="viewPosCart">
            <div class="cart-header">
                <!-- Order Type: Dine-in vs Takeaway -->
                <div class="order-type-switch">
                    <button class="order-type-btn active" id="btnTypeDineIn" onclick="setOrderType('DINE_IN')">
                        <span class="material-symbols-outlined">table_restaurant</span>
                        <span>Tại quán</span>
                    </button>
                    <button class="order-type-btn" id="btnTypeTakeaway" onclick="setOrderType('TAKEAWAY')">
                        <span class="material-symbols-outlined">shopping_bag</span>
                        <span>Mang về</span>
                    </button>
                </div>

                <!-- Customer Search / Loyalty -->
                <div class="customer-loyalty-section">
                    <div class="cust-input-group" id="custSearchBox">
                        <input type="text" id="custPhoneInput" class="cust-input" placeholder="Nhập SĐT khách hội viên..." onkeydown="if(event.key==='Enter') searchCustomer()">
                        <button class="cust-btn-search" onclick="searchCustomer()">Tìm</button>
                    </div>
                    <div id="custInfoBox" style="display: none;">
                        <div class="cust-info-badge">
                            <div>
                                <span class="fw-bold" id="custNameDisplay">Nguyễn Văn A</span>
                                <span class="badge bg-secondary ms-1" id="custTierDisplay">GOLD</span>
                            </div>
                            <button class="cust-remove-btn" onclick="clearCustomer()" title="Xóa thông tin khách">
                                <span class="material-symbols-outlined">close</span>
                            </button>
                        </div>
                        <div class="points-redeem-row mt-1">
                            <input type="checkbox" id="chkUsePoints" onchange="toggleUsePoints()">
                            <label for="chkUsePoints" class="cursor-pointer">
                                Dùng điểm: <span class="cust-points" id="custPointsDisplay">0</span> pts (-<span id="custDiscountDisplay">0đ</span>)
                            </label>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Cart Items Container -->
            <div class="cart-items-container" id="cartItemsList">
                <div class="empty-cart-state" id="emptyCartMessage">
                    <span class="material-symbols-outlined empty-cart-icon">shopping_cart</span>
                    <div>Chưa có món nào trong đơn</div>
                    <div class="small">Vui lòng chọn món từ menu bên trái</div>
                </div>
            </div>

            <!-- Order Note -->
            <div class="cart-note-box">
                <input type="text" id="orderNoteInput" class="cart-note-input" placeholder="Ghi chú chung cho toàn bộ đơn (nếu có)...">
            </div>

            <!-- Cart Footer / Summary -->
            <div class="cart-footer">
                <div class="summary-row">
                    <span>Tạm tính (<span id="cartCountTotal">0</span> món)</span>
                    <span id="lblSubtotal">0đ</span>
                </div>
                <div class="summary-row" id="rowDiscount" style="display: none;">
                    <span>Giảm giá hội viên</span>
                    <span class="text-danger" id="lblDiscount">-0đ</span>
                </div>
                <div class="summary-row">
                    <span>Thuế VAT (8%)</span>
                    <span id="lblVat">0đ</span>
                </div>
                <div class="summary-row total-row">
                    <span>Tổng thanh toán</span>
                    <span class="summary-total-price" id="lblFinalTotal">0đ</span>
                </div>

                <div class="cart-action-buttons">
                    <button class="btn-reset-cart" onclick="resetCart()">
                        <span class="material-symbols-outlined">refresh</span>
                        <span>Làm mới</span>
                    </button>
                    <button class="btn-checkout-primary" id="btnCheckout" onclick="openPaymentModal()" disabled>
                        <span class="material-symbols-outlined">payments</span>
                        <span>Thanh toán</span>
                    </button>
                </div>
            </div>
        </div>

        <!-- ================= VIEW 2: TODAY'S ORDERS HISTORY ================= -->
        <div class="orders-panel" id="viewOrdersList">
            <div class="orders-toolbar">
                <div class="d-flex align-items-center gap-2">
                    <span class="material-symbols-outlined text-primary">history</span>
                    <span class="fw-bold fs-6">Danh sách đơn tại quầy hôm nay</span>
                </div>
                <button class="btn btn-sm btn-outline-secondary d-flex align-items-center gap-1" onclick="window.location.reload()">
                    <span class="material-symbols-outlined">refresh</span>
                    <span>Tải lại dữ liệu</span>
                </button>
            </div>
            <div class="orders-table-wrapper">
                <table class="pos-table">
                    <thead>
                        <tr>
                            <th>Mã đơn</th>
                            <th>Thời gian</th>
                            <th>Loại đơn</th>
                            <th>Khách hàng</th>
                            <th>Món đã gọi</th>
                            <th>Tổng tiền</th>
                            <th>Hình thức</th>
                            <th>Trạng thái</th>
                            <th>Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${not empty counterOrders}">
                                <c:forEach var="o" items="${counterOrders}">
                                <tr>
                                    <td class="fw-bold">${o.orderCode}</td>
                                    <td>${o.createdTimeText}</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${o.orderType eq 'TAKEAWAY'}">
                                                <span class="badge bg-light text-dark border">Mang về</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-light text-dark border">Tại quán</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <span>${empty o.recipientName ? 'Khách tại quầy' : o.recipientName}</span>
                                    </td>
                                    <td style="max-width: 250px;" class="text-truncate" title="${o.itemSummary}">
                                        ${o.itemSummary}
                                    </td>
                                    <td class="fw-bold text-danger">${o.formattedFinalAmount}</td>
                                    <td>${o.paymentMethodLabel}</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${o.status eq 'CONFIRMED'}">
                                                <span class="badge-status confirmed">
                                                    <span class="material-symbols-outlined">check_circle</span>
                                                    <span>Đã thanh toán</span>
                                                </span>
                                            </c:when>
                                            <c:when test="${o.status eq 'PREPARING'}">
                                                <span class="badge-status preparing">
                                                    <span class="material-symbols-outlined">sync</span>
                                                    <span>Đang pha chế</span>
                                                </span>
                                            </c:when>
                                            <c:when test="${o.status eq 'READY'}">
                                                <span class="badge-status ready">
                                                    <span class="material-symbols-outlined">notifications_active</span>
                                                    <span>Sẵn sàng</span>
                                                </span>
                                            </c:when>
                                            <c:when test="${o.status eq 'COMPLETED'}">
                                                <span class="badge-status completed">
                                                    <span class="material-symbols-outlined">done_all</span>
                                                    <span>Hoàn thành</span>
                                                </span>
                                            </c:when>
                                            <c:when test="${o.status eq 'CANCELLED'}">
                                                <span class="badge-status cancelled">
                                                    <span class="material-symbols-outlined">cancel</span>
                                                    <span>Đã hủy</span>
                                                </span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-secondary">${o.statusLabel}</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <div class="d-flex align-items-center gap-1">
                                            <a href="${pageContext.request.contextPath}/bill?orderId=${o.id}" target="_blank" class="btn btn-sm btn-outline-primary py-1 px-2" title="Xem & In hóa đơn">
                                                <span class="material-symbols-outlined" style="font-size: 16px;">receipt</span>
                                            </a>
                                            <c:if test="${o.status ne 'COMPLETED' && o.status ne 'CANCELLED'}">
                                                <button class="btn btn-sm btn-outline-danger py-1 px-2" onclick="cancelOrder(${o.id})" title="Hủy đơn này">
                                                    <span class="material-symbols-outlined" style="font-size: 16px;">close</span>
                                                </button>
                                            </c:if>
                                        </div>
                                    </td>
                                </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td colspan="9" class="text-center py-4 text-muted">
                                        Chưa có đơn hàng nào được ghi nhận tại quầy trong hôm nay.
                                    </td>
                                </tr>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>

    </div>

    <!-- MODAL 1: PRODUCT CUSTOMIZATION -->
    <div class="modal fade" id="customizationModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header">
                    <div>
                        <div class="modal-title" id="custModalItemName">Tên món</div>
                        <div class="text-muted small" id="custModalBasePrice">0đ</div>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <!-- Size -->
                    <div class="opt-group-title">Chọn Size</div>
                    <div class="opt-chips-grid">
                        <label>
                            <input type="radio" name="optSize" value="M" checked class="d-none">
                            <span class="opt-radio-label">Size M (Tiêu chuẩn)</span>
                        </label>
                        <label>
                            <input type="radio" name="optSize" value="L" class="d-none">
                            <span class="opt-radio-label">Size L (+8.000đ)</span>
                        </label>
                    </div>

                    <!-- Sugar & Ice -->
                    <div class="row">
                        <div class="col-6">
                            <div class="opt-group-title">Mức đường</div>
                            <select id="optSugar" class="form-select form-select-sm">
                                <option value="100% đường">100% Đường (Chuẩn)</option>
                                <option value="70% đường">70% Đường</option>
                                <option value="50% đường">50% Đường</option>
                                <option value="30% đường">30% Đường</option>
                                <option value="Không đường">Không đường</option>
                            </select>
                        </div>
                        <div class="col-6">
                            <div class="opt-group-title">Mức đá</div>
                            <select id="optIce" class="form-select form-select-sm">
                                <option value="100% đá">100% Đá (Chuẩn)</option>
                                <option value="70% đá">70% Đá</option>
                                <option value="50% đá">50% Đá</option>
                                <option value="Ít đá">Ít đá</option>
                                <option value="Không đá">Không đá</option>
                                <option value="Nóng">Uống Nóng</option>
                            </select>
                        </div>
                    </div>

                    <!-- Toppings -->
                    <div class="opt-group-title mt-3">Topping thêm (+10.000đ)</div>
                    <div class="opt-chips-grid">
                        <label>
                            <input type="checkbox" name="optTopping" value="Trân châu đen" class="d-none">
                            <span class="opt-radio-label">Trân châu đen</span>
                        </label>
                        <label>
                            <input type="checkbox" name="optTopping" value="Trân châu trắng" class="d-none">
                            <span class="opt-radio-label">Trân châu trắng</span>
                        </label>
                        <label>
                            <input type="checkbox" name="optTopping" value="Kem cheese" class="d-none">
                            <span class="opt-radio-label">Kem Cheese Macchiato</span>
                        </label>
                        <label>
                            <input type="checkbox" name="optTopping" value="Thạch sương sáo" class="d-none">
                            <span class="opt-radio-label">Thạch sương sáo</span>
                        </label>
                    </div>

                    <!-- Custom Note -->
                    <div class="opt-group-title mt-2">Ghi chú riêng cho món này</div>
                    <input type="text" id="custModalNote" class="form-control form-control-sm" placeholder="Ví dụ: Mang ly cá nhân, sữa ít béo...">
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                    <button type="button" class="btn btn-primary" style="background-color: var(--aura-primary); border-color: var(--aura-primary);" onclick="confirmAddToCart()">
                        Thêm vào đơn (<span id="custModalFinalPrice">0đ</span>)
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- MODAL 2: PAYMENT & CHECKOUT -->
    <div class="modal fade" id="paymentModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header">
                    <div class="modal-title d-flex align-items-center gap-2">
                        <span class="material-symbols-outlined text-success">verified</span>
                        <span>Xác nhận thanh toán tại quầy</span>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <!-- Banner Total -->
                    <div class="pay-total-banner">
                        <div>
                            <div class="small text-white-50">TỔNG TIỀN PHẢI THU</div>
                            <div class="pay-total-value" id="payModalTotalDisplay">0đ</div>
                        </div>
                        <div class="text-end small text-white-50" id="payModalSummaryDesc">
                            Tại quán | Bàn 01
                        </div>
                    </div>

                    <!-- Method Tabs -->
                    <div class="pay-method-tabs">
                        <button type="button" class="pay-method-btn active" id="btnMethodCash" onclick="selectPaymentMethod('CASH')">
                            <span class="material-symbols-outlined">payments</span>
                            <span>Tiền mặt</span>
                        </button>
                        <button type="button" class="pay-method-btn" id="btnMethodQR" onclick="selectPaymentMethod('BANK_TRANSFER')">
                            <span class="material-symbols-outlined">qr_code_2</span>
                            <span>VietQR Quét mã</span>
                        </button>
                    </div>

                    <!-- Tab CASH -->
                    <div id="panelCashPayment">
                        <label class="form-label small fw-bold text-secondary">Khách đưa (VNĐ):</label>
                        <input type="number" id="inputCashGiven" class="form-control form-control-lg fw-bold text-success" placeholder="0" oninput="calculateChange()">
                        
                        <div class="quick-cash-row">
                            <button type="button" class="btn-quick-cash" onclick="addQuickCash(0)">Đúng số tiền</button>
                            <button type="button" class="btn-quick-cash" onclick="setCashAmount(50000)">50.000đ</button>
                            <button type="button" class="btn-quick-cash" onclick="setCashAmount(100000)">100.000đ</button>
                            <button type="button" class="btn-quick-cash" onclick="setCashAmount(200000)">200.000đ</button>
                            <button type="button" class="btn-quick-cash" onclick="setCashAmount(500000)">500.000đ</button>
                            <button type="button" class="btn-quick-cash" onclick="setCashAmount(1000000)">1.000.000đ</button>
                        </div>

                        <div class="d-flex justify-content-between align-items-center mt-3 p-2 bg-light rounded border">
                            <span class="fw-bold">Tiền thừa trả khách:</span>
                            <span class="fw-bold fs-5 text-danger" id="lblCashChange">0đ</span>
                        </div>
                    </div>

                    <!-- Tab VIETQR -->
                    <div id="panelQRPayment" style="display: none;" class="text-center py-2">
                        <div class="mb-2 text-muted small">Quét mã thanh toán qua tài khoản cửa hàng Aura Café:</div>
                        <div class="d-inline-block p-2 bg-white rounded border shadow-sm">
                            <img id="vietQrImg" src="" alt="VietQR" style="width: 200px; height: 200px; object-fit: contain;">
                        </div>
                        <div class="mt-2 small text-secondary">
                            <div><strong>Ngân hàng Quân Đội (MBBank)</strong> - STK: <strong>0987654321</strong></div>
                            <div>Chủ TK: <strong>AURA CAFE SYSTEM</strong></div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-bs-dismiss="modal">Đóng</button>
                    <button type="button" class="btn btn-primary" id="btnSubmitOrder" style="background-color: var(--aura-primary); border-color: var(--aura-primary);" onclick="submitCounterOrder()">
                        <span class="material-symbols-outlined" style="font-size: 18px;">receipt</span>
                        <span>Hoàn tất & In hóa đơn</span>
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- Toast Notifications -->
    <div id="toastContainer"></div>

    <!-- Bootstrap 5 JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

    <!-- POS Logic Script -->
    <script>
        const CONTEXT_PATH = '${pageContext.request.contextPath}';

        // Cart State
        let cartItems = [];
        let orderType = 'DINE_IN';
        let currentCustomer = null;
        let usePoints = false;
        let selectedPaymentMethod = 'CASH';

        // Temporary state for modal customizer
        let tempItem = {
            id: null,
            name: '',
            basePrice: 0,
            imageUrl: ''
        };

        // Bootstrap Modals
        let customModalInstance = null;
        let paymentModalInstance = null;

        document.addEventListener('DOMContentLoaded', () => {
            customModalInstance = new bootstrap.Modal(document.getElementById('customizationModal'));
            paymentModalInstance = new bootstrap.Modal(document.getElementById('paymentModal'));

            // Realtime Clock
            setInterval(() => {
                const now = new Date();
                const hours = String(now.getHours()).padStart(2, '0');
                const mins = String(now.getMinutes()).padStart(2, '0');
                const secs = String(now.getSeconds()).padStart(2, '0');
                const el = document.getElementById('clockDisplay');
                if (el) el.textContent = hours + ':' + mins + ':' + secs;
            }, 1000);

            // Listeners for Customizer live price
            document.querySelectorAll('input[name="optSize"], input[name="optTopping"]').forEach(el => {
                el.addEventListener('change', updateCustomizerPrice);
            });
        });

        // 1. Navigation Switches
        function switchMainTab(tab) {
            const btnPos = document.getElementById('tabBtnPos');
            const btnOrders = document.getElementById('tabBtnOrders');
            const viewCatalog = document.getElementById('viewPosCatalog');
            const viewCart = document.getElementById('viewPosCart');
            const viewOrders = document.getElementById('viewOrdersList');

            if (tab === 'pos') {
                btnPos.classList.add('active');
                btnOrders.classList.remove('active');
                viewCatalog.style.display = 'flex';
                viewCart.style.display = 'flex';
                viewOrders.classList.remove('active');
            } else {
                btnOrders.classList.add('active');
                btnPos.classList.remove('active');
                viewCatalog.style.display = 'none';
                viewCart.style.display = 'none';
                viewOrders.classList.add('active');
            }
        }

        // 2. Search & Filter
        function handleSearch(query) {
            const term = query.trim().toLowerCase();
            const cards = document.querySelectorAll('.product-card');
            cards.forEach(card => {
                const name = card.getAttribute('data-name').toLowerCase();
                const matched = name.includes(term);
                card.style.display = matched ? 'flex' : 'none';
            });
        }

        function filterCategory(catId, btn) {
            document.querySelectorAll('.category-chip').forEach(c => c.classList.remove('active'));
            btn.classList.add('active');

            const cards = document.querySelectorAll('.product-card');
            cards.forEach(card => {
                const cardCat = card.getAttribute('data-cat-id');
                if (catId === 'all' || cardCat === catId) {
                    card.style.display = 'flex';
                } else {
                    card.style.display = 'none';
                }
            });
        }

        // 3. Order Type (Dine-in vs Takeaway)
        function setOrderType(type) {
            orderType = type;
            const btnDine = document.getElementById('btnTypeDineIn');
            const btnTake = document.getElementById('btnTypeTakeaway');

            if (type === 'DINE_IN') {
                btnDine.classList.add('active');
                btnTake.classList.remove('active');
            } else {
                btnTake.classList.add('active');
                btnDine.classList.remove('active');
            }
        }

        // 4. Customization Modal
        function openProductCustomization(id, name, basePrice, imageUrl) {
            tempItem = { id, name, basePrice, imageUrl };
            document.getElementById('custModalItemName').textContent = name;
            document.getElementById('custModalBasePrice').textContent = formatCurrency(basePrice);
            document.getElementById('custModalNote').value = '';

            // Reset options
            document.querySelector('input[name="optSize"][value="M"]').checked = true;
            document.querySelectorAll('input[name="optTopping"]').forEach(cb => cb.checked = false);
            document.getElementById('optSugar').value = '100% đường';
            document.getElementById('optIce').value = '100% đá';

            updateCustomizerPrice();
            customModalInstance.show();
        }

        function calculateCustomItemPrice() {
            let price = tempItem.basePrice;
            const size = document.querySelector('input[name="optSize"]:checked')?.value || 'M';
            if (size === 'L') price += 8000;

            const toppings = document.querySelectorAll('input[name="optTopping"]:checked');
            price += (toppings.length * 10000);
            return price;
        }

        function updateCustomizerPrice() {
            const finalP = calculateCustomItemPrice();
            document.getElementById('custModalFinalPrice').textContent = formatCurrency(finalP);
        }

        function confirmAddToCart() {
            const size = document.querySelector('input[name="optSize"]:checked')?.value || 'M';
            const sugar = document.getElementById('optSugar').value;
            const ice = document.getElementById('optIce').value;
            const note = document.getElementById('custModalNote').value.trim();

            const toppingsArr = [];
            document.querySelectorAll('input[name="optTopping"]:checked').forEach(cb => {
                toppingsArr.push(cb.value);
            });

            // Build customization description string
            const parts = [];
            parts.push('Size ' + size);
            if (sugar !== '100% đường') parts.push(sugar);
            if (ice !== '100% đá') parts.push(ice);
            if (toppingsArr.length > 0) parts.push('+ ' + toppingsArr.join(', '));
            if (note) parts.push('Lưu ý: ' + note);

            const customStr = parts.join(' | ');
            const unitPrice = calculateCustomItemPrice();

            // Check if exact same item and customization exists in cart
            const existing = cartItems.find(it => it.productId === tempItem.id && it.customization === customStr);
            if (existing) {
                existing.quantity += 1;
                existing.subtotal = existing.quantity * existing.unitPrice;
            } else {
                cartItems.push({
                    productId: tempItem.id,
                    productName: tempItem.name,
                    quantity: 1,
                    unitPrice: unitPrice,
                    subtotal: unitPrice,
                    customization: customStr
                });
            }

            customModalInstance.hide();
            renderCart();
            showToast('Đã thêm món ' + tempItem.name);
        }

        // 5. Cart Management
        function renderCart() {
            const listEl = document.getElementById('cartItemsList');
            const emptyEl = document.getElementById('emptyCartMessage');
            const btnCheckout = document.getElementById('btnCheckout');

            if (cartItems.length === 0) {
                listEl.innerHTML = '';
                listEl.appendChild(emptyEl);
                emptyEl.style.display = 'flex';
                btnCheckout.disabled = true;
                updateSummary(0);
                return;
            }

            emptyEl.style.display = 'none';
            btnCheckout.disabled = false;

            let html = '';
            let rawSubtotal = 0;
            let totalQty = 0;

            cartItems.forEach((item, index) => {
                rawSubtotal += item.subtotal;
                totalQty += item.quantity;

                html += `
                    <div class="cart-item-card">
                        <div class="cart-item-main">
                            <div class="cart-item-title">\${escapeHtml(item.productName)}</div>
                            <div class="cart-item-price">\${formatCurrency(item.subtotal)}</div>
                        </div>
                        \${item.customization ? `<div class="cart-item-customization">\${escapeHtml(item.customization)}</div>` : ''}
                        <div class="cart-item-controls">
                            <div class="qty-stepper">
                                <button type="button" class="btn-stepper" onclick="updateQty(\${index}, -1)">
                                    <span class="material-symbols-outlined" style="font-size: 16px;">remove</span>
                                </button>
                                <input type="text" class="qty-input" value="\${item.quantity}" readonly>
                                <button type="button" class="btn-stepper" onclick="updateQty(\${index}, 1)">
                                    <span class="material-symbols-outlined" style="font-size: 16px;">add</span>
                                </button>
                            </div>
                            <button type="button" class="btn-remove-item" onclick="removeItem(\${index})" title="Xóa món">
                                <span class="material-symbols-outlined" style="font-size: 18px;">delete</span>
                            </button>
                        </div>
                    </div>
                `;
            });

            listEl.innerHTML = html;
            document.getElementById('cartCountTotal').textContent = totalQty;
            updateSummary(rawSubtotal);
        }

        function updateQty(index, delta) {
            if (!cartItems[index]) return;
            cartItems[index].quantity += delta;
            if (cartItems[index].quantity <= 0) {
                cartItems.splice(index, 1);
            } else {
                cartItems[index].subtotal = cartItems[index].quantity * cartItems[index].unitPrice;
            }
            renderCart();
        }

        function removeItem(index) {
            cartItems.splice(index, 1);
            renderCart();
        }

        function resetCart() {
            if (cartItems.length > 0 && !confirm('Bạn có chắc muốn làm mới đơn hàng hiện tại?')) {
                return;
            }
            cartItems = [];
            clearCustomer();
            document.getElementById('orderNoteInput').value = '';
            renderCart();
        }

        // 6. Summary Calculations
        function updateSummary(subtotal) {
            let discount = 0;
            if (usePoints && currentCustomer && currentCustomer.totalPoints > 0) {
                // 1 point = 1.000đ discount, capped at subtotal
                discount = Math.min(currentCustomer.totalPoints * 1000, subtotal);
            }

            const afterDiscount = Math.max(0, subtotal - discount);
            const vat = Math.round(afterDiscount * 0.08);
            const finalTotal = afterDiscount + vat;

            document.getElementById('lblSubtotal').textContent = formatCurrency(subtotal);
            document.getElementById('lblVat').textContent = formatCurrency(vat);
            document.getElementById('lblFinalTotal').textContent = formatCurrency(finalTotal);

            const rowDisc = document.getElementById('rowDiscount');
            if (discount > 0) {
                rowDisc.style.display = 'flex';
                document.getElementById('lblDiscount').textContent = '-' + formatCurrency(discount);
            } else {
                rowDisc.style.display = 'none';
            }

            return { subtotal, discount, vat, finalTotal };
        }

        // 7. Customer Loyalty Lookup
        async function searchCustomer() {
            const phone = document.getElementById('custPhoneInput').value.trim();
            if (!phone) {
                alert('Vui lòng nhập số điện thoại hoặc email khách hàng.');
                return;
            }

            try {
                const resp = await fetch(`\${CONTEXT_PATH}/cashier?action=search-customer&phone=\${encodeURIComponent(phone)}`);
                const data = await resp.json();
                if (data.success && data.customer) {
                    currentCustomer = data.customer;
                    document.getElementById('custSearchBox').style.display = 'none';
                    document.getElementById('custInfoBox').style.display = 'block';
                    document.getElementById('custNameDisplay').textContent = currentCustomer.fullName;
                    document.getElementById('custTierDisplay').textContent = currentCustomer.tierName;
                    document.getElementById('custPointsDisplay').textContent = currentCustomer.totalPoints;
                    document.getElementById('custDiscountDisplay').textContent = formatCurrency(currentCustomer.totalPoints * 1000);
                    showToast('Đã nhận diện hội viên: ' + currentCustomer.fullName);
                    renderCart();
                } else {
                    alert(data.error || 'Không tìm thấy khách hàng với thông tin này.');
                }
            } catch (e) {
                console.error(e);
                alert('Lỗi kết nối khi tìm kiếm khách hàng.');
            }
        }

        function clearCustomer() {
            currentCustomer = null;
            usePoints = false;
            document.getElementById('chkUsePoints').checked = false;
            document.getElementById('custSearchBox').style.display = 'flex';
            document.getElementById('custInfoBox').style.display = 'none';
            document.getElementById('custPhoneInput').value = '';
            renderCart();
        }

        function toggleUsePoints() {
            usePoints = document.getElementById('chkUsePoints').checked;
            renderCart();
        }

        // 8. Checkout & Payment Modal
        function openPaymentModal() {
            if (cartItems.length === 0) return;

            const sub = cartItems.reduce((acc, it) => acc + it.subtotal, 0);
            const { finalTotal } = updateSummary(sub);

            document.getElementById('payModalTotalDisplay').textContent = formatCurrency(finalTotal);
            document.getElementById('payModalSummaryDesc').textContent = (orderType === 'DINE_IN' ? 'Dùng tại quán' : 'Mang về');

            selectPaymentMethod('CASH');
            setCashAmount(finalTotal);
            paymentModalInstance.show();
        }

        function selectPaymentMethod(method) {
            selectedPaymentMethod = method;
            const btnCash = document.getElementById('btnMethodCash');
            const btnQR = document.getElementById('btnMethodQR');
            const pCash = document.getElementById('panelCashPayment');
            const pQR = document.getElementById('panelQRPayment');

            if (method === 'CASH') {
                btnCash.classList.add('active');
                btnQR.classList.remove('active');
                pCash.style.display = 'block';
                pQR.style.display = 'none';
            } else {
                btnQR.classList.add('active');
                btnCash.classList.remove('active');
                pCash.style.display = 'none';
                pQR.style.display = 'block';

                // Render VietQR image
                const sub = cartItems.reduce((acc, it) => acc + it.subtotal, 0);
                const { finalTotal } = updateSummary(sub);
                const desc = 'AURAPOS ' + Date.now().toString().slice(-6);
                const qrUrl = `https://img.vietqr.io/image/MB-0987654321-compact2.png?amount=\${finalTotal}&addInfo=\${encodeURIComponent(desc)}&accountName=AURA%20CAFE`;
                document.getElementById('vietQrImg').src = qrUrl;
            }
        }

        function setCashAmount(val) {
            document.getElementById('inputCashGiven').value = val;
            calculateChange();
        }

        function addQuickCash(delta) {
            const sub = cartItems.reduce((acc, it) => acc + it.subtotal, 0);
            const { finalTotal } = updateSummary(sub);
            if (delta === 0) {
                setCashAmount(finalTotal);
            } else {
                const cur = Number(document.getElementById('inputCashGiven').value) || 0;
                setCashAmount(cur + delta);
            }
        }

        function calculateChange() {
            const sub = cartItems.reduce((acc, it) => acc + it.subtotal, 0);
            const { finalTotal } = updateSummary(sub);
            const given = Number(document.getElementById('inputCashGiven').value) || 0;
            const change = Math.max(0, given - finalTotal);
            document.getElementById('lblCashChange').textContent = formatCurrency(change);
        }

        // 9. Submit Order via AJAX
        async function submitCounterOrder() {
            const btnSubmit = document.getElementById('btnSubmitOrder');
            btnSubmit.disabled = true;
            btnSubmit.innerHTML = `<span class="material-symbols-outlined" style="font-size: 18px;">hourglass_empty</span><span>Đang xử lý...</span>`;

            const sub = cartItems.reduce((acc, it) => acc + it.subtotal, 0);
            const { discount, finalTotal } = updateSummary(sub);

            const orderNote = document.getElementById('orderNoteInput').value.trim();

            const payload = {
                orderType: orderType,
                tableNumber: '',
                customerId: currentCustomer ? currentCustomer.id : null,
                recipientName: currentCustomer ? currentCustomer.fullName : (orderType === 'DINE_IN' ? 'Khách tại quán' : 'Khách mang về'),
                recipientPhone: currentCustomer ? currentCustomer.phone : '',
                orderNote: orderNote,
                paymentMethod: selectedPaymentMethod,
                pointsUsed: (usePoints && currentCustomer) ? Math.min(currentCustomer.totalPoints, Math.floor(discount / 1000)) : 0,
                discountAmount: discount,
                totalAmount: sub,
                finalAmount: finalTotal,
                items: cartItems.map(it => ({
                    productId: it.productId,
                    productName: it.productName,
                    quantity: it.quantity,
                    unitPrice: it.unitPrice,
                    subtotal: it.subtotal,
                    customization: it.customization
                }))
            };

            try {
                const response = await fetch(`\${CONTEXT_PATH}/cashier?action=create-order`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                const res = await response.json();

                if (res.success) {
                    paymentModalInstance.hide();
                    showToast('Tạo đơn thành công! Mã đơn: ' + res.orderCode);
                    
                    // Reset cart
                    cartItems = [];
                    clearCustomer();
                    document.getElementById('orderNoteInput').value = '';
                    renderCart();

                    // Offer to open e-Bill
                    setTimeout(() => {
                        if (confirm(`Đơn hàng \${res.orderCode} đã được tạo thành công!\nBạn có muốn mở xem / in hóa đơn ngay không?`)) {
                            window.open(`\${CONTEXT_PATH}/bill?orderId=\${res.orderId}`, '_blank');
                        }
                        window.location.reload();
                    }, 400);
                } else {
                    alert(res.error || 'Có lỗi xảy ra khi tạo đơn.');
                }
            } catch (err) {
                console.error(err);
                alert('Lỗi kết nối máy chủ.');
            } finally {
                btnSubmit.disabled = false;
                btnSubmit.innerHTML = `<span class="material-symbols-outlined" style="font-size: 18px;">receipt</span><span>Hoàn tất & In hóa đơn</span>`;
            }
        }

        // 10. Cancel Order
        async function cancelOrder(orderId) {
            if (!confirm(`Bạn có chắc chắn muốn hủy đơn hàng #${orderId}?`)) {
                return;
            }

            try {
                const resp = await fetch(`\${CONTEXT_PATH}/cashier?action=cancel-order&orderId=\${orderId}`, {
                    method: 'POST'
                });
                const res = await resp.json();
                if (res.success) {
                    showToast('Đã hủy đơn thành công');
                    setTimeout(() => window.location.reload(), 600);
                } else {
                    alert(res.error || 'Không thể hủy đơn.');
                }
            } catch (e) {
                console.error(e);
                alert('Lỗi kết nối máy chủ.');
            }
        }

        // Utility Helpers
        function formatCurrency(amount) {
            return new Intl.NumberFormat('vi-VN').format(Math.round(amount)) + 'đ';
        }

        function escapeHtml(str) {
            if (!str) return '';
            return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
        }

        function showToast(msg) {
            const container = document.getElementById('toastContainer');
            const toast = document.createElement('div');
            toast.className = 'pos-toast';
            toast.innerHTML = `<span class="material-symbols-outlined">check_circle</span><span>\${msg}</span>`;
            container.appendChild(toast);
            setTimeout(() => {
                toast.style.opacity = '0';
                toast.style.transition = 'opacity 0.3s ease';
                setTimeout(() => toast.remove(), 300);
            }, 3000);
        }
    </script>
</body>
</html>
