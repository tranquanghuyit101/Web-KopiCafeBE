package com.kopi.kopi.service.ai;

import com.kopi.kopi.util.SlugUtil;

import java.util.*;
import java.util.regex.Pattern;

public final class DishHeuristic {
    private static final List<String> KNOWN_HEADS = Arrays.asList(
            "latte","coffee","matcha","mocha","cappuccino","americano","espresso",
            "milk tea","tea","fruit tea","lemon tea","strawberry","coconut","caramel","salted"
    );
    private static final Pattern SEP = Pattern.compile("[\\p{Punct}\\s]+");

    private DishHeuristic(){}

    public static String guessDishName(String title, String description) {
        String t = (title == null ? "" : title).toLowerCase();
        String d = (description == null ? "" : description).toLowerCase();
        String text = t + " " + d;

        // ví dụ bắt cụm “salted caramel latte”, “dirty matcha”, “coconut coffee”
        // đơn giản: ưu tiên các cụm {adj} {flavor} {base}
        List<String> candidates = new ArrayList<>();
        if (text.contains("salted") && text.contains("caramel") && text.contains("latte"))
            candidates.add("Salted Caramel Latte");
        if (text.contains("dirty") && text.contains("matcha"))
            candidates.add("Dirty Matcha");
        if (text.contains("coconut") && text.contains("coffee"))
            candidates.add("Coconut Coffee");
        if (text.contains("strawberry") && text.contains("latte"))
            candidates.add("Strawberry Latte");

        if (!candidates.isEmpty()) return candidates.get(0);

        // Rơi vào trường hợp chung: lấy các token và ráp theo head
        String[] toks = SEP.split(text);
        Set<String> set = new LinkedHashSet<>(Arrays.asList(toks));
        if (set.contains("latte")) {
            if (set.contains("caramel")) return "Caramel Latte";
            if (set.contains("strawberry")) return "Strawberry Latte";
            if (set.contains("matcha")) return "Matcha Latte";
            return "Latte";
        }
        if (set.contains("coffee")) {
            if (set.contains("coconut")) return "Coconut Coffee";
            return "Coffee";
        }
        if (set.contains("matcha")) return "Matcha";
        return null;
    }

    public static String keyOf(String dishName) {
        return SlugUtil.slug(dishName);
    }

    // Gom đồng nghĩa (ví dụ: “Sea-salt Caramel Latte” → “Salted Caramel Latte”)
    public static String normalizeSynonym(String name) {
        if (name == null) return null;
        String s = name.trim();
        String key = SlugUtil.slug(s);
        Map<String,String> synonyms = Map.of(
                "sea-salt-caramel-latte", "Salted Caramel Latte",
                "salt-caramel-latte",     "Salted Caramel Latte",
                "dirty-matcha-latte",     "Dirty Matcha"
        );
        return synonyms.getOrDefault(key, s);
    }
}
