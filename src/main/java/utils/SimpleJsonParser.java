package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiện ích đọc dữ liệu JSON đơn giản (không cần thư viện ngoài) và thoát ký tự khi trả về JSON.
 */
public final class SimpleJsonParser {

    private SimpleJsonParser() {
    }

    /** Lấy giá trị chuỗi theo tên khóa ở cấp ngoài cùng của đối tượng JSON. */
    public static String getString(String jsonText, String keyName, String defaultValue) {
        int valueStart = findValueStart(jsonText, keyName);
        if (valueStart < 0 || valueStart >= jsonText.length() || jsonText.charAt(valueStart) != '"') {
            return defaultValue;
        }
        StringBuilder textBuilder = new StringBuilder();
        readStringUntilClosingQuote(jsonText, valueStart, textBuilder);
        return textBuilder.toString();
    }

    /** Lấy giá trị số nguyên theo tên khóa; trả về defaultValue nếu không đọc được. */
    public static int getInt(String jsonText, String keyName, int defaultValue) {
        int valueStart = findValueStart(jsonText, keyName);
        if (valueStart < 0 || valueStart >= jsonText.length()) {
            return defaultValue;
        }

        int valueEnd = valueStart;
        while (valueEnd < jsonText.length()) {
            char currentChar = jsonText.charAt(valueEnd);
            boolean isDigit = currentChar >= '0' && currentChar <= '9';
            boolean isLeadingMinus = valueEnd == valueStart && currentChar == '-';
            if (isDigit || isLeadingMinus) {
                valueEnd++;
            } else {
                break;
            }
        }
        if (valueEnd == valueStart) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(jsonText.substring(valueStart, valueEnd));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /** Lấy giá trị số thực theo tên khóa; trả về defaultValue nếu không đọc được. */
    public static double getDouble(String jsonText, String keyName, double defaultValue) {
        int valueStart = findValueStart(jsonText, keyName);
        if (valueStart < 0 || valueStart >= jsonText.length()) {
            return defaultValue;
        }

        int valueEnd = valueStart;
        while (valueEnd < jsonText.length()) {
            char currentChar = jsonText.charAt(valueEnd);
            boolean isNumberChar = (currentChar >= '0' && currentChar <= '9')
                    || currentChar == '-' || currentChar == '+'
                    || currentChar == '.' || currentChar == 'e' || currentChar == 'E';
            if (isNumberChar) {
                valueEnd++;
            } else {
                break;
            }
        }
        if (valueEnd == valueStart) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(jsonText.substring(valueStart, valueEnd));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    /** Tách danh sách từng chuỗi đối tượng JSON `{...}` nằm trong một mảng (ví dụ mảng "items"). */
    public static List<String> getArrayObjects(String jsonText, String arrayKey) {
        List<String> objectList = new ArrayList<>();
        int arrayStart = findValueStart(jsonText, arrayKey);
        if (arrayStart < 0 || arrayStart >= jsonText.length() || jsonText.charAt(arrayStart) != '[') {
            return objectList;
        }

        boolean inString = false;
        boolean escaped = false;
        int bracketDepth = 0;
        int braceDepth = 0;
        int objectStartIndex = -1;

        for (int index = arrayStart; index < jsonText.length(); index++) {
            char currentChar = jsonText.charAt(index);

            // Bỏ qua mọi ký tự nằm bên trong chuỗi "..." (kể cả dấu }, ], \")
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (currentChar == '\\') {
                    escaped = true;
                } else if (currentChar == '"') {
                    inString = false;
                }
                continue;
            }

            if (currentChar == '"') {
                inString = true;
            } else if (currentChar == '[') {
                bracketDepth++;
            } else if (currentChar == ']') {
                bracketDepth--;
                if (bracketDepth == 0) {
                    break;
                }
            } else if (currentChar == '{') {
                if (braceDepth == 0) {
                    objectStartIndex = index;
                }
                braceDepth++;
            } else if (currentChar == '}') {
                braceDepth--;
                if (braceDepth == 0 && objectStartIndex >= 0) {
                    objectList.add(jsonText.substring(objectStartIndex, index + 1));
                    objectStartIndex = -1;
                }
            }
        }

        return objectList;
    }

    /** Thoát ký tự đặc biệt trước khi ghép chuỗi vào JSON trả về cho frontend. */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // Tìm vị trí ký tự đầu tiên của giá trị ứng với keyName (bỏ qua key trùng nằm sâu trong object con)
    private static int findValueStart(String jsonText, String keyName) {
        if (jsonText == null || keyName == null) {
            return -1;
        }

        int textLength = jsonText.length();
        int nestingDepth = 0;
        int currentIndex = 0;

        while (currentIndex < textLength) {
            char currentChar = jsonText.charAt(currentIndex);

            if (currentChar == '"') {
                StringBuilder keyBuilder = new StringBuilder();
                int afterQuoteIndex = readStringUntilClosingQuote(jsonText, currentIndex, keyBuilder);

                if (nestingDepth <= 1 && keyName.equals(keyBuilder.toString())) {
                    int colonIndex = skipSpaces(jsonText, afterQuoteIndex);
                    if (colonIndex < textLength && jsonText.charAt(colonIndex) == ':') {
                        return skipSpaces(jsonText, colonIndex + 1);
                    }
                }
                currentIndex = afterQuoteIndex;
                continue;
            }

            if (currentChar == '{' || currentChar == '[') {
                nestingDepth++;
            } else if (currentChar == '}' || currentChar == ']') {
                nestingDepth--;
            }
            currentIndex++;
        }

        return -1;
    }

    // Đọc chuỗi JSON từ vị trí dấu '"' mở; trả về vị trí ngay sau dấu '"' đóng
    private static int readStringUntilClosingQuote(String jsonText, int openQuoteIndex, StringBuilder output) {
        int textLength = jsonText.length();
        int currentIndex = openQuoteIndex + 1;

        while (currentIndex < textLength) {
            char currentChar = jsonText.charAt(currentIndex);

            if (currentChar == '\\' && currentIndex + 1 < textLength) {
                char escapedChar = jsonText.charAt(currentIndex + 1);
                if (escapedChar == 'n') {
                    output.append('\n');
                } else if (escapedChar == 'r') {
                    output.append('\r');
                } else if (escapedChar == 't') {
                    output.append('\t');
                } else {
                    output.append(escapedChar);
                }
                currentIndex += 2;
                continue;
            }

            if (currentChar == '"') {
                return currentIndex + 1;
            }

            output.append(currentChar);
            currentIndex++;
        }

        return textLength;
    }

    private static int skipSpaces(String text, int startIndex) {
        int currentIndex = startIndex;
        while (currentIndex < text.length() && Character.isWhitespace(text.charAt(currentIndex))) {
            currentIndex++;
        }
        return currentIndex;
    }
}
