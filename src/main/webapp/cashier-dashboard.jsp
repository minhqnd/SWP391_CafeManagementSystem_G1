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
            background: #f8fafc;
            display: none;
            flex-direction: column;
            overflow-y: auto;
            padding: 24px 32px;
            gap: 20px;
        }

        .orders-panel.active {
            display: flex;
        }

        /* Top Header Bar */
        .orders-view-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 16px;
        }

        .orders-view-title {
            font-size: 22px;
            font-weight: 800;
            color: var(--slate-900);
            display: flex;
            align-items: center;
            gap: 10px;
            letter-spacing: -0.5px;
            margin: 0;
        }

        .orders-view-subtitle {
            font-size: 13.5px;
            color: var(--slate-500);
            margin-top: 4px;
        }

        .btn-reload-orders {
            background: #ffffff;
            border: 1px solid var(--slate-200);
            border-radius: 8px;
            padding: 9px 16px;
            font-size: 13px;
            font-weight: 600;
            color: var(--slate-700);
            display: inline-flex;
            align-items: center;
            gap: 8px;
            cursor: pointer;
            box-shadow: 0 1px 2px rgba(0,0,0,0.05);
            transition: all 0.2s ease;
        }

        .btn-reload-orders:hover {
            background: var(--slate-50);
            border-color: var(--slate-300);
            color: var(--slate-900);
            transform: translateY(-1px);
        }

        /* Stats KPI Cards */
        .orders-kpi-grid {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 16px;
        }

        @media (max-width: 1200px) {
            .orders-kpi-grid {
                grid-template-columns: repeat(2, 1fr);
            }
        }

        .kpi-stat-card {
            background: #ffffff;
            border: 1px solid var(--slate-200);
            border-radius: 12px;
            padding: 16px 20px;
            display: flex;
            align-items: center;
            gap: 16px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.03);
            transition: transform 0.2s ease, box-shadow 0.2s ease;
        }

        .kpi-stat-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 16px rgba(0,0,0,0.05);
        }

        .kpi-stat-icon {
            width: 48px;
            height: 48px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
        }

        .kpi-stat-icon.total {
            background: #eff6ff;
            color: #2563eb;
        }

        .kpi-stat-icon.revenue {
            background: #ecfdf5;
            color: #059669;
        }

        .kpi-stat-icon.success {
            background: #f0fdfa;
            color: #0d9488;
        }

        .kpi-stat-icon.cancelled {
            background: #fff1f2;
            color: #e11d48;
        }

        .kpi-stat-info {
            display: flex;
            flex-direction: column;
        }

        .kpi-stat-label {
            font-size: 12px;
            font-weight: 600;
            color: var(--slate-500);
            text-transform: uppercase;
            letter-spacing: 0.3px;
        }

        .kpi-stat-value {
            font-size: 20px;
            font-weight: 800;
            color: var(--slate-900);
            line-height: 1.2;
            margin-top: 2px;
        }

        .kpi-stat-sub {
            font-size: 11.5px;
            color: var(--slate-400);
            margin-top: 2px;
        }

        /* Filter & Search Toolbar Card */
        .orders-filter-card {
            background: #ffffff;
            border: 1px solid var(--slate-200);
            border-radius: 12px;
            padding: 14px 18px;
            display: flex;
            flex-direction: column;
            gap: 12px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.03);
        }

        .filter-row-top {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 16px;
            flex-wrap: wrap;
        }

        .search-input-wrapper {
            position: relative;
            flex: 1;
            min-width: 280px;
        }

        .search-input-wrapper .search-icon {
            position: absolute;
            left: 14px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--slate-400);
            font-size: 19px;
            pointer-events: none;
        }

        .orders-search-input {
            width: 100%;
            height: 38px;
            padding: 6px 14px 6px 40px;
            border-radius: 8px;
            border: 1px solid var(--slate-200);
            background: var(--slate-50);
            font-size: 13.5px;
            color: var(--slate-800);
            transition: all 0.2s ease;
            outline: none;
        }

        .orders-search-input:focus {
            background: #ffffff;
            border-color: var(--aura-primary);
            box-shadow: 0 0 0 3px rgba(144, 11, 26, 0.1);
        }

        .filter-type-group {
            display: flex;
            align-items: center;
            background: var(--slate-100);
            padding: 3px;
            border-radius: 8px;
            border: 1px solid var(--slate-200);
            gap: 2px;
        }

        .btn-filter-type {
            border: none;
            background: transparent;
            padding: 6px 14px;
            border-radius: 6px;
            font-size: 13px;
            font-weight: 600;
            color: var(--slate-600);
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 6px;
            transition: all 0.15s ease;
        }

        .btn-filter-type:hover {
            color: var(--slate-900);
        }

        .btn-filter-type.active {
            background: #ffffff;
            color: var(--slate-900);
            box-shadow: 0 1px 3px rgba(0,0,0,0.08);
        }

        .filter-status-pills {
            display: flex;
            align-items: center;
            gap: 8px;
            flex-wrap: wrap;
        }

        .btn-status-pill {
            border: 1px solid var(--slate-200);
            background: #ffffff;
            padding: 5px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
            color: var(--slate-600);
            cursor: pointer;
            transition: all 0.15s ease;
            display: inline-flex;
            align-items: center;
            gap: 5px;
        }

        .btn-status-pill:hover {
            border-color: var(--slate-300);
            color: var(--slate-800);
        }

        .btn-status-pill.active {
            background: var(--slate-900);
            color: #ffffff;
            border-color: var(--slate-900);
        }

        /* Orders Table Card */
        .orders-table-card {
            background: #ffffff;
            border: 1px solid var(--slate-200);
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 1px 4px rgba(0,0,0,0.03);
        }

        .orders-table-wrapper {
            overflow-x: auto;
            max-height: calc(100vh - 380px);
            min-height: 250px;
        }

        .pos-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 13.5px;
        }

        .pos-table th {
            background: var(--slate-50);
            color: var(--slate-600);
            font-size: 11.5px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            padding: 12px 18px;
            border-bottom: 1px solid var(--slate-200);
            text-align: left;
            position: sticky;
            top: 0;
            z-index: 10;
            white-space: nowrap;
        }

        .pos-table td {
            padding: 13px 18px;
            border-bottom: 1px solid var(--slate-100);
            vertical-align: middle;
            color: var(--slate-800);
        }

        .order-table-row {
            transition: background-color 0.15s ease;
        }

        .order-table-row:hover {
            background-color: #f8fafc;
        }

        /* Table Badges & Elements */
        .order-code-badge {
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
            font-weight: 700;
            color: var(--slate-900);
            background: var(--slate-100);
            padding: 4px 8px;
            border-radius: 6px;
            border: 1px solid var(--slate-200);
            display: inline-flex;
            align-items: center;
            gap: 4px;
            font-size: 12.5px;
        }

        .badge-type {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 3px 9px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 600;
        }

        .badge-type.dine-in {
            background: #fef3c7;
            color: #92400e;
            border: 1px solid #fde68a;
        }

        .badge-type.takeaway {
            background: #e0f2fe;
            color: #0369a1;
            border: 1px solid #bae6fd;
        }

        .order-items-snippet {
            max-width: 320px;
            display: flex;
            flex-direction: column;
            gap: 2px;
        }

        .order-items-text {
            font-size: 13px;
            font-weight: 500;
            color: var(--slate-800);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .order-items-qty {
            font-size: 11.5px;
            color: var(--slate-500);
        }

        .order-price-val {
            font-size: 14.5px;
            font-weight: 700;
            color: var(--aura-primary);
            font-variant-numeric: tabular-nums;
        }

        .badge-payment {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 3px 8px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 600;
        }

        .badge-payment.cash {
            background: #f1f5f9;
            color: #334155;
            border: 1px solid var(--slate-200);
        }

        .badge-payment.qr {
            background: #ecfeff;
            color: #0e7490;
            border: 1px solid #a5f3fc;
        }

        .badge-status {
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 700;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            white-space: nowrap;
        }

        .status-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
        }

        .badge-status.confirmed {
            background: #ecfdf5;
            color: #065f46;
            border: 1px solid #a7f3d0;
        }
        .badge-status.confirmed .status-dot { background: #10b981; }

        .badge-status.preparing {
            background: #eff6ff;
            color: #1d4ed8;
            border: 1px solid #bfdbfe;
        }
        .badge-status.preparing .status-dot { background: #3b82f6; }

        .badge-status.ready {
            background: #fffbeb;
            color: #b45309;
            border: 1px solid #fde68a;
        }
        .badge-status.ready .status-dot { background: #f59e0b; }

        .badge-type.delivery {
            background: #fdf2f8;
            color: #9d174d;
            border: 1px solid #fbcfe8;
        }

        .badge-status.completed {
            background: #f1f5f9;
            color: #334155;
            border: 1px solid #cbd5e1;
        }
        .badge-status.completed .status-dot { background: #64748b; }

        .badge-status.delivering {
            background: #fae8ff;
            color: #86198f;
            border: 1px solid #f0abfc;
        }
        .badge-status.delivering .status-dot { background: #d946ef; }

        .badge-status.cancelled {
            background: #fff1f2;
            color: #9f1239;
            border: 1px solid #fecdd3;
        }
        .badge-status.cancelled .status-dot { background: #f43f5e; }

        .col-actions {
            width: 210px;
            min-width: 210px;
            text-align: center;
            white-space: nowrap;
        }

        .btn-action-bill {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 4px;
            padding: 5px 10px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 600;
            background: #ffffff;
            border: 1px solid var(--slate-200);
            color: var(--slate-700);
            text-decoration: none;
            transition: all 0.15s ease;
            white-space: nowrap;
            flex-shrink: 0;
            height: 32px;
        }

        .btn-action-bill:hover {
            background: var(--slate-50);
            border-color: var(--slate-300);
            color: var(--slate-900);
        }

        .btn-action-deliver-order {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 3px;
            padding: 5px 9px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 600;
            background: #fae8ff;
            border: 1px solid #f0abfc;
            color: #86198f;
            cursor: pointer;
            transition: all 0.15s ease;
            white-space: nowrap;
            flex-shrink: 0;
            height: 32px;
        }

        .btn-action-deliver-order:hover {
            background: #f5d0fe;
            border-color: #e879f9;
            color: #701a75;
        }

        .btn-action-complete-order {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 3px;
            padding: 5px 9px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 600;
            background: #ecfdf5;
            border: 1px solid #a7f3d0;
            color: #047857;
            cursor: pointer;
            transition: all 0.15s ease;
            white-space: nowrap;
            flex-shrink: 0;
            height: 32px;
        }

        .btn-action-complete-order:hover {
            background: #d1fae5;
            border-color: #6ee7b7;
            color: #065f46;
        }

        .btn-action-cancel-order {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 3px;
            padding: 5px 9px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 600;
            background: #fff1f2;
            border: 1px solid #fecdd3;
            color: #be123c;
            cursor: pointer;
            transition: all 0.15s ease;
            white-space: nowrap;
            flex-shrink: 0;
            height: 32px;
        }

        .btn-action-cancel-order:hover {
            background: #ffe4e6;
            border-color: #fda4af;
            color: #9f1239;
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
            gap: 10px;
            row-gap: 12px;
            margin-bottom: 16px;
        }

        .opt-chips-grid label {
            display: inline-flex;
            margin: 0;
        }

        .opt-radio-label {
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            padding: 8px 16px;
            background: var(--slate-100);
            border: 1px solid var(--slate-200);
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            line-height: 1.4;
            color: var(--slate-700);
            transition: all 0.15s;
            user-select: none;
        }

        input[type="radio"]:checked + .opt-radio-label,
        input[type="checkbox"]:checked + .opt-radio-label {
            background: var(--aura-primary);
            border-color: var(--aura-primary);
            color: #ffffff;
        }

        /* Payment Modal Specifics */
        #paymentModal .modal-dialog {
            max-width: 480px;
        }

        #paymentModal .modal-content {
            border-radius: 16px;
            border: 1px solid var(--slate-200);
            box-shadow: 0 20px 40px -15px rgba(15, 23, 42, 0.2);
            overflow: hidden;
        }

        #paymentModal .modal-header {
            padding: 16px 20px 14px;
            border-bottom: 1px solid var(--slate-100);
            background: #ffffff;
        }

        .pay-modal-icon-badge {
            width: 38px;
            height: 38px;
            border-radius: 10px;
            background: var(--aura-primary-light);
            color: var(--aura-primary);
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .pay-total-banner {
            background: linear-gradient(135deg, #8b0e1d 0%, #5c0510 100%);
            color: #ffffff;
            border-radius: 12px;
            padding: 16px 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 16px;
            box-shadow: 0 6px 18px rgba(139, 14, 29, 0.22);
        }

        .pay-total-label {
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 0.8px;
            text-transform: uppercase;
            color: rgba(255, 255, 255, 0.75);
            margin-bottom: 2px;
        }

        .pay-total-value {
            font-size: 26px;
            font-weight: 800;
            color: #ffffff;
            letter-spacing: -0.5px;
            line-height: 1.2;
        }

        .pay-order-badge {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            background: rgba(255, 255, 255, 0.16);
            backdrop-filter: blur(4px);
            padding: 5px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
            color: #ffffff;
            border: 1px solid rgba(255, 255, 255, 0.22);
        }

        /* Segmented Method Tabs */
        .pay-method-tabs {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 4px;
            padding: 4px;
            background: var(--slate-100);
            border-radius: 10px;
            margin-bottom: 18px;
        }

        .pay-method-btn {
            padding: 9px 12px;
            border-radius: 7px;
            border: none;
            background: transparent;
            font-weight: 600;
            font-size: 13.5px;
            color: var(--slate-600);
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 7px;
            transition: all 0.15s ease;
        }

        .pay-method-btn:hover {
            color: var(--slate-900);
        }

        .pay-method-btn.active {
            background: #ffffff;
            color: var(--aura-primary);
            font-weight: 700;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
        }

        /* Cash Input */
        .input-cash-wrapper {
            position: relative;
            display: flex;
            align-items: center;
        }

        .cash-input-icon {
            position: absolute;
            left: 12px;
            font-size: 20px;
            color: var(--slate-400);
            pointer-events: none;
        }

        .input-cash-field {
            width: 100%;
            padding: 10px 50px 10px 38px;
            border: 1.5px solid var(--slate-200);
            border-radius: 10px;
            font-size: 20px;
            font-weight: 700;
            color: var(--slate-900);
            background: #ffffff;
            transition: border-color 0.15s, box-shadow 0.15s;
        }

        .input-cash-field:focus {
            outline: none;
            border-color: var(--aura-primary);
            box-shadow: 0 0 0 3px rgba(144, 11, 26, 0.1);
        }

        .input-cash-field::-webkit-outer-spin-button,
        .input-cash-field::-webkit-inner-spin-button {
            -webkit-appearance: none;
            margin: 0;
        }

        .input-cash-field[type=number] {
            -moz-appearance: textfield;
        }

        .cash-currency-tag {
            position: absolute;
            right: 14px;
            font-size: 13px;
            font-weight: 700;
            color: var(--slate-400);
            pointer-events: none;
        }

        /* Quick Cash Chips */
        .quick-cash-row {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 7px;
            margin-top: 10px;
            margin-bottom: 16px;
        }

        .btn-quick-cash {
            border: 1px solid var(--slate-200);
            background: #ffffff;
            padding: 8px 4px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 700;
            color: var(--slate-700);
            cursor: pointer;
            transition: all 0.15s ease;
            text-align: center;
        }

        .btn-quick-cash:hover {
            background: var(--slate-50);
            border-color: var(--slate-300);
            color: var(--slate-900);
            transform: translateY(-1px);
        }

        .btn-quick-cash.exact-cash {
            background: #ecfdf5;
            border-color: #a7f3d0;
            color: #065f46;
        }

        .btn-quick-cash.exact-cash:hover {
            background: #d1fae5;
            border-color: #6ee7b7;
        }

        /* Cash Change Card */
        .cash-change-card {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 12px 16px;
            background: #f8fafc;
            border: 1.5px dashed var(--slate-200);
            border-radius: 10px;
            transition: all 0.2s ease;
        }

        .cash-change-title {
            font-size: 13px;
            font-weight: 700;
            color: var(--slate-800);
        }

        .cash-change-sub {
            font-size: 11px;
            color: var(--slate-400);
        }

        .cash-change-amount {
            font-size: 18px;
            font-weight: 800;
        }

        /* VietQR Styling */
        .qr-payment-panel {
            text-align: center;
            padding: 4px 0 10px;
        }

        .qr-status-pill {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: #f0fdf4;
            color: #166534;
            border: 1px solid #bbf7d0;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
            margin-bottom: 12px;
        }

        .qr-pulse-dot {
            width: 7px;
            height: 7px;
            background-color: #22c55e;
            border-radius: 50%;
            display: inline-block;
            box-shadow: 0 0 0 rgba(34, 197, 94, 0.4);
            animation: pulse-dot 1.8s infinite;
        }

        @keyframes pulse-dot {
            0% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7); }
            70% { box-shadow: 0 0 0 6px rgba(34, 197, 94, 0); }
            100% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
        }

        .qr-card-wrapper {
            background: #ffffff;
            border: 1px solid var(--slate-200);
            border-radius: 12px;
            padding: 14px;
            display: inline-flex;
            flex-direction: column;
            align-items: center;
            box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
            margin-bottom: 14px;
        }

        .qr-frame {
            border-radius: 8px;
            overflow: hidden;
            display: flex;
            justify-content: center;
            align-items: center;
        }

        .qr-image {
            width: 190px;
            height: 190px;
            object-fit: contain;
            display: block;
        }

        .qr-scan-guide {
            display: flex;
            align-items: center;
            gap: 4px;
            color: var(--slate-500);
            font-size: 12px;
            font-weight: 500;
            margin-top: 10px;
        }

        .bank-details-card {
            background: var(--slate-50);
            border: 1px solid var(--slate-200);
            border-radius: 10px;
            padding: 10px 14px;
            display: flex;
            flex-direction: column;
            gap: 6px;
            text-align: left;
        }

        .bank-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 12.5px;
        }

        .bank-label {
            color: var(--slate-500);
            font-weight: 500;
        }

        .bank-val {
            color: var(--slate-800);
            font-weight: 600;
        }

        .btn-copy-stk {
            background: transparent;
            border: none;
            color: var(--slate-400);
            padding: 2px 4px;
            border-radius: 4px;
            cursor: pointer;
            display: flex;
            align-items: center;
            transition: all 0.15s;
        }

        .btn-copy-stk:hover {
            color: var(--aura-primary);
            background: var(--slate-200);
        }

        /* Pay Modal Footer */
        .pay-modal-footer {
            padding: 12px 20px;
            border-top: 1px solid var(--slate-100);
            display: flex;
            justify-content: flex-end;
            gap: 8px;
            background: #ffffff;
        }

        .btn-pay-cancel {
            padding: 9px 18px;
            border-radius: 8px;
            border: 1px solid var(--slate-200);
            background: #ffffff;
            font-weight: 600;
            font-size: 13.5px;
            color: var(--slate-600);
            cursor: pointer;
            transition: all 0.15s;
        }

        .btn-pay-cancel:hover {
            background: var(--slate-100);
            color: var(--slate-800);
        }

        .btn-pay-submit {
            padding: 9px 22px;
            border-radius: 8px;
            border: none;
            background: var(--aura-primary);
            color: #ffffff;
            font-weight: 700;
            font-size: 13.5px;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            box-shadow: 0 4px 12px rgba(144, 11, 26, 0.25);
            transition: all 0.15s;
        }

        .btn-pay-submit:hover {
            background: var(--aura-primary-hover);
            box-shadow: 0 6px 16px rgba(144, 11, 26, 0.35);
            transform: translateY(-1px);
        }

        .btn-pay-submit:disabled {
            opacity: 0.7;
            cursor: not-allowed;
            transform: none;
        }

        /* Toast Container */
        #toastContainer {
            position: fixed;
            bottom: 20px;
            left: 20px;
            right: auto;
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
        <c:set var="todayCount" value="${not empty counterOrders ? counterOrders.size() : 0}" />

        <div class="orders-panel" id="viewOrdersList">
            <!-- Header Section -->
            <div class="orders-view-header">
                <div>
                    <h2 class="orders-view-title">
                        <span class="material-symbols-outlined text-primary" style="font-size: 28px;">receipt_long</span>
                        <span>Danh sách đơn hàng tại quầy hôm nay</span>
                    </h2>
                    <div class="orders-view-subtitle">
                        Theo dõi thời gian thực các đơn bán tại quầy • Tự động đồng bộ với màn hình Barista KDS
                    </div>
                </div>
                <div class="d-flex align-items-center gap-2">
                    <button type="button" class="btn-reload-orders" onclick="window.location.reload()" title="Tải lại danh sách đơn mới nhất">
                        <span class="material-symbols-outlined" style="font-size: 18px;">sync</span>
                        <span>Tải lại dữ liệu</span>
                    </button>
                </div>
            </div>

            <!-- Filter & Search Toolbar Card -->
            <div class="orders-filter-card">
                <div class="filter-row-top">
                    <!-- Search Input -->
                    <div class="search-input-wrapper">
                        <span class="material-symbols-outlined search-icon">search</span>
                        <input type="text" id="orderSearchInput" class="orders-search-input" 
                               placeholder="Tìm nhanh theo mã đơn (ORD-...), tên khách, số điện thoại, món..." 
                               oninput="applyOrderFilters()">
                    </div>

                    <!-- Type Filter -->
                    <div class="filter-type-group">
                        <button type="button" class="btn-filter-type active" data-type="ALL" onclick="setOrderTypeFilter('ALL', this)">
                            <span class="material-symbols-outlined" style="font-size: 16px;">filter_list</span>
                            <span>Tất cả</span>
                        </button>
                        <button type="button" class="btn-filter-type" data-type="DINE_IN" onclick="setOrderTypeFilter('DINE_IN', this)">
                            <span class="material-symbols-outlined" style="font-size: 16px;">storefront</span>
                            <span>Tại quán</span>
                        </button>
                        <button type="button" class="btn-filter-type" data-type="TAKEAWAY" onclick="setOrderTypeFilter('TAKEAWAY', this)">
                            <span class="material-symbols-outlined" style="font-size: 16px;">takeout_dining</span>
                            <span>Mang về</span>
                        </button>
                        <button type="button" class="btn-filter-type" data-type="ONLINE" onclick="setOrderTypeFilter('ONLINE', this)">
                            <span class="material-symbols-outlined" style="font-size: 16px;">delivery_dining</span>
                            <span>Giao hàng</span>
                        </button>
                    </div>
                </div>

                <!-- Status Filter Pills -->
                <div class="filter-status-pills">
                    <span class="text-xs fw-bold text-muted me-1 text-uppercase" style="letter-spacing: 0.5px;">Trạng thái:</span>
                    <button type="button" class="btn-status-pill active" data-status="ALL" onclick="setOrderStatusFilter('ALL', this)">
                        <span>Tất cả (<span id="statusCountAll">${todayCount}</span>)</span>
                    </button>
                    <button type="button" class="btn-status-pill" data-status="CONFIRMED" onclick="setOrderStatusFilter('CONFIRMED', this)">
                        <span class="status-dot" style="background: #10b981;"></span>
                        <span>Đã thanh toán</span>
                    </button>
                    <button type="button" class="btn-status-pill" data-status="PREPARING" onclick="setOrderStatusFilter('PREPARING', this)">
                        <span class="status-dot" style="background: #3b82f6;"></span>
                        <span>Đang pha chế</span>
                    </button>
                    <button type="button" class="btn-status-pill" data-status="READY" onclick="setOrderStatusFilter('READY', this)">
                        <span class="status-dot" style="background: #f59e0b;"></span>
                        <span>Sẵn sàng</span>
                    </button>
                    <button type="button" class="btn-status-pill" data-status="DELIVERING" onclick="setOrderStatusFilter('DELIVERING', this)">
                        <span class="status-dot" style="background: #d946ef;"></span>
                        <span>Đang giao</span>
                    </button>
                    <button type="button" class="btn-status-pill" data-status="COMPLETED" onclick="setOrderStatusFilter('COMPLETED', this)">
                        <span class="status-dot" style="background: #64748b;"></span>
                        <span>Hoàn thành</span>
                    </button>
                    <button type="button" class="btn-status-pill" data-status="CANCELLED" onclick="setOrderStatusFilter('CANCELLED', this)">
                        <span class="status-dot" style="background: #f43f5e;"></span>
                        <span>Đã hủy</span>
                    </button>
                </div>
            </div>

            <!-- Orders Table Card -->
            <div class="orders-table-card">
                <div class="orders-table-wrapper">
                    <table class="pos-table" id="ordersTable">
                        <thead>
                            <tr>
                                <th style="width: 160px; white-space: nowrap;">Mã đơn</th>
                                <th style="width: 95px; white-space: nowrap;">Thời gian</th>
                                <th style="width: 110px; white-space: nowrap;">Loại đơn</th>
                                <th style="width: 150px; white-space: nowrap;">Khách hàng</th>
                                <th>Món đã gọi</th>
                                <th class="text-end" style="width: 120px; white-space: nowrap;">Tổng tiền</th>
                                <th style="width: 110px; white-space: nowrap;">Hình thức</th>
                                <th style="width: 140px; white-space: nowrap;">Trạng thái</th>
                                <th class="col-actions">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody id="ordersTableBody">
                            <c:choose>
                                <c:when test="${not empty counterOrders}">
                                    <c:forEach var="o" items="${counterOrders}">
                                    <tr class="order-table-row" 
                                        data-type="${o.orderType}" 
                                        data-status="${o.status}" 
                                        data-search="${o.orderCode} ${o.recipientName} ${o.recipientPhone} ${o.itemSummary} ${o.deliveryAddress}">
                                        <td>
                                            <span class="order-code-badge" onclick="copyOrderCode('${o.orderCode}')" title="Bấm để sao chép mã" style="cursor: pointer;">
                                                <span class="material-symbols-outlined" style="font-size: 14px; color: var(--slate-500);">tag</span>
                                                <span>${o.orderCode}</span>
                                            </span>
                                        </td>
                                        <td>
                                            <div class="fw-semibold text-slate-800">${o.createdTimeText}</div>
                                            <div style="font-size: 11px; color: var(--slate-400);">Hôm nay</div>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${o.orderType eq 'ONLINE'}">
                                                    <span class="badge-type delivery">
                                                        <span class="material-symbols-outlined" style="font-size: 14px;">delivery_dining</span>
                                                        <span>Giao hàng</span>
                                                    </span>
                                                </c:when>
                                                <c:when test="${o.orderType eq 'TAKEAWAY'}">
                                                    <span class="badge-type takeaway">
                                                        <span class="material-symbols-outlined" style="font-size: 14px;">takeout_dining</span>
                                                        <span>Mang về</span>
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge-type dine-in">
                                                        <span class="material-symbols-outlined" style="font-size: 14px;">storefront</span>
                                                        <span>Tại quán<c:if test="${not empty o.tableNumber}"> (Bàn ${o.tableNumber})</c:if></span>
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <div class="fw-semibold text-slate-800 d-flex align-items-center gap-1">
                                                <span class="material-symbols-outlined text-slate-400" style="font-size: 16px;">person</span>
                                                <span class="text-truncate" style="max-width: 130px;" title="${empty o.recipientName ? 'Khách tại quầy' : o.recipientName}">
                                                    ${empty o.recipientName ? 'Khách tại quầy' : o.recipientName}
                                                </span>
                                            </div>
                                            <c:if test="${not empty o.recipientPhone}">
                                                <div style="font-size: 11px; color: var(--slate-500); padding-left: 20px;">${o.recipientPhone}</div>
                                            </c:if>
                                            <c:if test="${not empty o.deliveryAddress}">
                                                <div style="font-size: 11px; color: var(--slate-400); padding-left: 20px;" class="text-truncate" style="max-width: 130px;" title="${o.deliveryAddress}">${o.deliveryAddress}</div>
                                            </c:if>
                                        </td>
                                        <td>
                                            <div class="order-items-snippet" title="${o.itemSummary}">
                                                <div class="order-items-text">${o.itemSummary}</div>
                                                <div class="order-items-qty">${o.itemCount} món</div>
                                            </div>
                                        </td>
                                        <td class="text-end">
                                            <div class="order-price-val">${o.formattedFinalAmount}</div>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${o.paymentMethod eq 'BANK_TRANSFER' || (not empty o.paymentMethodLabel && o.paymentMethodLabel.contains('VietQR'))}">
                                                    <span class="badge-payment qr">
                                                        <span class="material-symbols-outlined" style="font-size: 14px;">qr_code_2</span>
                                                        <span>VietQR</span>
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge-payment cash">
                                                        <span class="material-symbols-outlined" style="font-size: 14px;">payments</span>
                                                        <span>Tiền mặt</span>
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${o.status eq 'CONFIRMED'}">
                                                    <span class="badge-status confirmed">
                                                        <span class="status-dot"></span>
                                                        <span>Đã thanh toán</span>
                                                    </span>
                                                </c:when>
                                                <c:when test="${o.status eq 'PREPARING'}">
                                                    <span class="badge-status preparing">
                                                        <span class="status-dot"></span>
                                                        <span>Đang pha chế</span>
                                                    </span>
                                                </c:when>
                                                <c:when test="${o.status eq 'READY'}">
                                                    <span class="badge-status ready">
                                                        <span class="status-dot"></span>
                                                        <span>Sẵn sàng</span>
                                                    </span>
                                                </c:when>
                                                <c:when test="${o.status eq 'DELIVERING'}">
                                                    <span class="badge-status delivering">
                                                        <span class="status-dot"></span>
                                                        <span>Đang giao</span>
                                                    </span>
                                                </c:when>
                                                <c:when test="${o.status eq 'COMPLETED'}">
                                                    <span class="badge-status completed">
                                                        <span class="status-dot"></span>
                                                        <span>Hoàn thành</span>
                                                    </span>
                                                </c:when>
                                                <c:when test="${o.status eq 'CANCELLED'}">
                                                    <span class="badge-status cancelled">
                                                        <span class="status-dot"></span>
                                                        <span>Đã hủy</span>
                                                    </span>
                                                </c:when>
                                                <c:when test="${o.status eq 'PENDING'}">
                                                    <span class="badge-status" style="background:#fef9c3;color:#854d0e;border:1px solid #fef08a;">
                                                        <span class="status-dot" style="background:#eab308;"></span>
                                                        <span>Chờ thanh toán</span>
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-secondary">${o.statusLabel}</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="col-actions">
                                            <div class="d-flex align-items-center justify-content-center gap-1 flex-nowrap">
                                                <a href="${pageContext.request.contextPath}/bill?orderId=${o.id}" target="_blank" class="btn-action-bill" title="Mở xem và in hóa đơn">
                                                    <span class="material-symbols-outlined" style="font-size: 15px;">receipt_long</span>
                                                    <span>Hóa đơn</span>
                                                </a>
                                                <c:if test="${o.orderType eq 'ONLINE' && o.status eq 'READY'}">
                                                    <button type="button" class="btn-action-deliver-order" onclick="deliverOrder(${o.id})" title="Chuyển sang Đang giao hàng">
                                                        <span class="material-symbols-outlined" style="font-size: 15px;">delivery_dining</span>
                                                        <span>Giao</span>
                                                    </button>
                                                </c:if>
                                                <c:if test="${o.status eq 'READY' || o.status eq 'DELIVERING'}">
                                                    <button type="button" class="btn-action-complete-order" onclick="completeOrder(${o.id})" title="Đánh dấu hoàn tất">
                                                        <span class="material-symbols-outlined" style="font-size: 15px;">check_circle</span>
                                                        <span>Hoàn tất</span>
                                                    </button>
                                                </c:if>
                                                <c:if test="${o.status eq 'PENDING' || o.status eq 'CONFIRMED'}">
                                                    <button type="button" class="btn-action-cancel-order" onclick="cancelOrder(${o.id})" title="Hủy đơn hàng này">
                                                        <span class="material-symbols-outlined" style="font-size: 15px;">close</span>
                                                        <span>Hủy</span>
                                                    </button>
                                                </c:if>
                                            </div>
                                        </td>
                                    </tr>
                                    </c:forEach>
                                    <tr id="noFilterResultsRow" style="display: none;">
                                        <td colspan="9" class="text-center py-5">
                                            <div class="d-flex flex-column align-items-center justify-content-center text-muted">
                                                <span class="material-symbols-outlined mb-2" style="font-size: 40px; color: var(--slate-300);">search_off</span>
                                                <div class="fw-bold fs-6 text-slate-700">Không tìm thấy đơn hàng nào phù hợp</div>
                                                <div class="small text-slate-400 mt-1">Thử thay đổi từ khóa hoặc bộ lọc trạng thái</div>
                                            </div>
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <tr>
                                        <td colspan="9" class="text-center py-5">
                                            <div class="d-flex flex-column align-items-center justify-content-center text-muted">
                                                <span class="material-symbols-outlined mb-2" style="font-size: 48px; color: var(--slate-300);">inbox</span>
                                                <div class="fw-bold fs-6 text-slate-700">Chưa có đơn hàng nào tại quầy hôm nay</div>
                                                <div class="small text-slate-400 mt-1">Các đơn hàng phát sinh trong ca làm việc sẽ hiển thị tại đây</div>
                                            </div>
                                        </td>
                                    </tr>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
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
                        <div class="pay-modal-icon-badge">
                            <span class="material-symbols-outlined" style="font-size: 20px;">payments</span>
                        </div>
                        <div>
                            <div class="fw-bold text-dark" style="font-size: 15px; line-height: 1.2;">Xác nhận thanh toán tại quầy</div>
                            <div class="text-muted" style="font-size: 11.5px; font-weight: 500;">Bán hàng trực tiếp & thu ngân</div>
                        </div>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-3">
                    <!-- Banner Total -->
                    <div class="pay-total-banner">
                        <div>
                            <div class="pay-total-label">TỔNG TIỀN PHẢI THU</div>
                            <div class="pay-total-value" id="payModalTotalDisplay">0đ</div>
                        </div>
                        <div class="pay-order-badge" id="payModalSummaryDesc">
                            <span class="material-symbols-outlined" style="font-size: 14px;">storefront</span>
                            <span>Dùng tại quán</span>
                        </div>
                    </div>

                    <!-- Method Tabs -->
                    <div class="pay-method-tabs">
                        <button type="button" class="pay-method-btn active" id="btnMethodCash" onclick="selectPaymentMethod('CASH')">
                            <span class="material-symbols-outlined" style="font-size: 18px;">payments</span>
                            <span>Tiền mặt</span>
                        </button>
                        <button type="button" class="pay-method-btn" id="btnMethodQR" onclick="selectPaymentMethod('BANK_TRANSFER')">
                            <span class="material-symbols-outlined" style="font-size: 18px;">qr_code_2</span>
                            <span>VietQR Quét mã</span>
                        </button>
                    </div>

                    <!-- Tab CASH -->
                    <div id="panelCashPayment">
                        <div class="mb-2">
                            <label class="form-label d-flex justify-content-between align-items-center mb-1">
                                <span class="small fw-bold text-secondary">Khách đưa (VNĐ):</span>
                                <span class="small text-muted">Chọn nhanh hoặc nhập số</span>
                            </label>
                            <div class="input-cash-wrapper">
                                <span class="material-symbols-outlined cash-input-icon">attach_money</span>
                                <input type="number" id="inputCashGiven" class="input-cash-field" placeholder="0" oninput="calculateChange()">
                                <span class="cash-currency-tag">VNĐ</span>
                            </div>
                        </div>
                        
                        <div class="quick-cash-row">
                            <button type="button" class="btn-quick-cash exact-cash" onclick="addQuickCash(0)">Đúng số tiền</button>
                            <button type="button" class="btn-quick-cash" onclick="setCashAmount(50000)">50.000đ</button>
                            <button type="button" class="btn-quick-cash" onclick="setCashAmount(100000)">100.000đ</button>
                            <button type="button" class="btn-quick-cash" onclick="setCashAmount(200000)">200.000đ</button>
                            <button type="button" class="btn-quick-cash" onclick="setCashAmount(500000)">500.000đ</button>
                            <button type="button" class="btn-quick-cash" onclick="setCashAmount(1000000)">1.000.000đ</button>
                        </div>

                        <div class="cash-change-card" id="boxCashChange">
                            <div class="d-flex align-items-center gap-2">
                                <span class="material-symbols-outlined text-success" id="iconCashChange" style="font-size: 22px;">change_circle</span>
                                <div>
                                    <div class="cash-change-title" id="titleCashChange">Tiền thừa trả khách</div>
                                    <div class="cash-change-sub" id="subCashChange">Khách đưa vừa đủ</div>
                                </div>
                            </div>
                            <div class="cash-change-amount text-success" id="lblCashChange">0đ</div>
                        </div>
                    </div>

                    <!-- Tab VIETQR -->
                    <div id="panelQRPayment" style="display: none;" class="qr-payment-panel">
                        <div class="qr-status-pill">
                            <span class="qr-pulse-dot"></span>
                            <span>Đang chờ khách quét mã thanh toán</span>
                        </div>

                        <div class="qr-card-wrapper">
                            <div class="qr-frame">
                                <img id="vietQrImg" src="" alt="VietQR" class="qr-image">
                            </div>
                            <div class="qr-scan-guide">
                                <span class="material-symbols-outlined" style="font-size: 15px;">qr_code_scanner</span>
                                <span>Mở ứng dụng Ngân hàng hoặc Ví điện tử để quét</span>
                            </div>
                        </div>

                        <div class="bank-details-card">
                            <div class="bank-row">
                                <span class="bank-label">Ngân hàng thụ hưởng:</span>
                                <span class="bank-val">MBBank (Quân Đội)</span>
                            </div>
                            <div class="bank-row">
                                <span class="bank-label">Số tài khoản:</span>
                                <div class="d-flex align-items-center gap-1">
                                    <span class="bank-val font-monospace fw-bold" id="bankStk">0987654321</span>
                                    <button type="button" class="btn-copy-stk" onclick="copyBankSTK()" title="Sao chép số tài khoản">
                                        <span class="material-symbols-outlined" style="font-size: 15px;">content_copy</span>
                                    </button>
                                </div>
                            </div>
                            <div class="bank-row">
                                <span class="bank-label">Chủ tài khoản:</span>
                                <span class="bank-val fw-bold" style="color: var(--aura-primary);">AURA CAFE SYSTEM</span>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer pay-modal-footer">
                    <button type="button" class="btn-pay-cancel" data-bs-dismiss="modal">Đóng</button>
                    <button type="button" class="btn-pay-submit" id="btnSubmitOrder" onclick="submitCounterOrder()">
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
            const btnCheckout = document.getElementById('btnCheckout');

            if (cartItems.length === 0) {
                listEl.innerHTML = `
                    <div class="empty-cart-state" id="emptyCartMessage">
                        <span class="material-symbols-outlined empty-cart-icon">shopping_cart</span>
                        <div>Chưa có món nào trong đơn</div>
                        <div class="small">Vui lòng chọn món từ menu bên trái</div>
                    </div>
                `;
                btnCheckout.disabled = true;
                document.getElementById('cartCountTotal').textContent = '0';
                updateSummary(0);
                return;
            }

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
                const resp = await fetch(`\${CONTEXT_PATH}/cashier?action=search-customer&phone=\${encodeURIComponent(phone)}`, {
                    headers: { 'Accept': 'application/json' }
                });
                if (resp.status === 401) {
                    alert('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');
                    window.location.href = `\${CONTEXT_PATH}/login`;
                    return;
                }
                const data = await resp.json();
                if (data.success && data.customer) {
                    currentCustomer = data.customer;
                    document.getElementById('custSearchBox').style.display = 'none';
                    document.getElementById('custInfoBox').style.display = 'block';
                    document.getElementById('custNameDisplay').textContent = currentCustomer.fullName;
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
            const summaryDescEl = document.getElementById('payModalSummaryDesc');
            if (summaryDescEl) {
                summaryDescEl.innerHTML = `<span class="material-symbols-outlined" style="font-size: 14px;">\${orderType === 'DINE_IN' ? 'storefront' : 'shopping_bag'}</span><span>\${orderType === 'DINE_IN' ? 'Dùng tại quán' : 'Mang về'}</span>`;
            }

            selectPaymentMethod('CASH');
            setCashAmount(finalTotal);
            paymentModalInstance.show();
        }

        function copyBankSTK() {
            navigator.clipboard.writeText('0987654321').then(() => {
                showToast('Đã sao chép STK: 0987654321');
            }).catch(() => {
                showToast('STK: 0987654321');
            });
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
            const changeEl = document.getElementById('lblCashChange');
            const titleEl = document.getElementById('titleCashChange');
            const subEl = document.getElementById('subCashChange');
            const iconEl = document.getElementById('iconCashChange');
            const boxEl = document.getElementById('boxCashChange');

            if (given >= finalTotal) {
                const change = given - finalTotal;
                changeEl.textContent = formatCurrency(change);
                changeEl.className = 'cash-change-amount text-success';
                if (titleEl) titleEl.textContent = 'Tiền thừa trả khách';
                if (subEl) subEl.textContent = (change === 0) ? 'Khách đưa vừa đủ' : 'Thối lại tiền mặt';
                if (iconEl) {
                    iconEl.textContent = 'check_circle';
                    iconEl.className = 'material-symbols-outlined text-success';
                }
                if (boxEl) {
                    boxEl.style.borderColor = '#bbf7d0';
                    boxEl.style.background = '#f0fdf4';
                }
            } else {
                const missing = finalTotal - given;
                changeEl.textContent = '-' + formatCurrency(missing);
                changeEl.className = 'cash-change-amount text-danger';
                if (titleEl) titleEl.textContent = 'Còn thiếu';
                if (subEl) subEl.textContent = 'Khách đưa chưa đủ tiền';
                if (iconEl) {
                    iconEl.textContent = 'error';
                    iconEl.className = 'material-symbols-outlined text-danger';
                }
                if (boxEl) {
                    boxEl.style.borderColor = '#fecaca';
                    boxEl.style.background = '#fef2f2';
                }
            }
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

            let res;
            try {
                const response = await fetch(`\${CONTEXT_PATH}/cashier?action=create-order`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                if (response.status === 401) {
                    alert('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');
                    window.location.href = `\${CONTEXT_PATH}/login`;
                    return;
                }

                res = await response.json();
            } catch (err) {
                console.error(err);
                alert('Lỗi kết nối máy chủ.');
                return;
            } finally {
                btnSubmit.disabled = false;
                btnSubmit.innerHTML = `<span class="material-symbols-outlined" style="font-size: 18px;">receipt</span><span>Hoàn tất & In hóa đơn</span>`;
            }

            if (res && res.success) {
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
            } else if (res) {
                alert(res.error || 'Có lỗi xảy ra khi tạo đơn.');
            }
        }

        // 10. Order Actions & Realtime Sync
        async function deliverOrder(orderId) {
            if (!confirm(`Xác nhận bắt đầu giao đơn hàng #\${orderId}?`)) {
                return;
            }

            try {
                const resp = await fetch(`\${CONTEXT_PATH}/cashier?action=deliver-order&orderId=\${orderId}`, {
                    method: 'POST',
                    headers: { 'Accept': 'application/json' }
                });
                if (resp.status === 401) {
                    alert('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');
                    window.location.href = `\${CONTEXT_PATH}/login`;
                    return;
                }
                const res = await resp.json();
                if (res.success) {
                    showToast(res.message || 'Đã chuyển đơn sang trạng thái Đang giao hàng.');
                    refreshOrdersList();
                } else {
                    alert(res.error || 'Không thể chuyển trạng thái giao hàng.');
                }
            } catch (e) {
                console.error(e);
                alert('Lỗi kết nối máy chủ.');
            }
        }

        async function completeOrder(orderId) {
            if (!confirm(`Xác nhận hoàn tất đơn hàng #\${orderId}?`)) {
                return;
            }

            try {
                const resp = await fetch(`\${CONTEXT_PATH}/cashier?action=complete-order&orderId=\${orderId}`, {
                    method: 'POST',
                    headers: { 'Accept': 'application/json' }
                });
                if (resp.status === 401) {
                    alert('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');
                    window.location.href = `\${CONTEXT_PATH}/login`;
                    return;
                }
                const res = await resp.json();
                if (res.success) {
                    showToast(res.message || 'Đã hoàn tất đơn hàng.');
                    refreshOrdersList();
                } else {
                    alert(res.error || 'Không thể hoàn tất đơn hàng.');
                }
            } catch (e) {
                console.error(e);
                alert('Lỗi kết nối máy chủ.');
            }
        }

        async function cancelOrder(orderId) {
            if (!confirm(`Bạn có chắc chắn muốn hủy đơn hàng #\${orderId}?`)) {
                return;
            }

            try {
                const resp = await fetch(`\${CONTEXT_PATH}/cashier?action=cancel-order&orderId=\${orderId}`, {
                    method: 'POST',
                    headers: { 'Accept': 'application/json' }
                });
                if (resp.status === 401) {
                    alert('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');
                    window.location.href = `\${CONTEXT_PATH}/login`;
                    return;
                }
                const res = await resp.json();
                if (res.success) {
                    showToast(res.message || 'Đã hủy đơn thành công');
                    refreshOrdersList();
                } else {
                    alert(res.error || 'Không thể hủy đơn.');
                }
            } catch (e) {
                console.error(e);
                alert('Lỗi kết nối máy chủ.');
            }
        }

        // Realtime Polling for Today's Orders (Every 5 seconds)
        let isRefreshingOrders = false;
        async function refreshOrdersList() {
            if (isRefreshingOrders) return;
            isRefreshingOrders = true;

            try {
                const resp = await fetch(`\${CONTEXT_PATH}/cashier?action=orders`, {
                    headers: { 'Accept': 'application/json' }
                });
                if (!resp.ok) return;
                const data = await resp.json();
                if (!data.success || !Array.isArray(data.orders)) return;

                const tbody = document.getElementById('ordersTableBody');
                if (!tbody) return;

                if (data.orders.length === 0) {
                    tbody.innerHTML = `
                        <tr>
                            <td colspan="9" class="text-center py-5">
                                <div class="d-flex flex-column align-items-center justify-content-center text-muted">
                                    <span class="material-symbols-outlined mb-2" style="font-size: 48px; color: var(--slate-300);">inbox</span>
                                    <div class="fw-bold fs-6 text-slate-700">Chưa có đơn hàng nào tại quầy hôm nay</div>
                                    <div class="small text-slate-400 mt-1">Các đơn hàng phát sinh trong ca làm việc sẽ hiển thị tại đây</div>
                                </div>
                            </td>
                        </tr>`;
                    return;
                }

                let html = '';
                for (const o of data.orders) {
                    html += renderOrderRowHtml(o);
                }

                html += `
                    <tr id="noFilterResultsRow" style="display: none;">
                        <td colspan="9" class="text-center py-5">
                            <div class="d-flex flex-column align-items-center justify-content-center text-muted">
                                <span class="material-symbols-outlined mb-2" style="font-size: 40px; color: var(--slate-300);">search_off</span>
                                <div class="fw-bold fs-6 text-slate-700">Không tìm thấy đơn hàng nào phù hợp</div>
                                <div class="small text-slate-400 mt-1">Thử thay đổi từ khóa hoặc bộ lọc trạng thái</div>
                            </div>
                        </td>
                    </tr>`;

                tbody.innerHTML = html;
                applyOrderFilters();
            } catch (err) {
                console.warn('Orders poll error:', err);
            } finally {
                isRefreshingOrders = false;
            }
        }

        function renderOrderRowHtml(o) {
            let typeBadgeHtml = '';
            if (o.orderType === 'ONLINE') {
                typeBadgeHtml = `
                    <span class="badge-type delivery">
                        <span class="material-symbols-outlined" style="font-size: 14px;">delivery_dining</span>
                        <span>Giao hàng</span>
                    </span>`;
            } else if (o.orderType === 'TAKEAWAY') {
                typeBadgeHtml = `
                    <span class="badge-type takeaway">
                        <span class="material-symbols-outlined" style="font-size: 14px;">takeout_dining</span>
                        <span>Mang về</span>
                    </span>`;
            } else {
                typeBadgeHtml = `
                    <span class="badge-type dine-in">
                        <span class="material-symbols-outlined" style="font-size: 14px;">storefront</span>
                        <span>Tại quán\${o.tableNumber ? ' (Bàn ' + escapeHtml(o.tableNumber) + ')' : ''}</span>
                    </span>`;
            }

            let custHtml = `
                <div class="fw-semibold text-slate-800 d-flex align-items-center gap-1">
                    <span class="material-symbols-outlined text-slate-400" style="font-size: 16px;">person</span>
                    <span class="text-truncate" style="max-width: 130px;" title="\${escapeHtml(o.recipientName)}">
                        \${escapeHtml(o.recipientName)}
                    </span>
                </div>`;
            if (o.recipientPhone) {
                custHtml += `<div style="font-size: 11px; color: var(--slate-500); padding-left: 20px;">\${escapeHtml(o.recipientPhone)}</div>`;
            }
            if (o.deliveryAddress) {
                custHtml += `<div style="font-size: 11px; color: var(--slate-400); padding-left: 20px;" class="text-truncate" style="max-width: 130px;" title="\${escapeHtml(o.deliveryAddress)}">\${escapeHtml(o.deliveryAddress)}</div>`;
            }

            let paymentBadgeHtml = '';
            if (o.paymentMethodLabel && o.paymentMethodLabel.includes('VietQR')) {
                paymentBadgeHtml = `
                    <span class="badge-payment qr">
                        <span class="material-symbols-outlined" style="font-size: 14px;">qr_code_2</span>
                        <span>VietQR</span>
                    </span>`;
            } else {
                paymentBadgeHtml = `
                    <span class="badge-payment cash">
                        <span class="material-symbols-outlined" style="font-size: 14px;">payments</span>
                        <span>Tiền mặt</span>
                    </span>`;
            }

            let statusBadgeHtml = '';
            if (o.status === 'CONFIRMED') {
                statusBadgeHtml = `<span class="badge-status confirmed"><span class="status-dot"></span><span>Đã thanh toán</span></span>`;
            } else if (o.status === 'PREPARING') {
                statusBadgeHtml = `<span class="badge-status preparing"><span class="status-dot"></span><span>Đang pha chế</span></span>`;
            } else if (o.status === 'READY') {
                statusBadgeHtml = `<span class="badge-status ready"><span class="status-dot"></span><span>Sẵn sàng</span></span>`;
            } else if (o.status === 'DELIVERING') {
                statusBadgeHtml = `<span class="badge-status delivering"><span class="status-dot"></span><span>Đang giao</span></span>`;
            } else if (o.status === 'COMPLETED') {
                statusBadgeHtml = `<span class="badge-status completed"><span class="status-dot"></span><span>Hoàn thành</span></span>`;
            } else if (o.status === 'CANCELLED') {
                statusBadgeHtml = `<span class="badge-status cancelled"><span class="status-dot"></span><span>Đã hủy</span></span>`;
            } else if (o.status === 'PENDING') {
                statusBadgeHtml = `<span class="badge-status" style="background:#fef9c3;color:#854d0e;border:1px solid #fef08a;"><span class="status-dot" style="background:#eab308;"></span><span>Chờ thanh toán</span></span>`;
            } else {
                statusBadgeHtml = `<span class="badge bg-secondary">\${escapeHtml(o.statusLabel || o.status)}</span>`;
            }

            let actionBtnsHtml = `
                <a href="\${CONTEXT_PATH}/bill?orderId=\${o.id}" target="_blank" class="btn-action-bill" title="Mở xem và in hóa đơn">
                    <span class="material-symbols-outlined" style="font-size: 15px;">receipt_long</span>
                    <span>Hóa đơn</span>
                </a>`;

            if (o.orderType === 'ONLINE' && o.status === 'READY') {
                actionBtnsHtml += `
                    <button type="button" class="btn-action-deliver-order" onclick="deliverOrder(\${o.id})" title="Chuyển sang Đang giao hàng">
                        <span class="material-symbols-outlined" style="font-size: 15px;">delivery_dining</span>
                        <span>Giao</span>
                    </button>`;
            }

            if (o.status === 'READY' || o.status === 'DELIVERING') {
                actionBtnsHtml += `
                    <button type="button" class="btn-action-complete-order" onclick="completeOrder(\${o.id})" title="Đánh dấu hoàn tất">
                        <span class="material-symbols-outlined" style="font-size: 15px;">check_circle</span>
                        <span>Hoàn tất</span>
                    </button>`;
            }

            if (o.status === 'PENDING' || o.status === 'CONFIRMED') {
                actionBtnsHtml += `
                    <button type="button" class="btn-action-cancel-order" onclick="cancelOrder(\${o.id})" title="Hủy đơn hàng này">
                        <span class="material-symbols-outlined" style="font-size: 15px;">close</span>
                        <span>Hủy</span>
                    </button>`;
            }

            const searchStr = `\${escapeHtml(o.orderCode)} \${escapeHtml(o.recipientName)} \${escapeHtml(o.recipientPhone)} \${escapeHtml(o.itemSummary)} \${escapeHtml(o.deliveryAddress)}`;

            return `
                <tr class="order-table-row" 
                    data-type="\${escapeHtml(o.orderType)}" 
                    data-status="\${escapeHtml(o.status)}" 
                    data-search="\${searchStr}">
                    <td>
                        <span class="order-code-badge" onclick="copyOrderCode('\${escapeHtml(o.orderCode)}')" title="Bấm để sao chép mã" style="cursor: pointer;">
                            <span class="material-symbols-outlined" style="font-size: 14px; color: var(--slate-500);">tag</span>
                            <span>\${escapeHtml(o.orderCode)}</span>
                        </span>
                    </td>
                    <td>
                        <div class="fw-semibold text-slate-800">\${escapeHtml(o.createdTimeText)}</div>
                        <div style="font-size: 11px; color: var(--slate-400);">Hôm nay</div>
                    </td>
                    <td>\${typeBadgeHtml}</td>
                    <td>\${custHtml}</td>
                    <td>
                        <div class="order-items-snippet" title="\${escapeHtml(o.itemSummary)}">
                            <div class="order-items-text">\${escapeHtml(o.itemSummary)}</div>
                            <div class="order-items-qty">\${o.itemCount} món</div>
                        </div>
                    </td>
                    <td class="text-end">
                        <div class="order-price-val">\${escapeHtml(o.formattedFinalAmount)}</div>
                    </td>
                    <td>\${paymentBadgeHtml}</td>
                    <td>\${statusBadgeHtml}</td>
                    <td class="col-actions">
                        <div class="d-flex align-items-center justify-content-center gap-1 flex-nowrap">
                            \${actionBtnsHtml}
                        </div>
                    </td>
                </tr>`;
        }

        // Start polling orders every 5 seconds
        setInterval(refreshOrdersList, 5000);

        // 11. Instant Orders Filter
        let currentOrderTypeFilter = 'ALL';
        let currentOrderStatusFilter = 'ALL';

        function setOrderTypeFilter(type, btn) {
            currentOrderTypeFilter = type;
            document.querySelectorAll('.btn-filter-type').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            applyOrderFilters();
        }

        function setOrderStatusFilter(status, btn) {
            currentOrderStatusFilter = status;
            document.querySelectorAll('.btn-status-pill').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            applyOrderFilters();
        }

        function applyOrderFilters() {
            const query = (document.getElementById('orderSearchInput')?.value || '').trim().toLowerCase();
            const rows = document.querySelectorAll('.order-table-row');
            let visibleCount = 0;

            rows.forEach(row => {
                const type = row.getAttribute('data-type') || '';
                const status = row.getAttribute('data-status') || '';
                const searchData = (row.getAttribute('data-search') || '').toLowerCase();

                const matchType = (currentOrderTypeFilter === 'ALL' || type === currentOrderTypeFilter);
                const matchStatus = (currentOrderStatusFilter === 'ALL' || status === currentOrderStatusFilter);
                const matchQuery = (!query || searchData.includes(query));

                if (matchType && matchStatus && matchQuery) {
                    row.style.display = '';
                    visibleCount++;
                } else {
                    row.style.display = 'none';
                }
            });

            const emptyRow = document.getElementById('noFilterResultsRow');
            if (emptyRow) {
                emptyRow.style.display = (visibleCount === 0 && rows.length > 0) ? '' : 'none';
            }
        }

        function copyOrderCode(code) {
            if (navigator.clipboard) {
                navigator.clipboard.writeText(code).then(() => {
                    showToast('Đã sao chép mã đơn ' + code);
                }).catch(() => {
                    showToast('Mã đơn: ' + code);
                });
            } else {
                showToast('Mã đơn: ' + code);
            }
        }

        // Utility Helpers
        function formatCurrency(amount) {
            return new Intl.NumberFormat('vi-VN').format(Math.round(amount)) + 'đ';
        }

        function escapeHtml(str) {
            if (!str) return '';
            return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
        }

        function showToast(msg) {
            const container = document.getElementById('toastContainer');
            if (!container) return;
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
