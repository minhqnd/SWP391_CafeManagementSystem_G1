package model;

public class Ingredient {
    private int id;
    private String name;
    private String unit;

    public Ingredient() {}

    public Ingredient(int id, String name, String unit) {
        this.id = id;
        this.name = name;
        this.unit = unit;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
