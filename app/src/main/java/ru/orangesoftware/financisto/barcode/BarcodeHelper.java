package ru.orangesoftware.financisto.barcode;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BarcodeHelper {

    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> q_pairs = new LinkedHashMap<String, String>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            q_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return q_pairs;
    }

}
