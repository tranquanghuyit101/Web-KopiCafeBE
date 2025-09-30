package com.kopi.kopi.config;

import com.kopi.kopi.security.ForceChangeStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ForceChangeConfig {

    @Bean
    public ForceChangeStore forceChangeStore() {
        // Đường dẫn file lưu cờ (relative hoặc absolute)
        // Relative: ./data/force_change.json (tính từ working dir khi chạy app)
        // Absolute Linux: /var/app/data/force_change.json  (nhớ cấp quyền ghi)
        //return new ForceChangeStore("./data/force_change.json");
        return new ForceChangeStore("/tmp/force_change.json");
    }
}
