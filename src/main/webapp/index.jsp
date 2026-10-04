<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="dao.ProductDAO" %>
<%@ page import="model.Product" %>
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
        
        .pulse-dot { width: 8px; height: 8px; background-color: var(--primary-container); border-radius: 50%; display: inline-block; animation: pulse 2s infinite; }
        @keyframes pulse { 0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(178, 41, 46, 0.7); } 70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(178, 41, 46, 0); } 100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(178, 41, 46, 0); } }
        
        .hero-title { font-size: 3.5rem; font-weight: 800; line-height: 1.1; letter-spacing: -0.02em; color: #1a1c1e; }
        @media (max-width: 768px) { .hero-title { font-size: 2.5rem; } }
        
        .btn-custom-primary { background-color: var(--primary); color: white; border-radius: 0.5rem; font-weight: 600; padding: 0.75rem 1.5rem; transition: opacity 0.2s; border: none; display: inline-flex; align-items: center; gap: 0.5rem; text-decoration: none; }
        .btn-custom-primary:hover { opacity: 0.9; color: white; }
        .btn-custom-secondary { background-color: var(--surface-container-lowest); color: #1a1c1e; border-radius: 0.5rem; font-weight: 600; padding: 0.75rem 1.5rem; transition: background-color 0.2s; border: 1px solid #e0e0e0; display: inline-flex; align-items: center; gap: 0.5rem; text-decoration: none; }
        .btn-custom-secondary:hover { background-color: var(--surface-container); color: #1a1c1e; }
        
        .stats-box { background: rgba(255, 255, 255, 0.8); backdrop-filter: blur(8px); border-radius: 1rem; padding: 1.5rem; box-shadow: 0 2px 10px rgba(0,0,0,0.03); }
        .stat-value { font-size: 1.5rem; font-weight: 800; color: var(--primary); }
        
        .hero-img-container { position: relative; height: 440px; border-radius: 1.5rem; overflow: hidden; box-shadow: 0 1rem 3rem rgba(0,0,0,0.1); }
        .hero-img-bg { background-size: cover; background-position: center; width: 100%; height: 100%; transition: transform 0.7s; }
        .hero-img-container:hover .hero-img-bg { transform: scale(1.05); }
        .hero-img-overlay { position: absolute; inset: 0; background: linear-gradient(to top, rgba(0,0,0,0.8) 0%, rgba(0,0,0,0) 60%); display: flex; flex-direction: column; justify-content: flex-end; padding: 2rem; color: white; }
        
        .voucher-floating { position: absolute; bottom: -1.5rem; left: -1.5rem; background: white; padding: 1rem; border-radius: 1rem; box-shadow: 0 0.5rem 1.5rem rgba(0,0,0,0.1); border: 1px solid var(--surface-container-high); display: flex; align-items: center; gap: 1rem; z-index: 2; max-width: 300px; }
        
        /* Product Cards */
        .product-card { background: white; border-radius: 1rem; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.03); transition: transform 0.2s, box-shadow 0.2s; border: 1px solid var(--surface-container-high); height: 100%; display: flex; flex-direction: column; }
        .product-card:hover { transform: translateY(-4px); box-shadow: 0 10px 20px rgba(0,0,0,0.08); }
        .product-img-box { height: 220px; position: relative; overflow: hidden; }
        .product-img { width: 100%; height: 100%; background-size: cover; background-position: center; transition: transform 0.5s; }
        .product-card:hover .product-img { transform: scale(1.05); }
        
        .badge-tag { position: absolute; top: 12px; left: 12px; padding: 4px 8px; border-radius: 6px; font-size: 0.6875rem; font-weight: 700; text-transform: uppercase; z-index: 2; color: white; }
        .price-tag { position: absolute; bottom: 12px; right: 12px; padding: 4px 8px; border-radius: 4px; background: rgba(255,255,255,0.9); backdrop-filter: blur(4px); font-weight: 600; font-size: 0.875rem; z-index: 2; color: #1a1c1e; }
        
        .btn-add { width: 32px; height: 32px; border-radius: 8px; border: none; background: var(--surface-container-high); color: #1a1c1e; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
        .btn-add:hover { background: var(--primary); color: white; }
        
        /* Seasonal Section */
        .seasonal-section { background-color: var(--surface-container-low); padding: 5rem 0; }
        .seasonal-card { background: white; border-radius: 1.5rem; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.03); display: flex; flex-direction: column; height: 100%; border: 1px solid var(--surface-container-high); }
        .seasonal-img { height: 250px; background-size: cover; background-position: center; transition: transform 0.5s; }
        .seasonal-card:hover .seasonal-img { transform: scale(1.05); }
        
        /* Loyalty Section */
        .tier-card { border-radius: 1.5rem; padding: 2rem; display: flex; flex-direction: column; height: 100%; position: relative; overflow: hidden; }
        .tier-silver { background: white; border: 1px solid var(--surface-container-high); box-shadow: 0 4px 15px rgba(0,0,0,0.03); }
        .tier-gold { background: linear-gradient(180deg, #ffffff 0%, #fffbf2 100%); border: 2px solid rgba(149, 129, 90, 0.3); box-shadow: 0 10px 30px rgba(149, 129, 90, 0.1); }
        .tier-diamond { background-color: #121c2a; color: white; box-shadow: 0 15px 40px rgba(0,0,0,0.2); }
        
        /* CTA Section */
        .cta-box { background-color: var(--primary); color: white; border-radius: 2rem; padding: 4rem; position: relative; overflow: hidden; box-shadow: 0 20px 40px rgba(144, 11, 26, 0.2); margin-bottom: 5rem; }
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
                    <div class="d-inline-flex align-items-center gap-2 bg-white px-3 py-1 rounded-pill shadow-sm mb-4">
                        <span class="pulse-dot"></span>
                        <span class="text-primary-custom fw-bold" style="font-size: 0.75rem; letter-spacing: 1px; text-transform: uppercase;">Aura Privilege Club</span>
                    </div>
                    
                    <div class="mb-4">
                        <span class="text-secondary-custom fw-bold text-uppercase d-block mb-2" style="letter-spacing: 2px; font-size: 0.875rem;">Chào mừng bạn đến với</span>
                        <h1 class="hero-title">
                            Hương vị chuẩn mực.<br/>
                            <span class="text-primary-custom">Đặc quyền hội viên.</span>
                        </h1>
                    </div>
                    
                    <p class="hero-subtitle mb-4">
                        Khám phá không gian cà phê thủ công chọn lọc từ những nông trại cao nguyên danh tiếng cùng quà tặng chào mừng trị giá tới 150.000đ dành riêng cho thành viên mới.
                    </p>
                    
                    <div class="d-flex flex-wrap gap-3 mb-5">
                        <a href="${pageContext.request.contextPath}/menu" class="btn-custom-primary">
                            Khám phá toàn bộ Menu <span class="material-symbols-outlined fs-5">east</span>
                        </a>
                        <% if(session.getAttribute("user") == null) { %>
                            <button class="btn-custom-secondary" data-bs-toggle="modal" data-bs-target="#authModal">
                                <span class="material-symbols-outlined text-primary-custom fs-5">workspace_premium</span>
                                Kích hoạt thẻ thành viên
                            </button>
                        <% } else { %>
                            <a href="#" class="btn-custom-secondary">
                                <span class="material-symbols-outlined text-primary-custom fs-5">workspace_premium</span>
                                Thẻ thành viên của bạn
                            </a>
                        <% } %>
                    </div>
                    
                    <div class="stats-box row g-3 text-center text-md-start">
                        <div class="col-4">
                            <div class="stat-value">100%</div>
                            <div class="stat-label">Hạt Arabica Cầu Đất</div>
                        </div>
                        <div class="col-4">
                            <div class="stat-value">4.9/5</div>
                            <div class="stat-label">Đánh giá trải nghiệm</div>
                        </div>
                        <div class="col-4">
                            <div class="stat-value">15 Phút</div>
                            <div class="stat-label">Giao tận nơi giữ nhiệt</div>
                        </div>
                    </div>
                </div>
                
                <div class="col-lg-5 position-relative mt-5 mt-lg-0">
                    <div class="hero-img-container">
                        <div class="hero-img-bg" style="background-image: url('https://lh3.googleusercontent.com/aida-public/AB6AXuBw-dbJgXjvCR5nkOrwZ8VgbS5GY2OEyRxEbOPz5Icbwto9r4_yy3ThjTmDPVYgkVS0qvw9zgWOIgZyovscgGXFfWYGZIWnz4J-kEojbOR_3UIVRm6Pc2K3JfHrvidn2O5NIY6kYDmJT7_FHzfZXsvm5SPBgxa8HNtPCnWXgdeqGGBi8jSUbqQkSy9_zS1iUSwY3th8A9HlEIBbs8WrZi_JfRhmcTX_OGMjA0Fi-G89MqKh9nlG6OfL');"></div>
                        <div class="hero-img-overlay">
                            <span class="text-uppercase fw-bold mb-1" style="color: #ffb4a9; font-size: 0.75rem; letter-spacing: 1px;">Món Signature Aura</span>
                            <h2 class="fw-bold text-white fs-3 mb-2">Cà Phê Muối Aura</h2>
                            <p class="text-white-50 mb-3" style="font-size: 0.875rem;">Lớp kem béo mặn đặc chế hòa quyện cà phê phin nguyên bản từ cao nguyên Lâm Đồng.</p>
                            <a href="#" class="btn btn-sm bg-primary-custom d-inline-flex align-items-center gap-1 align-self-start fw-semibold py-2 px-3 shadow">
                                Tùy chỉnh món <span class="material-symbols-outlined fs-6">arrow_forward</span>
                            </a>
                        </div>
                    </div>
                    
                    <div class="voucher-floating d-none d-sm-flex">
                        <div class="rounded-3 bg-primary-custom d-flex align-items-center justify-content-center" style="width: 48px; height: 48px;">
                            <span class="material-symbols-outlined">card_membership</span>
                        </div>
                        <div>
                            <div class="text-primary-custom fw-bold text-uppercase" style="font-size: 0.75rem;">Voucher Welcome 50K</div>
                            <div class="text-dark" style="font-size: 0.875rem;">Áp dụng cho đơn hàng đầu tiên</div>
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
                    <div class="d-flex align-items-center gap-1 text-primary-custom fw-bold mb-2">
                        <span class="material-symbols-outlined fs-5">local_fire_department</span>
                        <span class="text-uppercase" style="font-size: 0.75rem; letter-spacing: 1px;">Ưa chuộng nhất</span>
                    </div>
                    <h2 class="fw-bold text-dark fs-2 mb-2">Sản phẩm bán chạy trong tuần</h2>
                    <p class="text-secondary-custom mb-0">Những công thức hương vị được yêu thích và đặt nhiều nhất bởi tín đồ sành cà phê.</p>
                </div>
                <a href="#" class="text-primary-custom fw-semibold text-decoration-none d-flex align-items-center gap-1">
                    Xem tất cả danh mục <span class="material-symbols-outlined fs-5">chevron_right</span>
                </a>
            </div>
            
            <div class="row g-4">
                <%
                    ProductDAO productDAO = new ProductDAO();
                    List<Product> productsList = productDAO.getAllProducts();
                    DecimalFormat formatter = new DecimalFormat("###,###,###");
                    for (Product p : productsList) { 
                        String priceStr = formatter.format(p.getBasePrice()).replace(",", ".") + "đ";
                        String desc = "Cà phê đặc sản thơm ngon, đậm đà.";
                        String imgUrl = p.getImageUrl() != null && !p.getImageUrl().isEmpty() ? p.getImageUrl() : "https://lh3.googleusercontent.com/aida-public/AB6AXuDZIHvaJT4mQKloRO3wsOdLK_H-3jVQ6dlX5-V0MNiGe9EovAzUhlDHw4iFDJtfJeJfdvT9P6mdfbUGDTw_S4MpFvJT_JgLtBh308WQKUWEf29pPjIWTcUameeU4l5ufqziY_8QwroRkxCAhBlf0pUKdbFwLrrTYOeVTgCO0N8ZxuIqKFXyVkSDgIrDu18Gy9OJ42tGAayDee90kqkZgUbnSxICfokYqruhylqZ3Kbea0iuhuFL904x";
                        
                        switch(p.getId()) {
                            case 1: 
                                desc = "Espresso nguyên chất pha loãng, giữ trọn hương vị đậm đà mộc mạc."; 
                                imgUrl = "https://images.unsplash.com/photo-1551030173-122aabc4489c?auto=format&fit=crop&q=80&w=800";
                                break;
                            case 2: 
                                desc = "Sự cân bằng hoàn hảo giữa Espresso đậm đà và bọt sữa nóng mịn màng."; 
                                imgUrl = "https://images.unsplash.com/photo-1570968915860-54d5c301fa9f?auto=format&fit=crop&q=80&w=800";
                                break;
                            case 3: 
                                desc = "Phiên bản đậm đà hơn của Latte với lớp bọt sữa dày dặn hấp dẫn."; 
                                imgUrl = "https://images.unsplash.com/photo-1572442388796-11668a67e53d?auto=format&fit=crop&q=80&w=800";
                                break;
                            case 4: 
                                desc = "Trà đào mát lạnh cùng những miếng đào tươi giòn sần sật xua tan oi bức."; 
                                imgUrl = "https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&q=80&w=800";
                                break;
                            case 5: 
                                desc = "Trà chanh truyền thống với hương chanh tươi mát, chua ngọt thanh dịu."; 
                                imgUrl = "https://images.unsplash.com/photo-1596704017254-9eb10a3e8ff6?auto=format&fit=crop&q=80&w=800";
                                break;
                            case 6: 
                                desc = "Trà sữa vị lá trà đen tự nhiên, ngọt ngào quyện cùng trân châu dai giòn."; 
                                imgUrl = "https://images.unsplash.com/photo-1558857563-b371032bda98?auto=format&fit=crop&q=80&w=800";
                                break;
                            case 7: 
                                desc = "Bánh phô mai nướng mềm mịn, tan chảy trong miệng với độ béo ngậy khó cưỡng."; 
                                imgUrl = "https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&q=80&w=800";
                                break;
                            case 8: 
                                desc = "Nước cam vắt nguyên chất, giàu vitamin C và năng lượng cho ngày mới."; 
                                imgUrl = "https://images.unsplash.com/photo-1621506289937-a8e4df240d0b?auto=format&fit=crop&q=80&w=800";
                                break;
                        }
                %>
                <!-- Card -->
                <div class="col-sm-6 col-lg-3">
                    <div class="product-card">
                        <div class="product-img-box">
                            <div class="product-img" style="background-image: url('<%= imgUrl %>');"></div>
                            <div class="price-tag"><%= priceStr %></div>
                        </div>
                        <div class="card-body-custom">
                            <div>
                                <h3 class="product-title"><%= p.getName() %></h3>
                                <p class="product-desc text-secondary-custom" style="font-size: 0.85rem; margin-top: 0.5rem;"><%= desc %></p>
                            </div>
                            <div class="d-flex justify-content-between align-items-center mt-3 pt-3 border-top">
                                <span class="text-primary-custom fw-medium" style="font-size: 0.75rem;"></span>
                                <button class="btn-add" data-product-id="<%= p.getId() %>"><span class="material-symbols-outlined fs-5">add</span></button>
                            </div>
                        </div>
                    </div>
                </div>
                <% } %>
            </div>
        </div>
    </section>

    <!-- Seasonal Releases Section -->
    <section class="seasonal-section">
        <div class="container">
            <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-end mb-4 gap-3">
                <div>
                    <span class="text-primary-custom fw-bold text-uppercase d-block mb-2" style="font-size: 0.75rem; letter-spacing: 1px;">Aura Seasonal Releases</span>
                    <h2 class="fw-bold text-dark fs-2 mb-2">Bộ sưu tập món mới - Mùa Thảo Mộc & Hạt</h2>
                    <p class="text-secondary-custom mb-0">Sự thăng hoa của các loại hạt rang mộc và thảo dược tự nhiên được tuyển lựa đặc biệt cho mùa này.</p>
                </div>
                <div class="d-flex gap-2">
                    <span class="badge bg-white text-secondary-custom border">Phiên bản giới hạn</span>
                    <span class="badge bg-white text-secondary-custom border">Ra mắt tháng này</span>
                </div>
            </div>
            
            <div class="row g-4">
                <% 
                    int maxSeasonal = 3;
                    int countSeasonal = 0;
                    for (int i = 3; i < productsList.size() && countSeasonal < maxSeasonal; i++) {
                        Product p = productsList.get(i);
                        String priceStr = formatter.format(p.getBasePrice()).replace(",", ".") + "đ";
                        String desc = "Đồ uống đặc biệt theo mùa.";
                        String imgUrl = p.getImageUrl() != null && !p.getImageUrl().isEmpty() ? p.getImageUrl() : "https://lh3.googleusercontent.com/aida-public/AB6AXuAcgRPc8BItsw3RcB_6EvOwKbblAJajE3GUnNQJzOqbV8wu_PNvyvs1Om33aV8PewUezJRcqBzMb-2WdFoD0_VkUZgNj07Jme5IP6Mc31_Si0ZlI7cwIlZLx17Gp2rVR49Rd9YwRNSQgPewzdmOqd1F4dkobPBHSXXpRULTAM8u0cxM1waClivynY60mAjv0ni79mgw7xTpUaF8stgm0Rrp4aQ_ClW65FsN3fI0pBTWd3KfpF-XMGQR";
                        String badge = "New Arrival";
                        
                        switch(p.getId()) {
                            case 4: 
                                desc = "Trà đào mát lạnh cùng những miếng đào tươi giòn sần sật xua tan oi bức."; 
                                imgUrl = "https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&q=80&w=800";
                                badge = "Signature Craft";
                                break;
                            case 5: 
                                desc = "Trà chanh truyền thống với hương chanh tươi mát, chua ngọt thanh dịu."; 
                                imgUrl = "https://images.unsplash.com/photo-1596704017254-9eb10a3e8ff6?auto=format&fit=crop&q=80&w=800";
                                badge = "Summer Vibes";
                                break;
                            case 6: 
                                desc = "Trà sữa vị lá trà đen tự nhiên, ngọt ngào quyện cùng trân châu dai giòn."; 
                                imgUrl = "https://images.unsplash.com/photo-1558857563-b371032bda98?auto=format&fit=crop&q=80&w=800";
                                badge = "Best Choice";
                                break;
                        }
                %>
                <div class="col-md-4">
                    <div class="seasonal-card">
                        <div class="position-relative overflow-hidden">
                            <div class="seasonal-img" style="background-image: url('<%= imgUrl %>');"></div>
                            <div class="position-absolute top-0 start-0 m-3 bg-white px-2 py-1 rounded text-primary-custom fw-bold shadow-sm" style="font-size: 0.75rem;"><%= badge %></div>
                        </div>
                        <div class="p-4 d-flex flex-column flex-1 justify-content-between">
                            <div>
                                <div class="d-flex justify-content-between align-items-baseline mb-2">
                                    <h3 class="fs-5 fw-bold mb-0"><%= p.getName() %></h3>
                                    <span class="text-primary-custom fw-bold"><%= priceStr %></span>
                                </div>
                                <p class="text-secondary-custom" style="font-size: 0.875rem;"><%= desc %></p>
                            </div>
                            <div class="d-flex justify-content-between align-items-center mt-3 pt-3 border-top">
                                <span class="text-secondary-custom" style="font-size: 0.75rem;">Phục vụ: Nóng / Lạnh</span>
                                <a href="${pageContext.request.contextPath}/menu" class="text-primary-custom fw-semibold text-decoration-none" style="font-size: 0.875rem;">Chi tiết</a>
                            </div>
                        </div>
                    </div>
                </div>
                <% 
                        countSeasonal++;
                    } 
                %>
            </div>
        </div>
    </section>

    <!-- Loyalty Matrix Section -->
    <section class="loyalty-section container">
        <div class="mb-5 text-center text-md-start">
            <span class="text-primary-custom fw-bold text-uppercase d-block mb-2" style="font-size: 0.75rem; letter-spacing: 1px;">Aura Loyalty Matrix</span>
            <h2 class="fw-bold text-dark fs-2 mb-2">Ưu đãi điểm thưởng tích luỹ Member</h2>
            <p class="text-secondary-custom mb-0">Mỗi 10.000đ chi tiêu tương đương 1 Điểm Aura (A-Point). Nâng cấp hạng để nhận gấp bội đặc quyền.</p>
        </div>

        <div class="row g-4 align-items-stretch">
            <!-- Silver -->
            <div class="col-lg-4">
                <div class="tier-card tier-silver">
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <div class="rounded-3 bg-light d-flex align-items-center justify-content-center" style="width: 48px; height: 48px;">
                            <span class="material-symbols-outlined text-secondary">military_tech</span>
                        </div>
                        <span class="badge bg-light text-secondary border text-uppercase">Khởi đầu</span>
                    </div>
                    <h3 class="fs-3 fw-bold mb-1">Silver Member</h3>
                    <p class="text-secondary-custom mb-3" style="font-size: 0.875rem;">Tích lũy từ 0 - 299 Điểm</p>
                    <div class="progress mb-4" style="height: 6px;">
                        <div class="progress-bar bg-secondary" role="progressbar" style="width: 33%"></div>
                    </div>
                    <ul class="list-unstyled flex-grow-1 text-secondary-custom" style="font-size: 0.875rem; line-height: 1.8;">
                        <li class="d-flex gap-2"><span class="material-symbols-outlined text-primary-custom" style="font-size: 1.25rem;">check_circle</span> Tích luỹ <strong>1 Điểm</strong> cho mỗi 10.000đ</li>
                        <li class="d-flex gap-2 mt-2"><span class="material-symbols-outlined text-primary-custom" style="font-size: 1.25rem;">check_circle</span> Tặng 01 E-Voucher 20% tháng sinh nhật</li>
                        <li class="d-flex gap-2 mt-2"><span class="material-symbols-outlined text-primary-custom" style="font-size: 1.25rem;">check_circle</span> Đổi voucher món ăn kèm chỉ từ 50 Điểm</li>
                    </ul>
                    <a href="#" class="btn btn-light w-100 fw-semibold mt-4">Xem chi tiết hạng Silver</a>
                </div>
            </div>
            
            <!-- Gold -->
            <div class="col-lg-4">
                <div class="tier-card tier-gold">
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <div class="rounded-3 d-flex align-items-center justify-content-center text-white" style="width: 48px; height: 48px; background-color: #95815a;">
                            <span class="material-symbols-outlined">workspace_premium</span>
                        </div>
                        <span class="badge text-white text-uppercase" style="background-color: #95815a;">Phổ biến nhất</span>
                    </div>
                    <h3 class="fs-3 fw-bold mb-1" style="color: #6b5c3f;">Gold Member</h3>
                    <p class="text-secondary-custom mb-3" style="font-size: 0.875rem;">Tích lũy từ 300 - 799 Điểm</p>
                    <div class="progress mb-4" style="height: 6px;">
                        <div class="progress-bar" role="progressbar" style="width: 66%; background-color: #95815a;"></div>
                    </div>
                    <ul class="list-unstyled flex-grow-1 text-secondary-custom" style="font-size: 0.875rem; line-height: 1.8;">
                        <li class="d-flex gap-2"><span class="material-symbols-outlined text-primary-custom" style="font-size: 1.25rem;">check_circle</span> Tích luỹ nhân hệ số <strong>x1.5 Điểm</strong></li>
                        <li class="d-flex gap-2 mt-2"><span class="material-symbols-outlined text-primary-custom" style="font-size: 1.25rem;">check_circle</span> Tặng 01 Ly đồ uống miễn phí tháng sinh nhật</li>
                        <li class="d-flex gap-2 mt-2"><span class="material-symbols-outlined text-primary-custom" style="font-size: 1.25rem;">check_circle</span> Miễn phí nâng cấp Size 2 lần/tháng</li>
                        <li class="d-flex gap-2 mt-2"><span class="material-symbols-outlined text-primary-custom" style="font-size: 1.25rem;">check_circle</span> Ưu tiên pha chế giờ cao điểm tại quầy</li>
                    </ul>
                    <a href="#" class="btn w-100 fw-semibold text-white mt-4" style="background-color: #95815a;">Đặc quyền Gold Member</a>
                </div>
            </div>
            
            <!-- Diamond -->
            <div class="col-lg-4">
                <div class="tier-card tier-diamond">
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <div class="rounded-3 bg-primary-custom d-flex align-items-center justify-content-center" style="width: 48px; height: 48px;">
                            <span class="material-symbols-outlined">diamond</span>
                        </div>
                        <span class="badge text-white text-uppercase" style="background-color: #b2292e;">VIP Tối thượng</span>
                    </div>
                    <h3 class="fs-3 fw-bold mb-1">Diamond Member</h3>
                    <p class="text-white-50 mb-3" style="font-size: 0.875rem;">Tích lũy từ 800 Điểm trở lên</p>
                    <div class="progress mb-4" style="height: 6px; background-color: rgba(255,255,255,0.2);">
                        <div class="progress-bar bg-primary-custom" role="progressbar" style="width: 100%;"></div>
                    </div>
                    <ul class="list-unstyled flex-grow-1 text-white-50" style="font-size: 0.875rem; line-height: 1.8;">
                        <li class="d-flex gap-2"><span class="material-symbols-outlined text-primary-custom" style="font-size: 1.25rem;">verified</span> <span class="text-white">Tích luỹ nhân hệ số <strong>x2.0 Điểm</strong> toàn diện</span></li>
                        <li class="d-flex gap-2 mt-2"><span class="material-symbols-outlined text-primary-custom" style="font-size: 1.25rem;">verified</span> Combo Sinh nhật: Bánh ngọt + Đồ uống</li>
                        <li class="d-flex gap-2 mt-2"><span class="material-symbols-outlined text-primary-custom" style="font-size: 1.25rem;">verified</span> Miễn phí giao hàng không giới hạn khoảng cách</li>
                        <li class="d-flex gap-2 mt-2"><span class="material-symbols-outlined text-primary-custom" style="font-size: 1.25rem;">verified</span> Lời mời tham dự Workshop Cà phê thử nếm</li>
                    </ul>
                    <a href="#" class="btn btn-light w-100 fw-semibold mt-4 text-dark">Gia nhập Diamond Club</a>
                </div>
            </div>
        </div>
    </section>

    <!-- Call to Action -->
    <div class="container cta-section">
        <div class="cta-box d-flex flex-column flex-md-row align-items-center justify-content-between gap-4">
            <div class="max-w-2xl text-center text-md-start">
                <span class="text-uppercase fw-bold mb-2 d-block" style="color: #ffb4a9; font-size: 0.875rem; letter-spacing: 2px;">Trải nghiệm không giới hạn</span>
                <h2 class="display-5 fw-bold mb-3">Bạn đã sẵn sàng thưởng thức ly cà phê hoàn hảo hôm nay?</h2>
                <p class="mb-0" style="color: #ffb4a9; font-size: 1.125rem;">Hơn 45 thức uống được cân chỉnh hoàn hảo từ đội ngũ Barista tâm huyết đang sẵn sàng phục vụ tại chi nhánh hoặc giao nóng hổi tận nơi.</p>
            </div>
            <div class="flex-shrink-0 mt-4 mt-md-0">
                <a href="${pageContext.request.contextPath}/menu" class="btn btn-light btn-lg fw-bold d-inline-flex align-items-center gap-2 px-4 py-3" style="color: var(--primary);">
                    Khám phá toàn bộ Menu <span class="material-symbols-outlined">restaurant_menu</span>
                </a>
            </div>
        </div>
    </div>

    <!-- Nhúng Component Auth Modal -->
    <% if(session.getAttribute("user") == null) { %>
        <jsp:include page="components/auth_modal.jsp" />
    <% } %>

    <!-- Bootstrap 5 JS Bundle -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

    <!-- Script Add To Cart -->
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            const addButtons = document.querySelectorAll('.btn-add');
            addButtons.forEach(button => {
                button.addEventListener('click', function() {
                    const productId = this.getAttribute('data-product-id');
                    if (productId) {
                        fetch('${pageContext.request.contextPath}/cart/add', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded',
                            },
                            body: 'productId=' + productId + '&quantity=1'
                        })
                        .then(response => {
                            if (response.status === 401) {
                                const authModal = new bootstrap.Modal(document.getElementById('authModal'));
                                authModal.show();
                                throw new Error('Unauthorized');
                            }
                            if (!response.ok) {
                                throw new Error('Network response was not ok');
                            }
                            return response.json();
                        })
                        .then(data => {
                            if (data.success) {
                                // Jump cart number animation and update
                                const badge = document.getElementById('headerCartBadge');
                                if (badge) {
                                    badge.textContent = data.totalItems;
                                    
                                    // Simple pop animation
                                    badge.style.transform = 'scale(1.5)';
                                    badge.style.transition = 'transform 0.2s';
                                    setTimeout(() => {
                                        badge.style.transform = 'scale(1)';
                                    }, 200);
                                }
                            }
                        })
                        .catch(error => {
                            if (error.message !== 'Unauthorized') {
                                console.error('Error:', error);
                                alert('Đã xảy ra lỗi khi thêm vào giỏ hàng.');
                            }
                        });
                    }
                });
            });
        });
    </script>
</body>
</html>
