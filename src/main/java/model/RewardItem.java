package model;

public class RewardItem {
    private int id;
    private String name;
    private String description;
    private int pointsCost;
    private String category;
    private String imageIcon;
    private int stock;
    private String status;

    public RewardItem() {}

    public RewardItem(int id, String name, String description, int pointsCost, String category, String imageIcon, int stock, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.pointsCost = pointsCost;
        this.category = category;
        this.imageIcon = imageIcon;
        this.stock = stock;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getPointsCost() { return pointsCost; }
    public void setPointsCost(int pointsCost) { this.pointsCost = pointsCost; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getImageIcon() { return imageIcon; }
    public void setImageIcon(String imageIcon) { this.imageIcon = imageIcon; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
