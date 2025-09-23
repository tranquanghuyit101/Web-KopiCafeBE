package com.kopi.kopi.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kopi.kopi.entity.Role;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.repository.RoleRepository;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.entity.enums.UserStatus;
import com.kopi.kopi.entity.Category;
import com.kopi.kopi.entity.Product;
import com.kopi.kopi.repository.CategoryRepository;
import com.kopi.kopi.repository.ProductRepository;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Configuration
public class DataInit {
	@Bean
	CommandLineRunner initData(UserRepository userRepository, RoleRepository roleRepository, CategoryRepository categoryRepository, ProductRepository productRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// Seed roles
			if (roleRepository.count() == 0) {
				Role admin = new Role();
				admin.setName("ADMIN");
				admin.setDescription("Quản trị hệ thống");
				roleRepository.save(admin);

				Role staff = new Role();
				staff.setName("STAFF");
				staff.setDescription("Nhân viên");
				roleRepository.save(staff);

				Role customer = new Role();
				customer.setName("CUSTOMER");
				customer.setDescription("Khách hàng");
				roleRepository.save(customer);
			}

			// Seed categories
			if (categoryRepository.count() == 0) {
				Category caPhe = new Category();
				caPhe.setName("Cà phê");
				caPhe.setActive(true);
				caPhe.setDisplayOrder(1);
				categoryRepository.save(caPhe);

				Category traSua = new Category();
				traSua.setName("Trà sữa");
				traSua.setActive(true);
				traSua.setDisplayOrder(2);
				categoryRepository.save(traSua);
			}

			// Seed products
			if (productRepository.count() == 0) {
				LocalDateTime now = LocalDateTime.now();
				Category caPhe = categoryRepository.findByName("Cà phê").orElseGet(() -> categoryRepository.findAll().stream().findFirst().orElseThrow());
				Category traSua = categoryRepository.findByName("Trà sữa").orElseGet(() -> categoryRepository.findAll().stream().skip(1).findFirst().orElse(caPhe));

				Product p1 = new Product();
				p1.setCategory(caPhe);
				p1.setName("Cà phê đen");
				p1.setImgUrl(null);
				p1.setSku("CF-DEN");
				p1.setPrice(new BigDecimal("25000"));
				p1.setAvailable(true);
				p1.setStockQty(100);
				p1.setCreatedAt(now);
				p1.setUpdatedAt(now);

				Product p2 = new Product();
				p2.setCategory(caPhe);
				p2.setName("Cà phê sữa");
				p2.setImgUrl(null);
				p2.setSku("CF-SUA");
				p2.setPrice(new BigDecimal("30000"));
				p2.setAvailable(true);
				p2.setStockQty(100);
				p2.setCreatedAt(now);
				p2.setUpdatedAt(now);

				Product p3 = new Product();
				p3.setCategory(traSua);
				p3.setName("Trà sữa trân châu");
				p3.setImgUrl(null);
				p3.setSku("TS-TRANCHAU");
				p3.setPrice(new BigDecimal("35000"));
				p3.setAvailable(true);
				p3.setStockQty(100);
				p3.setCreatedAt(now);
				p3.setUpdatedAt(now);

				Product p4 = new Product();
				p4.setCategory(traSua);
				p4.setName("Trà sữa matcha");
				p4.setImgUrl(null);
				p4.setSku("TS-MATCHA");
				p4.setPrice(new BigDecimal("40000"));
				p4.setAvailable(true);
				p4.setStockQty(100);
				p4.setCreatedAt(now);
				p4.setUpdatedAt(now);

				productRepository.save(p1);
				productRepository.save(p2);
				productRepository.save(p3);
				productRepository.save(p4);
			}

			// Seed users
			if (userRepository.count() < 3) {
                LocalDateTime now = LocalDateTime.now();

				// Seed admin user
				Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
				User admin = new User();
				admin.setUsername("admin");
				admin.setEmail("admin@example.com");
				admin.setPhone("0000000000");
				admin.setFullName("Administrator Nguyễn Ngọc Khôi");
				admin.setPasswordHash(passwordEncoder.encode("admin123"));
				admin.setRole(adminRole);
				admin.setStatus(UserStatus.ACTIVE);
				admin.setCreatedAt(now);
				admin.setUpdatedAt(now);
				userRepository.save(admin);

				// Seed staff user
				Role staffRole = roleRepository.findByName("STAFF").orElseThrow();
				User staff = new User();
				staff.setUsername("staff");
				staff.setEmail("staff@example.com");
				staff.setPhone("0000000001");
				staff.setFullName("Employee Nguyễn Ngọc Khôi");
				staff.setPasswordHash(passwordEncoder.encode("staff123"));
				staff.setRole(staffRole);
				staff.setStatus(UserStatus.ACTIVE);
				staff.setCreatedAt(now);
				staff.setUpdatedAt(now);
				userRepository.save(staff);

				// Seed customer user
				Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();
				User customer = new User();
				customer.setUsername("customer");
				customer.setEmail("customer@example.com");
				customer.setPhone("0000000002");
				customer.setFullName("Customer Nguyễn Ngọc Khôi");
				customer.setPasswordHash(passwordEncoder.encode("customer123"));
				customer.setRole(customerRole);
				customer.setStatus(UserStatus.ACTIVE);
				customer.setCreatedAt(now);
				customer.setUpdatedAt(now);
				userRepository.save(customer);
			}
		};
	}
} 