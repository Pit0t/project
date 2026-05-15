package AES;

// Example:
//   JSONBuilder.build("name","Alice","op","WITHDRAW","amount","500")
//   → {"name":"Alice","op":"WITHDRAW","amount":"500"}
public class JSONBuilder {

    // builds a JSON string from key-value pairs
    // usage: JSONBuilder.build("key1","val1","key2","val2",...)
    public static String build(String... pairs) {
        if (pairs.length % 2 != 0)
            return "{}";

        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0)
                sb.append(",");
            sb.append("\"").append(pairs[i]).append("\"");
            sb.append(":");
            sb.append("\"").append(pairs[i + 1]).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    // extracts a value from a JSON string by key
    // example: getValue(json, "name") → "Alice"
    public static String getValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1)
            return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1)
            return "";
        return json.substring(start, end);
    }

    // checks if a JSON string contains a specific key
    public static boolean hasKey(String json, String key) {
        return json.contains("\"" + key + "\":");
    }
}