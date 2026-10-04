package service;

import dao.ProductDAO;
import dao.VoucherDAO;
import model.Cart;
import model.CartItem;
import model.OrderDetail;
import model.Product;
import model.Voucher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CartService - Tính giá món trong giỏ hàng theo dữ liệu Database.
 * Giá luôn được tính ở server (giá gốc + size + topping), không tin giá gửi từ trình duyệt.
 */
public class CartService {

    public static final String STANDARD_SIZE = "Tiêu chuẩn";
    public static final double DEFAULT_TOPPING_PRICE = 8000;

    // Đọc variants_config dạng {"sizes":{"M":0,"L":10000},"toppings":[{"name":"Kem","price":8000}]}
    private static final Pattern SIZES_BLOCK = Pattern.compile("\"sizes\"\\s*:\\s*\\{([^}]*)\\}");
    private static final Pattern TOPPINGS_BLOCK = Pattern.compile("\"toppings\"\\s*:\\s*\\[([^\\]]*)\\]");
    private static final Pattern NAME_PRICE_PAIR = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{([^}]*)\\}");
    private static final Pattern NAME_FIELD = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PRICE_FIELD = Pattern.compile("\"price\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern QUOTED_TEXT = Pattern.compile("\"([^\"]+)\"");

    private final ProductDAO productDAO = new ProductDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();

    /**
     * Tạo một dòng giỏ hàng mới; trả về null nếu món không tồn tại hoặc đã ngừng phục vụ
     */
    public CartItem createItem(int productId, String size, List<String> toppings, int quantity) {
        Product product = productDAO.getProductById(productId);
        if (product == null || !product.isAvailable()) {
            return null;
        }
        CartItem item = new CartItem(productId, size, toppings, quantity);
        applyProduct(item, product);
        return item;
    }

    /**
     * Cập nhật lại tên, ảnh, giá, trạng thái còn phục vụ của các món và voucher trong giỏ theo Database.
     * customerId: khách đang đăng nhập (null nếu chưa đăng nhập) - voucher đổi điểm chỉ chủ của nó mới dùng được.
     * Trả về false nếu voucher đang dùng đã hết hạn / hết lượt và bị gỡ khỏi giỏ.
     */
    public boolean refreshCart(Cart cart, Integer customerId) {
        for (CartItem item : cart.getItems()) {
            Product product = productDAO.getProductById(item.getProductId());
            if (product == null || !product.isAvailable()) {
                item.setAvailable(false);
            } else {
                item.setAvailable(true);
                applyProduct(item, product);
            }
        }

        if (cart.getVoucher() != null) {
            Voucher voucher = voucherDAO.getUsableVoucherByCode(cart.getVoucher().getCode(), customerId);
            cart.setVoucher(voucher);
            return voucher != null;
        }
        return true;
    }

    /**
     * "Đặt lại đơn này": thêm lại các món của đơn cũ (đúng size, topping, số lượng) vào giỏ với giá hiện tại.
     * Món đã ngừng phục vụ được bỏ qua. Trả về số dòng món đã thêm.
     */
    public int addOrderItemsToCart(Cart cart, List<OrderDetail> details) {
        int added = 0;
        for (OrderDetail detail : details) {
            CartItem item = createItem(detail.getProductId(), detail.getSizeOption(),
                    detail.getToppingOptions(), detail.getQuantity());
            if (item != null) {
                cart.addItem(item);
                added++;
            }
        }
        return added;
    }

    /**
     * Chuẩn hoá size/topping theo cấu hình món và tính đơn giá = giá gốc + phụ thu size + topping
     */
    public void applyProduct(CartItem item, Product product) {
        Map<String, Double> sizes = getSizeOptions(product.getVariantsConfig());
        String size = item.getSize() != null ? item.getSize().trim() : "";
        if (!sizes.containsKey(size)) {
            size = getCheapestOption(sizes);
        }

        Map<String, Double> toppingOptions = getToppingOptions(product.getVariantsConfig());
        List<String> toppings = new ArrayList<>();
        double unitPrice = product.getBasePrice() + sizes.get(size);
        for (String topping : item.getToppings()) {
            // Bỏ qua topping không còn trong thực đơn của món
            if (toppingOptions.containsKey(topping) && !toppings.contains(topping)) {
                toppings.add(topping);
                unitPrice += toppingOptions.get(topping);
            }
        }

        item.setProductName(product.getName());
        item.setCategoryName(product.getCategoryName());
        item.setImageUrl(product.getImageUrl());
        item.setSize(size);
        item.setToppings(toppings);
        item.setUnitPrice(unitPrice);
    }

    /**
     * Các size của món và phụ thu tương ứng.
     * Món chưa cấu hình size dùng M/L giống popup chọn món ở trang Menu.
     */
    static Map<String, Double> getSizeOptions(String variantsConfig) {
        Map<String, Double> sizes = new LinkedHashMap<>();
        if (variantsConfig != null) {
            Matcher block = SIZES_BLOCK.matcher(variantsConfig);
            if (block.find()) {
                Matcher pair = NAME_PRICE_PAIR.matcher(block.group(1));
                while (pair.find()) {
                    sizes.put(pair.group(1), Double.parseDouble(pair.group(2)));
                }
            }
        }
        if (sizes.isEmpty()) {
            sizes.put(STANDARD_SIZE, 0.0);
            sizes.put("M", 0.0);
            sizes.put("L", 10000.0);
        }
        return sizes;
    }

    /**
     * Các topping của món và giá tương ứng (topping chỉ có tên thì giá mặc định 8.000đ như trang Menu)
     */
    static Map<String, Double> getToppingOptions(String variantsConfig) {
        Map<String, Double> toppings = new LinkedHashMap<>();
        if (variantsConfig == null) {
            return toppings;
        }
        Matcher block = TOPPINGS_BLOCK.matcher(variantsConfig);
        if (!block.find()) {
            return toppings;
        }

        String content = block.group(1);
        if (content.contains("{")) {
            Matcher object = JSON_OBJECT.matcher(content);
            while (object.find()) {
                Matcher name = NAME_FIELD.matcher(object.group(1));
                if (name.find()) {
                    Matcher price = PRICE_FIELD.matcher(object.group(1));
                    double value = price.find() ? Double.parseDouble(price.group(1)) : 0;
                    toppings.put(name.group(1), value > 0 ? value : DEFAULT_TOPPING_PRICE);
                }
            }
        } else {
            Matcher name = QUOTED_TEXT.matcher(content);
            while (name.find()) {
                toppings.put(name.group(1), DEFAULT_TOPPING_PRICE);
            }
        }
        return toppings;
    }

    private static String getCheapestOption(Map<String, Double> options) {
        String cheapest = null;
        for (Map.Entry<String, Double> option : options.entrySet()) {
            if (cheapest == null || option.getValue() < options.get(cheapest)) {
                cheapest = option.getKey();
            }
        }
        return cheapest;
    }
}
