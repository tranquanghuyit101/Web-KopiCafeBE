package com.kopi.kopi.service.ai;

import java.util.Set;

public interface MenuCatalogPort {
    /** Trả về tập tên món hiện có trong menu (đã chuẩn hoá chữ thường/không dấu/slug). */
    Set<String> getCurrentMenuNameKeys();
}
