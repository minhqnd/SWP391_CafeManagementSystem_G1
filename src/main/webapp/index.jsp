<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="dao.ProductDAO" %>
<%@ page import="dao.CategoryDAO" %>
<%@ page import="model.Product" %>
<%@ page import="model.Category" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.DecimalFormat" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Aura Café | Trang Chủ</title>
    
    <!-- Google Fonts & Icons -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet"/>
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet"/>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Shared Product Card & Modal CSS -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/product-card.css">
    
    <style>
        :root {
            --primary: #900b1a;
            --primary-container: #b2292e;
            --secondary: #555f71;
            --surface-container-lowest: #ffffff;
            --surface-container-low: #f8f9ff;
            --surface-container: #eff4ff;
            --surface-container-high: #dee9fc;
            --surface-container-highest: #d1e0fa;
            --tertiary: #6b5c3f;
            --tertiary-container: #95815a;
        }
        body { font-family: 'Inter', sans-serif; background-color: var(--surface-container-lowest); color: #1a1c1e; overflow-x: hidden; }
        
        .bg-primary-custom { background-color: var(--primary) !important; color: white !important; }
        .text-primary-custom { color: var(--primary) !important; }
        .text-secondary-custom { color: var(--secondary) !important; }
        
        /* Hero Section */
        .hero-section { background-color: var(--surface-container-low); position: relative; padding: 5rem 0; overflow: hidden; }
        .blur-blob-1 { position: absolute; right: -5rem; top: -6rem; width: 24rem; height: 24rem; border-radius: 50%; background-color: rgba(144, 11, 26, 0.1); filter: blur(3rem); pointer-events: none; }
        .blur-blob-2 { position: absolute; left: 25%; bottom: -4rem; width: 20rem; height: 20rem; border-radius: 50%; background-color: rgba(149, 129, 90, 0.1); filter: blur(3rem); pointer-events: none; }
        
        .hero-title { font-size: 3.25rem; font-weight: 800; line-height: 1.15; letter-spacing: -0.025em; color: #0f172a; }
        @media (max-width: 768px) { .hero-title { font-size: 2.25rem; } }
        
        .btn-custom-primary { background-color: var(--primary); color: white; border-radius: 0.75rem; font-weight: 600; padding: 0.85rem 1.75rem; transition: all 0.2s ease; border: none; display: inline-flex; align-items: center; justify-content: center; text-decoration: none; box-shadow: 0 4px 14px rgba(144, 11, 26, 0.2); }
        .btn-custom-primary:hover { opacity: 0.95; color: white; transform: translateY(-1px); box-shadow: 0 6px 20px rgba(144, 11, 26, 0.28); }
        .btn-custom-secondary { background-color: #ffffff; color: #1e293b; border-radius: 0.75rem; font-weight: 600; padding: 0.85rem 1.75rem; transition: all 0.2s ease; border: 1px solid #e2e8f0; display: inline-flex; align-items: center; justify-content: center; text-decoration: none; box-shadow: 0 2px 8px rgba(0,0,0,0.03); }
        .btn-custom-secondary:hover { background-color: #f8fafc; color: #0f172a; border-color: #cbd5e1; transform: translateY(-1px); }
        
        .stats-box { background: #ffffff; border: 1px solid rgba(0, 0, 0, 0.06); border-radius: 1rem; padding: 1.25rem 1.5rem; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
        .stat-value { font-size: 1.35rem; font-weight: 800; color: var(--primary); letter-spacing: -0.02em; line-height: 1.2; margin-bottom: 0.25rem; }
        .stat-label { font-size: 0.8rem; color: #64748b; font-weight: 500; }
        
        .hero-img-container { position: relative; height: 440px; border-radius: 1.5rem; overflow: hidden; box-shadow: 0 1rem 3rem rgba(0,0,0,0.1); }
        .hero-img-bg { background-size: cover; background-position: center; width: 100%; height: 100%; transition: transform 0.7s; }
        .hero-img-container:hover .hero-img-bg { transform: scale(1.05); }
        .hero-img-overlay { position: absolute; inset: 0; background: linear-gradient(to top, rgba(0,0,0,0.85) 0%, rgba(0,0,0,0) 65%); display: flex; flex-direction: column; justify-content: flex-end; padding: 2rem; color: white; }
        
        
        /* Category Section */
        .category-section { background-color: var(--surface-container-low); padding: 5rem 0; }
        .cat-card {
            background: #ffffff;
            border-radius: 1.25rem;
            overflow: hidden;
            border: 1px solid var(--surface-container-high);
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
            text-decoration: none;
            display: flex;
            flex-direction: column;
            height: 100%;
            transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
        }
        .cat-card:hover {
            transform: translateY(-6px);
            box-shadow: 0 16px 32px rgba(144, 11, 26, 0.1);
            border-color: rgba(144, 11, 26, 0.35);
        }
        .cat-img-box {
            height: 170px;
            position: relative;
            overflow: hidden;
            background-color: #f1f5f9;
        }
        .cat-img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
        }
        .cat-card:hover .cat-img {
            transform: scale(1.08);
        }
        .cat-icon-badge {
            position: absolute;
            bottom: 12px;
            left: 14px;
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(8px);
            color: var(--primary);
            width: 38px;
            height: 38px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
        }
        .cat-body {
            padding: 1.25rem;
            display: flex;
            flex-direction: column;
            flex-grow: 1;
            justify-content: space-between;
        }
        .cat-title {
            font-size: 1.125rem;
            font-weight: 700;
            color: #1a1c1e;
            margin-bottom: 0.25rem;
            transition: color 0.2s;
        }
        .cat-card:hover .cat-title {
            color: var(--primary);
        }
        .cat-meta {
            font-size: 0.85rem;
            color: #64748b;
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-top: 0.75rem;
            padding-top: 0.75rem;
            border-top: 1px solid #f1f5f9;
        }
        
        /* Store Section */
        .store-section {
            padding: 4.5rem 0;
        }
        .store-card {
            background: #ffffff;
            border-radius: 1.5rem;
            overflow: hidden;
            border: 1px solid var(--surface-container-high, #e2e8f0);
            box-shadow: 0 8px 30px rgba(0, 0, 0, 0.04);
            transition: box-shadow 0.3s ease;
        }
        .store-card:hover {
            box-shadow: 0 16px 40px rgba(0, 0, 0, 0.08);
        }
        .store-img-container {
            position: relative;
            height: 100%;
            min-height: 400px;
            overflow: hidden;
        }
        .store-img {
            width: 100%;
            height: 100%;
            min-height: 400px;
            object-fit: cover;
            transition: transform 0.6s ease;
        }
        .store-card:hover .store-img {
            transform: scale(1.03);
        }
        .btn-store-explore {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            padding: 0.85rem 2.25rem;
            border-radius: 50rem;
            border: 1.5px solid var(--primary);
            color: var(--primary);
            background: transparent;
            font-weight: 700;
            font-size: 0.85rem;
            text-transform: uppercase;
            letter-spacing: 1.5px;
            text-decoration: none;
            transition: all 0.25s ease;
        }
        .btn-store-explore:hover {
            background-color: var(--primary);
            color: #ffffff;
            transform: translateY(-2px);
            box-shadow: 0 6px 18px rgba(144, 11, 26, 0.2);
        }
        
        /* CTA Section */
        .cta-section {
            margin-bottom: 5rem;
        }
        .cta-box {
            background: linear-gradient(135deg, #900b1a 0%, #6e0814 100%);
            color: white;
            border-radius: 1.75rem;
            padding: 3.5rem;
            position: relative;
            overflow: hidden;
            box-shadow: 0 20px 40px rgba(144, 11, 26, 0.18);
        }
    </style>
</head>
<body>

    <!-- Shared Header Navbar Component -->
    <jsp:include page="/components/header.jsp">
        <jsp:param name="activePage" value="home" />
    </jsp:include>

    <!-- Hero Showcase Section -->
    <section class="hero-section">
        <div class="blur-blob-1"></div>
        <div class="blur-blob-2"></div>
        <div class="container position-relative z-1">
            <div class="row g-5 align-items-center">
                <div class="col-lg-7">
                    <div class="mb-4">
                        <span class="text-secondary-custom fw-bold text-uppercase d-block mb-2" style="letter-spacing: 2px; font-size: 0.8rem;">Chào mừng bạn đến với</span>
                        <h1 class="hero-title mb-3">
                            Hương vị chuẩn mực.<br/>
                            <span class="text-primary-custom">Đặc quyền hội viên.</span>
                        </h1>
                        <p class="text-muted mb-0" style="font-size: 1.05rem; line-height: 1.6; max-width: 520px;">
                            Khám phá không gian cà phê thủ công chọn lọc từ những nông trại cao nguyên danh tiếng, cùng đặc quyền tích luỹ điểm thưởng và ưu đãi hấp dẫn dành riêng cho thành viên mới.
                        </p>
                    </div>
                    
                    <div class="d-flex flex-wrap gap-3 mb-5">
                        <a href="${pageContext.request.contextPath}/menu" class="btn-custom-primary">
                            Khám phá Menu
                        </a>
                        <% if(session.getAttribute("user") == null) { %>
                            <button class="btn-custom-secondary" data-bs-toggle="modal" data-bs-target="#authModal">
                                Đăng ký thành viên
                            </button>
                        <% } else { %>
                            <a href="${pageContext.request.contextPath}/profile" class="btn-custom-secondary">
                                Thẻ thành viên của bạn
                            </a>
                        <% } %>
                    </div>
                    
                    <div class="stats-box row g-0 text-center text-md-start">
                        <div class="col-4 pe-3">
                            <div class="stat-value">100%</div>
                            <div class="stat-label">Hạt Arabica Cầu Đất</div>
                        </div>
                        <div class="col-4 px-3 border-start">
                            <div class="stat-value">4.9 / 5</div>
                            <div class="stat-label">Đánh giá trải nghiệm</div>
                        </div>
                        <div class="col-4 ps-3 border-start">
                            <div class="stat-value">15 phút</div>
                            <div class="stat-label">Giao tận nơi giữ nhiệt</div>
                        </div>
                    </div>
                </div>
                
                <div class="col-lg-5 position-relative mt-5 mt-lg-0">
                    <div class="hero-img-container">
                        <div class="hero-img-bg" style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuBw-dbJgXjvCR5nkOrwZ8VgbS5GY2OEyRxEbOPz5Icbwto9r4_yy3ThjTmDPVYgkVS0qvw9zgWOIgZyovscgGXFfWYGZIWnz4J-kEojbOR_3UIVRm6Pc2K3JfHrvidn2O5NIY6kYDmJT7_FHzfZXsvm5SPBgxa8HNtPCnWXgdeqGGBi8jSUbqQkSy9_zS1iUSwY3th8A9HlEIBbs8WrZi_JfRhmcTX_OGMjA0Fi-G89MqKh9nlG6OfL');"></div>
                        <div class="hero-img-overlay">
                            <span class="text-uppercase fw-bold mb-1" style="color: #ffb4a9; font-size: 0.75rem; letter-spacing: 1.5px;">Món Signature Aura</span>
                            <h2 class="fw-bold text-white fs-3 mb-2">Cà Phê Muối Aura</h2>
                            <p class="text-white-50 mb-0" style="font-size: 0.875rem; line-height: 1.5;">Lớp kem béo mặn đặc chế hòa quyện cà phê phin nguyên bản từ cao nguyên Lâm Đồng.</p>
                        </div>
                    </div>
                    
                </div>
            </div>
        </div>
    </section>

    <!-- Best Sellers Section -->
    <section class="py-5">
        <div class="container py-4">
            <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-end mb-4 gap-3">
                <div>
                    <div class="text-primary-custom fw-bold mb-1">
                        <span class="text-uppercase" style="font-size: 0.75rem; letter-spacing: 1.5px;">Ưa chuộng nhất</span>
                    </div>
                    <h2 class="fw-bold text-dark fs-2 mb-2">Sản phẩm bán chạy trong tuần</h2>
                    <p class="text-secondary-custom mb-0">Những công thức hương vị được yêu thích và đặt nhiều nhất bởi tín đồ sành cà phê.</p>
                </div>
                <a href="${pageContext.request.contextPath}/menu" class="text-primary-custom fw-semibold text-decoration-none d-flex align-items-center gap-1">
                    Xem tất cả danh mục <span class="material-symbols-outlined fs-5">chevron_right</span>
                </a>
            </div>
            
            <div class="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-lg-4 g-4">
                <%
                    ProductDAO productDAO = new ProductDAO();
                    List<Product> productsList = productDAO.getAllProducts();
                    request.setAttribute("bestSellerProducts", productsList);
                    DecimalFormat formatter = new DecimalFormat("###,###,###");
                %>
                <c:forEach var="p" items="${bestSellerProducts}">
                    <div class="col">
                        <c:set var="product" value="${p}" scope="request" />
                        <jsp:include page="/components/product_card.jsp" />
                    </div>
                </c:forEach>
            </div>
        </div>
    </section>

    <!-- Menu Categories Section -->
    <section class="category-section">
        <div class="container">
            <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-end mb-4 gap-3">
                <div>
                    <div class="text-primary-custom fw-bold mb-2">
                        <span class="text-uppercase" style="font-size: 0.75rem; letter-spacing: 1.5px;">Khám phá thực đơn</span>
                    </div>
                    <h2 class="fw-bold text-dark fs-2 mb-2">Danh mục đồ uống & Món ngon</h2>
                    <p class="text-secondary-custom mb-0">Lựa chọn hương vị tinh tế theo từng nhóm món yêu thích pha chế thủ công tại Aura Café.</p>
                </div>
                <a href="${pageContext.request.contextPath}/menu" class="text-primary-custom fw-semibold text-decoration-none d-flex align-items-center gap-1">
                    Xem tất cả menu <span class="material-symbols-outlined fs-5">chevron_right</span>
                </a>
            </div>
            
            <div class="row row-cols-1 row-cols-sm-2 row-cols-lg-3 g-4">
                <%
                    CategoryDAO categoryDAO = new CategoryDAO();
                    List<Category> landingCategories = categoryDAO.getAllActiveCategoriesWithCount();
                    request.setAttribute("landingCategories", landingCategories);
                %>
                <c:forEach var="cat" items="${landingCategories}">
                    <c:set var="catImg" value="https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80" />
                    <c:set var="catDesc" value="Hương vị trọn vẹn được tuyển chọn kỹ lưỡng." />
                    <c:choose>
                        <c:when test="${cat.id == 1}">
                            <c:set var="catImg" value="https://images.unsplash.com/photo-1485808191679-5f86510681a2?w=600&auto=format&fit=crop&q=80" />
                            <c:set var="catDesc" value="Espresso, Caramel Macchiato đậm đà chuẩn gu hiện đại." />
                        </c:when>
                        <c:when test="${cat.id == 2}">
                            <c:set var="catImg" value="https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80" />
                            <c:set var="catDesc" value="Cà phê muối, sữa đá Sài Gòn ủ phin nguyên bản ngọt êm." />
                        </c:when>
                        <c:when test="${cat.id == 3}">
                            <c:set var="catImg" value="https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=600&auto=format&fit=crop&q=80" />
                            <c:set var="catDesc" value="Trà đào cam sả, hoa quả nhiệt đới tươi mát thanh nhiệt." />
                        </c:when>
                        <c:when test="${cat.id == 4}">
                            <c:set var="catImg" value="https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=600&auto=format&fit=crop&q=80" />
                            <c:set var="catDesc" value="Matcha Uji thơm bùi, đá xay mát lạnh đánh tan ngày hè." />
                        </c:when>
                        <c:when test="${cat.id == 5}">
                            <c:set var="catImg" value="https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=600&auto=format&fit=crop&q=80" />
                            <c:set var="catDesc" value="Bánh Croissant nướng bơ Pháp, cheesecake mềm béo hấp dẫn." />
                        </c:when>
                        <c:when test="${cat.id == 6}">
                            <c:set var="catImg" value="https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?w=600&auto=format&fit=crop&q=80" />
                            <c:set var="catDesc" value="Cold brew ủ lạnh 16h đóng chai tiện lợi mang đi bất cứ đâu." />
                        </c:when>
                    </c:choose>

                    <div class="col">
                        <a href="${pageContext.request.contextPath}/menu?category=${cat.id}" class="cat-card">
                            <div class="cat-img-box">
                                <img src="${catImg}" alt="${cat.name}" class="cat-img" loading="lazy" />
                                <div class="cat-icon-badge">
                                    <span class="material-symbols-outlined fs-5">${not empty cat.icon ? cat.icon : 'local_cafe'}</span>
                                </div>
                            </div>
                            <div class="cat-body">
                                <div>
                                    <h3 class="cat-title">${cat.name}</h3>
                                    <p class="text-secondary-custom mb-0 small" style="line-height: 1.5;">${catDesc}</p>
                                </div>
                                <div class="cat-meta">
                                    <span class="fw-semibold text-primary-custom">${cat.productCount} món có sẵn</span>
                                    <span class="d-inline-flex align-items-center text-primary-custom fw-semibold small">
                                        Xem ngay <span class="material-symbols-outlined fs-6 ms-1">arrow_forward</span>
                                    </span>
                                </div>
                            </div>
                        </a>
                    </div>
                </c:forEach>
            </div>
        </div>
    </section>

    <!-- Store Near You Section -->
    <section class="store-section">
        <div class="container">
            <div class="store-card row g-0 align-items-center">
                <div class="col-lg-6 p-4 p-md-5 text-center text-lg-start d-flex flex-column justify-content-center">
                    <div class="text-primary-custom fw-bold mb-2">
                        <span class="text-uppercase" style="font-size: 0.75rem; letter-spacing: 2px;">Không gian & Chi nhánh</span>
                    </div>
                    <h2 class="display-6 fw-bold text-dark mb-3" style="line-height: 1.25;">
                        Cửa Hàng<br/>
                        <span class="text-primary-custom">Aura Gần Bạn</span>
                    </h2>
                    <p class="text-secondary-custom mb-4" style="font-size: 1.05rem; line-height: 1.6; max-width: 460px;">
                        Bạn ở đâu, có Aura ở đó! Khám phá không gian cà phê ấm cúng và thưởng thức những ly cà phê chuẩn vị gần bạn nhất.
                    </p>
                    <div class="d-flex flex-wrap gap-3 justify-content-center justify-content-lg-start">
                        <a href="${pageContext.request.contextPath}/menu" class="btn-store-explore">
                            Khám phá ngay
                        </a>
                    </div>
                </div>
                <div class="col-lg-6">
                    <div class="store-img-container">
                        <img src="https://www.highlandscoffee.com.vn/vnt_upload/home/1_1.jpg" alt="Cửa hàng Aura gần bạn" class="store-img" loading="lazy" />
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- Call to Action -->
    <div class="container cta-section">
        <div class="cta-box d-flex flex-column flex-md-row align-items-center justify-content-between gap-4">
            <div class="max-w-2xl text-center text-md-start">
                <span class="text-uppercase fw-bold mb-2 d-block" style="color: #ffb4a9; font-size: 0.8rem; letter-spacing: 2px;">Trải nghiệm không giới hạn</span>
                <h2 class="display-6 fw-bold mb-3">Bạn đã sẵn sàng thưởng thức ly cà phê hoàn hảo hôm nay?</h2>
                <p class="mb-0 text-white-50" style="font-size: 1rem; line-height: 1.6;">Hơn 45 thức uống được cân chỉnh hoàn hảo từ đội ngũ Barista tâm huyết đang sẵn sàng phục vụ tại chi nhánh hoặc giao nóng hổi tận nơi.</p>
            </div>
            <div class="flex-shrink-0 mt-3 mt-md-0">
                <a href="${pageContext.request.contextPath}/menu" class="btn btn-light btn-lg fw-bold d-inline-flex align-items-center gap-2 px-4 py-3 shadow" style="color: var(--primary);">
                    Khám phá toàn bộ Menu
                </a>
            </div>
        </div>
    </div>

    <!-- Shared Footer Component -->
    <jsp:include page="/components/footer.jsp" />

    <!-- Reusable Product Detail & Customization Modal Component -->
    <jsp:include page="/components/product_detail_modal.jsp" />

    <!-- Nhúng Component Auth Modal -->
    <% if(session.getAttribute("user") == null) { %>
        <jsp:include page="components/auth_modal.jsp" />
    <% } %>

    <!-- Bootstrap 5 JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

    <!-- Script Check Login URL Param -->
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            // Check for login query parameter
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.get('login') === 'true') {
                const authModal = new bootstrap.Modal(document.getElementById('authModal'));
                authModal.show();
                // Remove parameter from URL without refreshing
                window.history.replaceState({}, document.title, window.location.pathname);
            }
        });
    </script>
</body>
</html>
