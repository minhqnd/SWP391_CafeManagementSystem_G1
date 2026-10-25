package model;

public class ProductImage {
    private final byte[] data;
    private final String contentType;

    public ProductImage(byte[] data, String contentType) {
        this.data = data;
        this.contentType = contentType;
    }

    public byte[] getData() {
        return data;
    }

    public String getContentType() {
        return contentType;
    }
}
