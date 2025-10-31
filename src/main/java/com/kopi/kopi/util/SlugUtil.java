package com.kopi.kopi.util;

import java.text.Normalizer;

public final class SlugUtil {
    private SlugUtil() {}
    public static String slug(String s) {
        if (s == null) return null;
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", ""); // bỏ dấu
        n = n.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return n;
    }
}
