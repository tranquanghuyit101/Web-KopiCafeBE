package com.kopi.kopi.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lưu cặp email -> mustChangePassword vào file JSON để tồn tại qua restart.
 * Không phù hợp môi trường multi-instance (nhiều pod/server). Khi đó dùng Redis.
 */
public class ForceChangeStore {
    private final Path file;
    private final ObjectMapper om = new ObjectMapper();
    private final ConcurrentHashMap<String, Boolean> map = new ConcurrentHashMap<>();

    public ForceChangeStore(String filePath) {
        this.file = Paths.get(filePath);
        System.out.println("[ForceChangeStore] Using file: " + this.file);
        load();
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public boolean get(String email) {
        return map.getOrDefault(key(email), false);
    }

    /** mustChange = true -> set; false -> clear */
    public synchronized void set(String email, boolean mustChange) {
        if (mustChange) {
            map.put(key(email), true);
        } else {
            map.remove(key(email));
        }
        save();
    }

    private void load() {
        try {
            if (!Files.exists(file)) {
                // tạo thư mục cha nếu cần
                Path dir = file.getParent();
                if (dir != null && !Files.exists(dir)) {
                    Files.createDirectories(dir);
                }
                save(); // tạo file rỗng
                return;
            }
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length == 0) return;
            Map<String, Boolean> data = om.readValue(bytes, new TypeReference<>() {});
            map.clear();
            map.putAll(data != null ? data : Collections.emptyMap());
        } catch (IOException e) {
            System.err.println("[ForceChangeStore] Load failed: " + e.getMessage());
        }
    }

    private void save() {
        try {
            byte[] bytes = om.writerWithDefaultPrettyPrinter().writeValueAsBytes(map);
            Files.write(file, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[ForceChangeStore] Save failed: " + e.getMessage());
        }
    }
}
