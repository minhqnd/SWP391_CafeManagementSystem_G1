package model;

import java.io.Serializable;

public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String status;
    private String imageUrl;
    private boolean storedImage;
    private int productCount;

    public Category() {
    }

    public Category(int id, String name, String status, String imageUrl, int productCount) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.imageUrl = imageUrl;
        this.productCount = productCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isStoredImage() {
        return storedImage;
    }

    public void setStoredImage(boolean storedImage) {
        this.storedImage = storedImage;
    }

    public int getProductCount() {
        return productCount;
    }

    public void setProductCount(int productCount) {
        this.productCount = productCount;
    }
}
