package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Một dòng cấu hình dạng khóa - giá trị trong bảng shop_settings. */
public class ShopSettingEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private String key;
    private String value;
    private LocalDateTime updatedAt;

    public ShopSettingEntry() { }

    public ShopSettingEntry(String key, String value, LocalDateTime updatedAt) {
        this.key = key;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedAtLabel() {
        return updatedAt == null ? "--" : updatedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
