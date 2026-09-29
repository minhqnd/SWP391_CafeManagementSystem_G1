<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>An Nam Coffee Roasters | Đăng nhập</title>
    
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    
    <style>
        :root {
            --primary-color: #A62122; /* Đỏ đô - màu quán cafe */
            --primary-hover: #821516;
        }
        
        body {
            font-family: 'Outfit', sans-serif;
            background-color: #f8f9fa;
            min-height: 100vh;
            display: flex;
            align-items: center;
        }

        .login-card {
            border: none;
            border-radius: 12px;
            box-shadow: 0 10px 40px -10px rgba(0,0,0,0.08);
            overflow: hidden;
        }
        
        .login-card::before {
            content: "";
            position: absolute;
            top: 0; left: 0; right: 0;
            height: 6px;
            background: linear-gradient(90deg, var(--primary-color), #e63946);
            z-index: 10;
        }

        .logo-icon {
            width: 60px;
            height: 60px;
            background-color: var(--primary-color);
            color: white;
            border-radius: 50%;
            display: inline-flex;
            justify-content: center;
            align-items: center;
            margin-bottom: 1rem;
        }
        
        .form-control:focus {
            border-color: var(--primary-color);
            box-shadow: 0 0 0 0.25rem rgba(166, 33, 34, 0.25);
        }

        .btn-primary {
            background-color: var(--primary-color);
            border-color: var(--primary-color);
            font-weight: 600;
            padding: 0.75rem;
        }

        .btn-primary:hover, .btn-primary:focus, .btn-primary:active {
            background-color: var(--primary-hover) !important;
            border-color: var(--primary-hover) !important;
        }
        
        .hint-box {
            background-color: #f1f3f5;
            border-left: 4px solid #6c757d;
            border-radius: 6px;
        }
    </style>
</head>
<body>

<div class="container">
    <div class="row justify-content-center">
        <div class="col-12 col-sm-10 col-md-8 col-lg-5">
            <div class="card login-card position-relative p-4 p-sm-5">
                
                <div class="text-center mb-4">
                    <div class="logo-icon">
                        <svg xmlns="http://www.w3.org/2000/svg" width="30" height="30" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M18 8h1a4 4 0 0 1 0 8h-1"></path>
                            <path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"></path>
                            <line x1="6" y1="1" x2="6" y2="4"></line>
                            <line x1="10" y1="1" x2="10" y2="4"></line>
                            <line x1="14" y1="1" x2="14" y2="4"></line>
                        </svg>
                    </div>
                    <h2 class="fw-bold fs-3 text-dark mb-1">Chào mừng trở lại!</h2>
                    <p class="text-muted">Đăng nhập vào hệ thống An Nam Coffee</p>
                </div>

                <!-- Bắt lỗi từ Servlet trả về -->
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert alert-danger text-center p-2 mb-4" role="alert">
                        <%= request.getAttribute("error") %>
                    </div>
                <% } %>

                <form action="${pageContext.request.contextPath}/login" method="POST">
                    <div class="mb-3">
                        <label for="username" class="form-label fw-semibold">Tên đăng nhập</label>
                        <input type="text" class="form-control form-control-lg fs-6" id="username" name="username" placeholder="Nhập tên đăng nhập" required autocomplete="off">
                    </div>

                    <div class="mb-4">
                        <label for="password" class="form-label fw-semibold">Mật khẩu</label>
                        <input type="password" class="form-control form-control-lg fs-6" id="password" name="password" placeholder="••••••••" required>
                    </div>

                    <button type="submit" class="btn btn-primary w-100 mb-4">Đăng nhập</button>
                </form>

                <div class="hint-box p-3 small text-muted">
                    <strong class="d-block mb-2 text-dark">💡 Tài khoản Test (Pass: 123456):</strong>
                    <div class="row">
                        <div class="col-6">
                            <ul class="mb-0 ps-3">
                                <li>admin</li>
                                <li>cashier</li>
                                <li>barista</li>
                            </ul>
                        </div>
                        <div class="col-6">
                            <ul class="mb-0 ps-3">
                                <li>delivery</li>
                                <li>customer</li>
                            </ul>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>
</div>

<!-- Bootstrap 5 JS Bundle -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.css"></script>
</body>
</html>
