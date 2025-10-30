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
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.OrderDetail;
import com.kopi.kopi.entity.DiningTable;
import com.kopi.kopi.repository.DiningTableRepository;
import com.kopi.kopi.entity.Position;
import com.kopi.kopi.repository.PositionRepository;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

@Configuration
public class DataInit {
	@Bean
    CommandLineRunner initData(UserRepository userRepository, RoleRepository roleRepository, CategoryRepository categoryRepository, ProductRepository productRepository, PasswordEncoder passwordEncoder, OrderRepository orderRepository, DiningTableRepository diningTableRepository, PositionRepository positionRepository) {
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

            // Seed dining tables (exact values, including timestamps and QR tokens)
            if (diningTableRepository.count() == 0) {
                LocalDateTime t614 = LocalDateTime.of(2025, 10, 28, 8, 42, 47, 614_000_000);
                LocalDateTime t616 = LocalDateTime.of(2025, 10, 28, 8, 42, 47, 616_000_000);
                LocalDateTime t617 = LocalDateTime.of(2025, 10, 28, 8, 42, 47, 617_000_000);
                LocalDateTime t618 = LocalDateTime.of(2025, 10, 28, 8, 42, 47, 618_000_000);

                DiningTable[] tables = new DiningTable[] {
                    DiningTable.builder().number(1).name("Table 1").status("AVAILABLE").qrToken("CFA6F97D-316D-4742-AE6D-C62DD0238D30").createdAt(t614).updatedAt(t614).build(),
                    DiningTable.builder().number(2).name("Table 2").status("AVAILABLE").qrToken("F368DB66-6C0B-4BB5-B458-880BFB75E1EE").createdAt(t616).updatedAt(t616).build(),
                    DiningTable.builder().number(3).name("Table 3").status("AVAILABLE").qrToken("E9DBA1ED-C2DF-4F43-A932-E68D90C2A917").createdAt(t616).updatedAt(t616).build(),
                    DiningTable.builder().number(4).name("Table 4").status("AVAILABLE").qrToken("B2A5DD86-636C-4999-BBFE-2E96CD55EDF8").createdAt(t617).updatedAt(t617).build(),
                    DiningTable.builder().number(5).name("Table 5").status("AVAILABLE").qrToken("0394CD5F-3CD9-4895-9A03-9F7B0D392958").createdAt(t617).updatedAt(t617).build(),
                    DiningTable.builder().number(6).name("Table 6").status("AVAILABLE").qrToken("BDB7459C-439C-4B65-B896-498AAD23B863").createdAt(t617).updatedAt(t617).build(),
                    DiningTable.builder().number(7).name("Table 7").status("AVAILABLE").qrToken("E8DA5212-3819-4052-A482-1C0A00927064").createdAt(t617).updatedAt(t617).build(),
                    DiningTable.builder().number(8).name("Table 8").status("AVAILABLE").qrToken("1CAE8792-B367-410D-863B-DE2CC5E4786C").createdAt(t617).updatedAt(t617).build(),
                    DiningTable.builder().number(9).name("Table 9").status("AVAILABLE").qrToken("07D05AD5-626E-438B-A3CA-3AA0D4716190").createdAt(t618).updatedAt(t618).build(),
                    DiningTable.builder().number(10).name("Table 10").status("AVAILABLE").qrToken("1F1D8D9F-3124-428E-BDB4-1FC44206509E").createdAt(t618).updatedAt(t618).build(),
                };
                diningTableRepository.saveAll(Arrays.asList(tables));
            }

            // Seed users
			if (userRepository.count() < 4) {
                LocalDateTime now = LocalDateTime.now();

				// Fetch positions for staff (by id: 1 = Cashier, 4 = Shipper)
				Position cashierPos = positionRepository.findById(1).orElse(null);
				Position shipperPos = positionRepository.findById(4).orElse(null);

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
				admin.setEmailVerified(true);
				admin.setCreatedAt(now);
				admin.setUpdatedAt(now);
				userRepository.save(admin);

				// // Seed staff user
				Role staffRole = roleRepository.findByName("STAFF").orElseThrow();
				User staff = new User();
				staff.setUsername("staff");
				staff.setEmail("staff@example.com");
				staff.setPhone("0000000001");
				staff.setFullName("Employee Nguyễn Ngọc Khôi");
				staff.setPasswordHash(passwordEncoder.encode("staff123"));
				staff.setRole(staffRole);
				staff.setStatus(UserStatus.ACTIVE);
				staff.setEmailVerified(true);
				staff.setPosition(cashierPos);
				staff.setCreatedAt(now);
				staff.setUpdatedAt(now);
				userRepository.save(staff);

				// Seed staff2 user
				User staff2 = new User();
				staff2.setUsername("staff2");
				staff2.setEmail("staff2@example.com");
				staff2.setPhone("0000000003");
				staff2.setFullName("Shipper Nguyễn Ngọc Khôi");
				staff2.setPasswordHash(passwordEncoder.encode("staff123"));
				staff2.setRole(staffRole);
				staff2.setStatus(UserStatus.ACTIVE);
				staff2.setEmailVerified(true);
				staff2.setPosition(shipperPos);
				staff2.setCreatedAt(now);
				staff2.setUpdatedAt(now);
				userRepository.save(staff2);

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
				customer.setEmailVerified(true);
				customer.setCreatedAt(now);
				customer.setUpdatedAt(now);
				userRepository.save(customer);
			}
			
            // Seed positions
            if (positionRepository.count() == 0) {
                LocalDateTime now = LocalDateTime.now();
                Position cashier = Position.builder()
                        .positionName("Cashier")
                        .description("Nhân viên thu ngân")
                        .isActive(true)
                        .createdAt(now)
                        .build();
                Position waiter = Position.builder()
                        .positionName("Waiter")
                        .description("Nhân viên phục vụ")
                        .isActive(true)
                        .createdAt(now)
                        .build();
                Position barista = Position.builder()
                        .positionName("Barista")
                        .description("Nhân viên pha chế")
                        .isActive(true)
                        .createdAt(now)
                        .build();
				Position shipper = Position.builder()
                        .positionName("Shipper")
                        .description("Nhân viên giao hàng")
                        .isActive(true)
                        .createdAt(now)
                        .build();
                positionRepository.saveAll(Arrays.asList(cashier, waiter, barista, shipper));
            }

            // Seed sample completed orders for the seeded customer
			if (orderRepository.count() == 0) {
				LocalDateTime now = LocalDateTime.now();
				User customer = userRepository.findByUsername("customer").orElseGet(() -> userRepository.findById(3).orElse(null));
				if (customer != null) {
                    List<Product> products = productRepository.findAll();
					int max = Math.min(3, products.size());
					for (int i = 0; i < max; i++) {
						Product prod = products.get(i);
						OrderEntity order = new OrderEntity();
						order.setOrderCode("ORD-" + now.toLocalDate() + "-" + (i + 1));
						order.setCustomer(customer);
						order.setStatus("COMPLETED");
						order.setSubtotalAmount(prod.getPrice() != null ? prod.getPrice() : BigDecimal.ZERO);
						order.setDiscountAmount(BigDecimal.ZERO);
						order.setNote("Seed order " + (i + 1));
						order.setCreatedAt(now);
						order.setUpdatedAt(now);

						OrderDetail detail = new OrderDetail();
						detail.setOrder(order);
						detail.setProduct(prod);
						detail.setProductNameSnapshot(prod.getName());
						detail.setUnitPrice(prod.getPrice() != null ? prod.getPrice() : BigDecimal.ZERO);
						detail.setQuantity(1);

                        List<OrderDetail> details = new ArrayList<>();
						details.add(detail);
						order.setOrderDetails(details);

						orderRepository.save(order);
					}
				}
			}
		};
	}
} 