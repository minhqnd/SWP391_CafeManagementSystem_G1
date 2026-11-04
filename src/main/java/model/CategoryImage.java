package model;

public class CategoryImage {
    private final byte[] data;
    private final String contentType;

    public CategoryImage(byte[] data, String contentType) {
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
