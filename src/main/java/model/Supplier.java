package model;

import java.io.Serializable;

public class Supplier implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String phone;
    private String address;
    private String status; // 'ACTIVE', 'INACTIVE'
    private int ingredientCount;

    public Supplier() {
        this.status = "ACTIVE";
    }

    public Supplier(int id, String name, String phone, String address, String status) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.status = status != null ? status : "ACTIVE";
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getIngredientCount() {
        return ingredientCount;
    }

    public void setIngredientCount(int ingredientCount) {
        this.ingredientCount = ingredientCount;
    }
}
