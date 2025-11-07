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
import com.kopi.kopi.entity.Size;
import com.kopi.kopi.entity.AddOn;
import com.kopi.kopi.entity.Payment;
import com.kopi.kopi.entity.enums.PaymentMethod;
import com.kopi.kopi.entity.enums.PaymentStatus;
import com.kopi.kopi.entity.ProductSize;
import com.kopi.kopi.entity.ProductAddOn;
import com.kopi.kopi.repository.ProductSizeRepository;
import com.kopi.kopi.repository.ProductAddOnRepository;
import com.kopi.kopi.repository.SizeRepository;
import com.kopi.kopi.repository.AddOnRepository;
import com.kopi.kopi.repository.DiscountCodeRepository;
import com.kopi.kopi.repository.DiscountEventRepository;
import com.kopi.kopi.entity.DiscountCode;
import com.kopi.kopi.entity.DiscountEvent;
import com.kopi.kopi.entity.DiscountEventProduct;
import com.kopi.kopi.entity.enums.DiscountType;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Configuration
public class DataInit {
	@Bean
    CommandLineRunner initData(UserRepository userRepository, RoleRepository roleRepository, CategoryRepository categoryRepository, ProductRepository productRepository, PasswordEncoder passwordEncoder, OrderRepository orderRepository, DiningTableRepository diningTableRepository, PositionRepository positionRepository, SizeRepository sizeRepository, AddOnRepository addOnRepository, ProductSizeRepository productSizeRepository, ProductAddOnRepository productAddOnRepository, DiscountCodeRepository discountCodeRepository, DiscountEventRepository discountEventRepository) {
		return args -> {
			// Seed roles
			if (roleRepository.count() == 0) {
				Role admin = new Role();
				admin.setName("ADMIN");
				admin.setDescription("System Administrator");
				roleRepository.save(admin);

				Role staff = new Role();
				staff.setName("STAFF");
				staff.setDescription("Employee");
				roleRepository.save(staff);

				Role customer = new Role();
				customer.setName("CUSTOMER");
				customer.setDescription("Customer");
				roleRepository.save(customer);
			}

            // --- Seed Categories ---
            if (categoryRepository.count() == 0) {
                categoryRepository.saveAll(List.of(
                        new Category("Coffee", true, 1),
                        new Category("Milk Tea", true, 2),
                        new Category("Juice", true, 4),
                        new Category("Ice Blended", true, 5),
                        new Category("Yogurt", true, 6),
                        new Category("Others", true, 7),
                        new Category("Food", true, 8),
                        new Category("Cake", true, 9),
                        new Category("Soft Drink", true, 10)
                ));
            }

            // --- Seed Products ---
            if (productRepository.count() == 0) {
                LocalDateTime now = LocalDateTime.now();

                Map<String, Category> catMap = categoryRepository.findAll().stream()
                        .collect(Collectors.toMap(Category::getName, c -> c));

                BiFunction<Category, String, String> skuGen = (cat, name) -> {
                    String prefix = switch (cat.getName()) {
                        case "Coffee" -> "CF";
                        case "Milk Tea" -> "MT";
                        case "Juice" -> "JC";
                        case "Ice Blended" -> "IB";
                        case "Yogurt" -> "YG";
                        case "Others" -> "OT";
                        case "Food" -> "FD";
                        case "Cake" -> "CK";
                        case "Soft Drink" -> "SD";
                        default -> "XX";
                    };
                    return prefix + "-" + name.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
                };

                List<Product> products = new ArrayList<>();

                // --- Coffee ---
                products.addAll(List.of(
                        new Product(catMap.get("Coffee"), "Almond Coffee", skuGen.apply(catMap.get("Coffee"), "Almond Coffee"), new BigDecimal("45000"), "https://i.pinimg.com/736x/22/1b/54/221b545576fc891208fe4b93974f6dba.jpg"),
                        new Product(catMap.get("Coffee"), "Tiramisu Coffee", skuGen.apply(catMap.get("Coffee"), "Tiramisu Coffee"), new BigDecimal("45000"), "https://i.pinimg.com/736x/5a/0e/89/5a0e8916fa870e199d82c8a7e718bdee.jpg"),
                        new Product(catMap.get("Coffee"), "KOPI Salty Coffee (Light)", skuGen.apply(catMap.get("Coffee"), "KOPI Salty Coffee Light"), new BigDecimal("39000"), "https://i.pinimg.com/736x/54/b6/69/54b669adf2b32d0dd9ed166114c421ee.jpg"),
                        new Product(catMap.get("Coffee"), "Hue Salty Coffee (Strong)", skuGen.apply(catMap.get("Coffee"), "Hue Salty Coffee Strong"), new BigDecimal("39000"), "https://i.pinimg.com/1200x/90/8b/47/908b47a4cc7b89e0446495ce81c94dd9.jpg"),
                        new Product(catMap.get("Coffee"), "Coconut Coffee", skuGen.apply(catMap.get("Coffee"), "Coconut Coffee"), new BigDecimal("50000"), "https://i.pinimg.com/1200x/82/69/73/8269737f8ec6069e4297e66c9a90ab8a.jpg"),
                        new Product(catMap.get("Coffee"), "Avocado Coffee", skuGen.apply(catMap.get("Coffee"), "Avocado Coffee"), new BigDecimal("50000"), "https://i.pinimg.com/1200x/4f/86/8d/4f868d38bba215cdbb3ee397bc36cc84.jpg"),
                        new Product(catMap.get("Coffee"), "Americano (Hot)", skuGen.apply(catMap.get("Coffee"), "Americano Hot"), new BigDecimal("35000"), "https://i.pinimg.com/736x/25/9f/7e/259f7e094804371eaf226f287183dcb0.jpg"),
                        new Product(catMap.get("Coffee"), "Americano (Iced)", skuGen.apply(catMap.get("Coffee"), "Americano Iced"), new BigDecimal("39000"), "https://i.pinimg.com/1200x/bc/0c/ff/bc0cffc8b21c24b4b571e98b9ab5da12.jpg"),
                        new Product(catMap.get("Coffee"), "Espresso (Black) (Hot)", skuGen.apply(catMap.get("Coffee"), "Espresso Black Hot"), new BigDecimal("35000"), "https://i.pinimg.com/1200x/e8/8c/96/e88c963b93c4ee2f3481b3576e2c9395.jpg"),
                        new Product(catMap.get("Coffee"), "Espresso (Black) (Iced)", skuGen.apply(catMap.get("Coffee"), "Espresso Black Iced"), new BigDecimal("39000"), "https://i.pinimg.com/1200x/17/4a/4a/174a4aee10877e3122f00274d78dc439.jpg"),
                        new Product(catMap.get("Coffee"), "Espresso (Milk)", skuGen.apply(catMap.get("Coffee"), "Espresso Milk"), new BigDecimal("39000"), "https://i.pinimg.com/736x/70/cc/03/70cc03ab72020dcb92dce739030bf467.jpg"),
                        new Product(catMap.get("Coffee"), "Cappuccino", skuGen.apply(catMap.get("Coffee"), "Cappuccino"), new BigDecimal("45000"), "https://i.pinimg.com/736x/f0/65/5f/f0655f2737da76be9b4ac435c65e3d9b.jpg"),
                        new Product(catMap.get("Coffee"), "Latte", skuGen.apply(catMap.get("Coffee"), "Latte"), new BigDecimal("45000"), "https://i.pinimg.com/736x/e3/83/f9/e383f9aba12fcabbffd116323690fb57.jpg"),
                        new Product(catMap.get("Coffee"), "Caramel Macchiato", skuGen.apply(catMap.get("Coffee"), "Caramel Macchiato"), new BigDecimal("45000"), "https://i.pinimg.com/1200x/7f/7c/0a/7f7c0a441577459d9a0a01ee28b59cc5.jpg"),
                        new Product(catMap.get("Coffee"), "Milk Coffee (Hot)", skuGen.apply(catMap.get("Coffee"), "Milk Coffee Hot"), new BigDecimal("35000"), "https://i.pinimg.com/736x/33/17/10/33171068a3f34787c12300512e89a370.jpg"),
                        new Product(catMap.get("Coffee"), "Milk Coffee (Iced)", skuGen.apply(catMap.get("Coffee"), "Milk Coffee Iced"), new BigDecimal("29000"), "https://i.pinimg.com/736x/cb/e8/81/cbe881824be44f238fb45d8c4c1bd581.jpg"),
                        new Product(catMap.get("Coffee"), "Black Coffee (Hot)", skuGen.apply(catMap.get("Coffee"), "Black Coffee Hot"), new BigDecimal("29000"), "https://i.pinimg.com/1200x/6a/e4/82/6ae482a0690b8ca89fd3acd554a36d66.jpg"),
                        new Product(catMap.get("Coffee"), "Black Coffee (Iced)", skuGen.apply(catMap.get("Coffee"), "Black Coffee Iced"), new BigDecimal("35000"), "https://i.pinimg.com/1200x/e2/76/97/e276971d3e3318fa1fc264b2a6346af5.jpg"),
                        new Product(catMap.get("Coffee"), "Vietnamese Latte (Hot)", skuGen.apply(catMap.get("Coffee"), "Vietnamese Latte Hot"), new BigDecimal("29000"), "https://i.pinimg.com/1200x/2c/9d/97/2c9d972ae2d5bc4eaa07009c3aed54d1.jpg"),
                        new Product(catMap.get("Coffee"), "Vietnamese Latte (Iced)", skuGen.apply(catMap.get("Coffee"), "Vietnamese Latte Iced"), new BigDecimal("35000"), "https://i.pinimg.com/1200x/5b/e5/5b/5be55b5609db57e252ddcf0b56dc5f71.jpg"),
                        new Product(catMap.get("Coffee"), "Dalgona Coffee", skuGen.apply(catMap.get("Coffee"), "Dalgona Coffee"), new BigDecimal("35000"), "https://i.pinimg.com/1200x/61/88/00/618800b3379557f85c0936a2a0b9e39f.jpg"),
                        new Product(catMap.get("Coffee"), "Egg Coffee", skuGen.apply(catMap.get("Coffee"), "Egg Coffee"), new BigDecimal("45000"), "https://i.pinimg.com/1200x/45/9c/9a/459c9ab6a310638c7c4476d3e01c9482.jpg"),
                        new Product(catMap.get("Coffee"), "Lime Salt Cold Brew", skuGen.apply(catMap.get("Coffee"), "Lime Salt Cold Brew"), new BigDecimal("50000"), "https://i.pinimg.com/1200x/a0/8b/61/a08b618b6bbe45c5f3ae62ffdaff1d7d.jpg"),
                        new Product(catMap.get("Coffee"), "Orange Cold Brew", skuGen.apply(catMap.get("Coffee"), "Orange Cold Brew"), new BigDecimal("50000"), "https://i.pinimg.com/1200x/48/4d/22/484d22882262b6e556620d5157f06cf0.jpg"),
                        new Product(catMap.get("Coffee"), "Pineapple Cold Brew", skuGen.apply(catMap.get("Coffee"), "Pineapple Cold Brew"), new BigDecimal("50000"), "https://i.pinimg.com/736x/51/d5/fd/51d5fdc8698ead5617a87f024839c0f5.jpg"),
                        new Product(catMap.get("Coffee"), "Cold Brew Original", skuGen.apply(catMap.get("Coffee"), "Cold Brew Original"), new BigDecimal("50000"), "https://i.pinimg.com/1200x/3a/39/4b/3a394b88494bcabce6405484182d9d62.jpg")
                ));

                // --- Milk Tea ---
                products.addAll(List.of(
                        new Product(catMap.get("Milk Tea"), "Red Bean Milk Tea", skuGen.apply(catMap.get("Milk Tea"), "Red Bean Milk Tea"), new BigDecimal("45000"), "https://i.pinimg.com/736x/c5/9d/8b/c59d8b34d947ddd4ce284c8fce05611b.jpg"),
                        new Product(catMap.get("Milk Tea"), "Traditional Milk Tea", skuGen.apply(catMap.get("Milk Tea"), "Traditional Milk Tea"), new BigDecimal("35000"), "https://i.pinimg.com/1200x/33/e2/13/33e2136c478d7c10fff68b3b51bb56d0.jpg"),
                        new Product(catMap.get("Milk Tea"), "Cream Cheese and Toasted Coconut Milk Tea", skuGen.apply(catMap.get("Milk Tea"), "Cream Cheese Toasted Coconut"), new BigDecimal("42000"), "https://i.pinimg.com/1200x/d3/0e/98/d30e9898a9bd7b5ce6932a8f8a2b2388.jpg"),
                        new Product(catMap.get("Milk Tea"), "Brown Sugar Bubble Fresh Milk", skuGen.apply(catMap.get("Milk Tea"), "Brown Sugar Bubble"), new BigDecimal("39000"), "https://i.pinimg.com/736x/81/a4/bb/81a4bbf789dc005db307140990fcad69.jpg"),
                        new Product(catMap.get("Milk Tea"), "Peach Jelly Tea", skuGen.apply(catMap.get("Milk Tea"), "Peach Jelly Tea"), new BigDecimal("45000"), "https://i.pinimg.com/1200x/cd/37/96/cd37960b5daf591ee805483a33e13809.jpg"),
                        new Product(catMap.get("Milk Tea"), "Orange Lemongrass Peach Tea", skuGen.apply(catMap.get("Milk Tea"), "Orange Lemongrass Peach"), new BigDecimal("45000"), "https://i.pinimg.com/1200x/bb/cc/41/bbcc4105999458179debf09501ae30c2.jpg"),
                        new Product(catMap.get("Milk Tea"), "Lychee Lotus Tea", skuGen.apply(catMap.get("Milk Tea"), "Lychee Lotus Tea"), new BigDecimal("45000"), "https://i.pinimg.com/1200x/7d/3a/1b/7d3a1b0801edd4098104175c4825ddbf.jpg"),
                        new Product(catMap.get("Milk Tea"), "Lotus Tea With Cheese Foam", skuGen.apply(catMap.get("Milk Tea"), "Lotus Tea Cheese Foam"), new BigDecimal("42000"), "https://i.pinimg.com/1200x/b8/e4/bc/b8e4bc2f7c22acfeced58a4bc9e8b46b.jpg"),
                        new Product(catMap.get("Milk Tea"), "Mango Passion Fruit Iced Tea", skuGen.apply(catMap.get("Milk Tea"), "Mango Passion Fruit"), new BigDecimal("45000"), "https://i.pinimg.com/736x/61/59/ee/6159eebea819fd740debf4430a8e0f6b.jpg"),
                        new Product(catMap.get("Milk Tea"), "Honey Ginger Tea (Hot)", skuGen.apply(catMap.get("Milk Tea"), "Honey Ginger Tea"), new BigDecimal("39000"), "https://i.pinimg.com/1200x/6e/9a/03/6e9a0343ac44d93c99f7e19584579116.jpg"),
                        new Product(catMap.get("Milk Tea"), "Herbal Tea (Hot)", skuGen.apply(catMap.get("Milk Tea"), "Herbal Tea"), new BigDecimal("50000"), "https://i.pinimg.com/1200x/cc/b9/75/ccb975efccaf0cb3ff403ea6e4993ae0.jpg")
                ));

                // 🧃 Juice
                products.addAll(List.of(
                        new Product(catMap.get("Juice"), "Detox Celery, Pineapple, Lemon, Honey", skuGen.apply(catMap.get("Juice"), "Detox Celery Pineapple Lemon Honey"), new BigDecimal("45000"), "https://i.pinimg.com/736x/24/92/64/24926487d9238bc41c1b5295c2b2dbbe.jpg"),
                        new Product(catMap.get("Juice"), "Lemon Juice with Honey and Chia Seed", skuGen.apply(catMap.get("Juice"), "Lemon Juice Honey Chia"), new BigDecimal("39000"), "https://i.pinimg.com/736x/c1/ca/56/c1ca56b003402e569085e0ce3a42cbdc.jpg"),
                        new Product(catMap.get("Juice"), "Coconut Juice", skuGen.apply(catMap.get("Juice"), "Coconut Juice"), new BigDecimal("39000"), "https://i.pinimg.com/736x/2c/36/63/2c3663c29b96f9f3ed7b00f702fe67f3.jpg"),
                        new Product(catMap.get("Juice"), "Pineapple Juice", skuGen.apply(catMap.get("Juice"), "Pineapple Juice"), new BigDecimal("39000"), "https://i.pinimg.com/1200x/b9/e4/e8/b9e4e87b2ca8129b803987b6e9be77e9.jpg"),
                        new Product(catMap.get("Juice"), "Watermelon Juice", skuGen.apply(catMap.get("Juice"), "Watermelon Juice"), new BigDecimal("39000"), "https://i.pinimg.com/1200x/bb/9f/e3/bb9fe38e7ef6c818e93e07eebc608b1b.jpg"),
                        new Product(catMap.get("Juice"), "Passion Fruit Juice", skuGen.apply(catMap.get("Juice"), "Passion Fruit Juice"), new BigDecimal("39000"), "https://i.pinimg.com/736x/21/8c/b0/218cb03d12ab90518a435536c17f03e2.jpg"),
                        new Product(catMap.get("Juice"), "Orange Juice", skuGen.apply(catMap.get("Juice"), "Orange Juice"), new BigDecimal("45000"), "https://i.pinimg.com/736x/f8/56/5b/f8565b7442a9df396adc387c8555d8b4.jpg")
                ));

                // 🍨 Ice Blended
                products.addAll(List.of(
                        new Product(catMap.get("Ice Blended"), "Matcha Jelly", skuGen.apply(catMap.get("Ice Blended"), "Matcha Jelly"), new BigDecimal("45000"), "https://i.pinimg.com/736x/67/10/fe/6710fea39a037746366f9994ab018bf1.jpg"),
                        new Product(catMap.get("Ice Blended"), "Caramel Jelly", skuGen.apply(catMap.get("Ice Blended"), "Caramel Jelly"), new BigDecimal("45000"), "https://i.pinimg.com/736x/28/82/fc/2882fcfe56f66a50981205cebf3d3e9a.jpg"),
                        new Product(catMap.get("Ice Blended"), "Choco Jelly", skuGen.apply(catMap.get("Ice Blended"), "Choco Jelly"), new BigDecimal("45000"), "https://i.pinimg.com/736x/5d/cf/39/5dcf39492571515b97407fafebd16df6.jpg"),
                        new Product(catMap.get("Ice Blended"), "Chocolate Cookie (Oreo)", skuGen.apply(catMap.get("Ice Blended"), "Chocolate Cookie Oreo"), new BigDecimal("45000"), "https://i.pinimg.com/1200x/15/80/4e/15804e381f8e4aca5742f0c17de6a66d.jpg"),
                        new Product(catMap.get("Ice Blended"), "Matcha Cookie (Oreo)", skuGen.apply(catMap.get("Ice Blended"), "Matcha Cookie Oreo"), new BigDecimal("45000"), "https://i.pinimg.com/1200x/39/12/ac/3912acb1357b9278e549652fb814d243.jpg")
                ));

                // 🍶 Yogurt
                products.addAll(List.of(
                        new Product(catMap.get("Yogurt"), "Mango Yogurt", skuGen.apply(catMap.get("Yogurt"), "Mango Yogurt"), new BigDecimal("42000"), "https://i.pinimg.com/1200x/2b/8a/1d/2b8a1d4aea74c71ede87adc55f4c2d8a.jpg"),
                        new Product(catMap.get("Yogurt"), "Peach Yogurt", skuGen.apply(catMap.get("Yogurt"), "Peach Yogurt"), new BigDecimal("42000"), "https://i.pinimg.com/736x/8d/5f/e2/8d5fe2f8f6d418145b77487e6bbb906d.jpg"),
                        new Product(catMap.get("Yogurt"), "Strawberry Yogurt", skuGen.apply(catMap.get("Yogurt"), "Strawberry Yogurt"), new BigDecimal("42000"), "https://i.pinimg.com/736x/16/ed/d4/16edd421d8996d8b45bf83c0ba0c325b.jpg"),
                        new Product(catMap.get("Yogurt"), "Blueberry Yogurt", skuGen.apply(catMap.get("Yogurt"), "Blueberry Yogurt"), new BigDecimal("42000"), "https://i.pinimg.com/736x/a6/50/75/a6507523db3777896adea88e5ed6b987.jpg")
                ));

                // 🍧 Others
                products.addAll(List.of(
                        new Product(catMap.get("Others"), "Coconut Red Bean", skuGen.apply(catMap.get("Others"), "Coconut Red Bean"), new BigDecimal("45000"), "https://i.pinimg.com/736x/74/2f/5e/742f5ec967a30a895298796b256cf903.jpg"),
                        new Product(catMap.get("Others"), "Matcha Latte", skuGen.apply(catMap.get("Others"), "Matcha Latte"), new BigDecimal("39000"), "https://i.pinimg.com/1200x/68/c3/39/68c339de45f7b5efebf7041748c3fdc1.jpg"),
                        new Product(catMap.get("Others"), "Chocolate", skuGen.apply(catMap.get("Others"), "Chocolate"), new BigDecimal("39000"), "https://i.pinimg.com/1200x/ee/40/03/ee4003aba030698d738680991f5d9b8a.jpg"),
                        new Product(catMap.get("Others"), "Fresh Milk", skuGen.apply(catMap.get("Others"), "Fresh Milk"), new BigDecimal("39000"), "https://i.pinimg.com/736x/31/0a/6b/310a6b08c926de4ae7c9fcdad047be83.jpg")
                ));

                // 🍔 Food
                products.addAll(List.of(
                        new Product(catMap.get("Food"), "Beef Spaghetti", skuGen.apply(catMap.get("Food"), "Beef Spaghetti"), new BigDecimal("65000"), "https://i.pinimg.com/736x/5f/b9/ca/5fb9ca7393ac6c9d3c209c73d7f0999c.jpg"),
                        new Product(catMap.get("Food"), "Chicken Spaghetti", skuGen.apply(catMap.get("Food"), "Chicken Spaghetti"), new BigDecimal("65000"), "https://i.pinimg.com/1200x/50/74/be/5074becbd85825b9c4e434471111359d.jpg"),
                        new Product(catMap.get("Food"), "Egg Toast", skuGen.apply(catMap.get("Food"), "Egg Toast"), new BigDecimal("35000"), "https://i.pinimg.com/1200x/be/63/4c/be634c424a9739f6cebd922860f20c25.jpg"),
                        new Product(catMap.get("Food"), "Tuna Toast", skuGen.apply(catMap.get("Food"), "Tuna Toast"), new BigDecimal("45000"), "https://i.pinimg.com/736x/45/47/7b/45477b69048dfdfe535c90867a08eb2c.jpg"),
                        new Product(catMap.get("Food"), "Ham Toast", skuGen.apply(catMap.get("Food"), "Ham Toast"), new BigDecimal("45000"), "https://i.pinimg.com/1200x/d1/08/0c/d1080c7cbb94c43c980e36464af8902b.jpg")
                ));

                // 🍰 Cake
                products.addAll(List.of(
                        new Product(catMap.get("Cake"), "Tiramisu", skuGen.apply(catMap.get("Cake"), "Tiramisu"), new BigDecimal("39000"), "https://i.pinimg.com/736x/0e/c2/89/0ec289492947872976c5c52215280faa.jpg"),
                        new Product(catMap.get("Cake"), "Matcha Mousse", skuGen.apply(catMap.get("Cake"), "Matcha Mousse"), new BigDecimal("39000"), "https://i.pinimg.com/736x/d9/69/46/d96946685466a74796463ac2860c30aa.jpg"),
                        new Product(catMap.get("Cake"), "Blueberry Cheese", skuGen.apply(catMap.get("Cake"), "Blueberry Cheese"), new BigDecimal("45000"), "https://i.pinimg.com/1200x/41/0d/d9/410dd90f76570122d9276d74dc551cfd.jpg"),
                        new Product(catMap.get("Cake"), "Passion Fruit Cheese", skuGen.apply(catMap.get("Cake"), "Passion Fruit Cheese"), new BigDecimal("45000"), "https://i.pinimg.com/1200x/a3/a5/a8/a3a5a87e36a2f6ec17f686d3d1e7ce19.jpg")
                ));

                // 🥤 Soft Drink
                products.addAll(List.of(
                        new Product(catMap.get("Soft Drink"), "Coca Cola", skuGen.apply(catMap.get("Soft Drink"), "Coca Cola"), new BigDecimal("20000"), "https://i.pinimg.com/1200x/cf/29/04/cf2904a920ba034d48402aa4c1aabae7.jpg"),
                        new Product(catMap.get("Soft Drink"), "Pepsi", skuGen.apply(catMap.get("Soft Drink"), "Pepsi"), new BigDecimal("20000"), "https://i.pinimg.com/1200x/56/77/83/56778331576a589e4766f600eedbaf35.jpg"),
                        new Product(catMap.get("Soft Drink"), "7Up", skuGen.apply(catMap.get("Soft Drink"), "7Up"), new BigDecimal("20000"), "https://i.pinimg.com/736x/5d/f2/7e/5df27e9844d06b24f7a2144cf9308f3a.jpg"),
                        new Product(catMap.get("Soft Drink"), "Evian Water", skuGen.apply(catMap.get("Soft Drink"), "Evian Water"), new BigDecimal("15000"), "https://i.pinimg.com/736x/17/bc/6b/17bc6ba3aa2b4af918dc18421d51461b.jpg")
                ));

                // Add timestamps and active flag
                products.forEach(p -> {
                    p.setActive(true);
                    p.setCreatedAt(now);
                });

                productRepository.saveAll(products);
            }


            // --- Seed Sizes --- (moved above order seeding)
            if (sizeRepository.count() == 0) {
                LocalDateTime now = LocalDateTime.now();
                Size s1 = Size.builder().name("Regular").code("R").displayOrder(1).createdAt(now).updatedAt(now).build();
                Size s2 = Size.builder().name("Large").code("L").displayOrder(2).createdAt(now).updatedAt(now).build();
                Size s3 = Size.builder().name("Xtra Large").code("XL").displayOrder(3).createdAt(now).updatedAt(now).build();
                sizeRepository.saveAll(Arrays.asList(s1, s2, s3));
            }

            // --- Seed Add-ons --- (moved above order seeding)
            if (addOnRepository.count() == 0) {
                LocalDateTime now = LocalDateTime.now();
                List<AddOn> addOns = new ArrayList<>();
                addOns.add(AddOn.builder().name("Extra sugar").displayOrder(1).createdAt(now).updatedAt(now).build());
                addOns.add(AddOn.builder().name("Extra milk").displayOrder(2).createdAt(now).updatedAt(now).build());
                addOns.add(AddOn.builder().name("Black boba").displayOrder(3).createdAt(now).updatedAt(now).build());
                addOns.add(AddOn.builder().name("White boba").displayOrder(4).createdAt(now).updatedAt(now).build());
                addOns.add(AddOn.builder().name("Golden boba").displayOrder(5).createdAt(now).updatedAt(now).build());
                addOns.add(AddOn.builder().name("Cheese boba").displayOrder(6).createdAt(now).updatedAt(now).build());
                addOns.add(AddOn.builder().name("Nata de coco").displayOrder(7).createdAt(now).updatedAt(now).build());
                addOns.add(AddOn.builder().name("Taro jelly").displayOrder(8).createdAt(now).updatedAt(now).build());
                addOns.add(AddOn.builder().name("Flan").displayOrder(9).createdAt(now).updatedAt(now).build());
                addOnRepository.saveAll(addOns);
            }

            // --- Seed Product Sizes (each product has all 3 sizes) ---
            if (productSizeRepository.count() == 0) {
                List<Product> allProducts = productRepository.findAll();
                Map<String, Size> codeToSize = sizeRepository.findAll().stream()
                        .collect(Collectors.toMap(s -> s.getCode().toUpperCase(Locale.ROOT), s -> s));
                Size sizeR = codeToSize.get("R");
                Size sizeL = codeToSize.get("L");
                Size sizeXL = codeToSize.get("XL");
                List<ProductSize> psList = new ArrayList<>();
                for (Product p : allProducts) {
					// Price stored here is the incremental delta
					if (sizeR != null) psList.add(ProductSize.builder().product(p).size(sizeR).price(new BigDecimal("0")).available(true).build());
					if (sizeL != null) psList.add(ProductSize.builder().product(p).size(sizeL).price(new BigDecimal("5000")).available(true).build());
					if (sizeXL != null) psList.add(ProductSize.builder().product(p).size(sizeXL).price(new BigDecimal("10000")).available(true).build());
                }
                if (!psList.isEmpty()) productSizeRepository.saveAll(psList);
            }

			// --- Seed Product Add-ons ---
			// Coffee: Extra sugar + Extra milk (0)
			// Milk Tea: boba (black/white/golden/cheese) 7000; nata de coco 5000; taro jelly 5000; flan 10000
			// Juice: nata de coco 5000; taro jelly 5000
			// Ice Blended: boba 7000; taro jelly 5000; flan 10000
			// Yogurt: boba 7000; nata de coco 5000; taro jelly 5000
			// Soft Drink: no add-ons
			if (productAddOnRepository.count() == 0) {
				List<Category> categories = categoryRepository.findAll();
				Category coffee = categories.stream().filter(c -> Objects.equals(c.getName(), "Coffee")).findFirst().orElse(null);
				Category milkTea = categories.stream().filter(c -> Objects.equals(c.getName(), "Milk Tea")).findFirst().orElse(null);
				Category juice = categories.stream().filter(c -> Objects.equals(c.getName(), "Juice")).findFirst().orElse(null);
				Category iceBlended = categories.stream().filter(c -> Objects.equals(c.getName(), "Ice Blended")).findFirst().orElse(null);
				Category yogurt = categories.stream().filter(c -> Objects.equals(c.getName(), "Yogurt")).findFirst().orElse(null);
				Category others = categories.stream().filter(c -> Objects.equals(c.getName(), "Others")).findFirst().orElse(null);

				AddOn sugar = addOnRepository.findByName("Extra sugar").orElse(null);
				AddOn milk = addOnRepository.findByName("Extra milk").orElse(null);
				AddOn blackBoba = addOnRepository.findByName("Black boba").orElse(null);
				AddOn whiteBoba = addOnRepository.findByName("White boba").orElse(null);
				AddOn goldenBoba = addOnRepository.findByName("Golden boba").orElse(null);
				AddOn cheeseBoba = addOnRepository.findByName("Cheese boba").orElse(null);
				AddOn nata = addOnRepository.findByName("Nata de coco").orElse(null);
				AddOn taro = addOnRepository.findByName("Taro jelly").orElse(null);
				AddOn flan = addOnRepository.findByName("Flan").orElse(null);

				List<ProductAddOn> mappings = new ArrayList<>();
				List<Product> all = productRepository.findAll();

				if (coffee != null) {
					List<Product> coffees = all.stream().filter(p -> p.getCategory() != null && Objects.equals(p.getCategory().getCategoryId(), coffee.getCategoryId())).toList();
					for (Product p : coffees) {
						if (sugar != null) mappings.add(ProductAddOn.builder().product(p).addOn(sugar).price(BigDecimal.ZERO).available(true).build());
						if (milk != null) mappings.add(ProductAddOn.builder().product(p).addOn(milk).price(BigDecimal.ZERO).available(true).build());
					}
				}

				if (milkTea != null) {
					List<Product> list = all.stream().filter(p -> p.getCategory() != null && Objects.equals(p.getCategory().getCategoryId(), milkTea.getCategoryId())).toList();
					for (Product p : list) {
						if (blackBoba != null) mappings.add(ProductAddOn.builder().product(p).addOn(blackBoba).price(new BigDecimal("7000")).available(true).build());
						if (whiteBoba != null) mappings.add(ProductAddOn.builder().product(p).addOn(whiteBoba).price(new BigDecimal("7000")).available(true).build());
						if (goldenBoba != null) mappings.add(ProductAddOn.builder().product(p).addOn(goldenBoba).price(new BigDecimal("7000")).available(true).build());
						if (cheeseBoba != null) mappings.add(ProductAddOn.builder().product(p).addOn(cheeseBoba).price(new BigDecimal("7000")).available(true).build());
						if (nata != null) mappings.add(ProductAddOn.builder().product(p).addOn(nata).price(new BigDecimal("5000")).available(true).build());
						if (taro != null) mappings.add(ProductAddOn.builder().product(p).addOn(taro).price(new BigDecimal("5000")).available(true).build());
						if (flan != null) mappings.add(ProductAddOn.builder().product(p).addOn(flan).price(new BigDecimal("10000")).available(true).build());
					}
				}

				if (juice != null) {
					List<Product> list = all.stream().filter(p -> p.getCategory() != null && Objects.equals(p.getCategory().getCategoryId(), juice.getCategoryId())).toList();
					for (Product p : list) {
						if (nata != null) mappings.add(ProductAddOn.builder().product(p).addOn(nata).price(new BigDecimal("5000")).available(true).build());
						if (taro != null) mappings.add(ProductAddOn.builder().product(p).addOn(taro).price(new BigDecimal("5000")).available(true).build());
					}
				}

				if (iceBlended != null) {
					List<Product> list = all.stream().filter(p -> p.getCategory() != null && Objects.equals(p.getCategory().getCategoryId(), iceBlended.getCategoryId())).toList();
					for (Product p : list) {
						if (blackBoba != null) mappings.add(ProductAddOn.builder().product(p).addOn(blackBoba).price(new BigDecimal("7000")).available(true).build());
						if (taro != null) mappings.add(ProductAddOn.builder().product(p).addOn(taro).price(new BigDecimal("5000")).available(true).build());
						if (flan != null) mappings.add(ProductAddOn.builder().product(p).addOn(flan).price(new BigDecimal("10000")).available(true).build());
					}
				}

				if (yogurt != null) {
					List<Product> list = all.stream().filter(p -> p.getCategory() != null && Objects.equals(p.getCategory().getCategoryId(), yogurt.getCategoryId())).toList();
					for (Product p : list) {
						if (blackBoba != null) mappings.add(ProductAddOn.builder().product(p).addOn(blackBoba).price(new BigDecimal("7000")).available(true).build());
						if (nata != null) mappings.add(ProductAddOn.builder().product(p).addOn(nata).price(new BigDecimal("5000")).available(true).build());
						if (taro != null) mappings.add(ProductAddOn.builder().product(p).addOn(taro).price(new BigDecimal("5000")).available(true).build());
					}
				}

				if (others != null) {
					List<Product> list = all.stream().filter(p -> p.getCategory() != null && Objects.equals(p.getCategory().getCategoryId(), others.getCategoryId())).toList();
					for (Product p : list) {
						if (blackBoba != null) mappings.add(ProductAddOn.builder().product(p).addOn(blackBoba).price(new BigDecimal("7000")).available(true).build());
						if (nata != null) mappings.add(ProductAddOn.builder().product(p).addOn(nata).price(new BigDecimal("5000")).available(true).build());
					}
				}

				// Soft drink intentionally left without add-ons

				if (!mappings.isEmpty()) productAddOnRepository.saveAll(mappings);
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

            // Seed positions FIRST to ensure we have deterministic IDs (1=Cashier, 4=Shipper)
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

            // Seed sample completed orders for the seeded customer (more complete per schema)
			if (orderRepository.count() == 0) {
				LocalDateTime now = LocalDateTime.now();
				User customer = userRepository.findByUsername("customer").orElseGet(() -> userRepository.findById(3).orElse(null));
                User creator = userRepository.findByUsername("staff").orElseGet(() -> userRepository.findById(2).orElse(null));
                DiningTable table1 = diningTableRepository.findByNumber(1).orElse(null);
                Size regular = sizeRepository.findByName("Regular").orElse(null);
				if (customer != null) {
                    List<Product> products = productRepository.findAll();
					int max = Math.min(3, products.size());
					for (int i = 0; i < max; i++) {
						Product prod = products.get(i);
						OrderEntity order = new OrderEntity();
						order.setOrderCode("ORD-" + now.toLocalDate() + "-" + (i + 1));
						order.setCustomer(customer);
                        order.setCreatedBy(creator);
                        order.setTable(table1);
						order.setStatus("COMPLETED");
                        BigDecimal unit = prod.getPrice() != null ? prod.getPrice() : BigDecimal.ZERO;
                        BigDecimal shipping = BigDecimal.ZERO;
                        BigDecimal discount = BigDecimal.ZERO;
                        order.setSubtotalAmount(unit);
                        order.setShippingAmount(shipping);
                        order.setDiscountAmount(discount);
						order.setNote("Seed order " + (i + 1));
						order.setCreatedAt(now);
						order.setUpdatedAt(now);

						OrderDetail detail = new OrderDetail();
						detail.setOrder(order);
						detail.setProduct(prod);
						detail.setProductNameSnapshot(prod.getName());
                        detail.setUnitPrice(unit);
						detail.setQuantity(1);
                        if (regular != null) detail.setSize(regular);

                        List<OrderDetail> details = new ArrayList<>();
						details.add(detail);
						order.setOrderDetails(details);
                        // Add payment entry
                        Payment payment = Payment.builder()
                                .order(order)
                                .amount(unit.subtract(discount).add(shipping))
                                .method(PaymentMethod.CASH)
                                .status(PaymentStatus.PAID)
                                .createdAt(now)
                                .build();
                        order.getPayments().add(payment);
						orderRepository.save(order);
					}

            // --- Seed Discounts (Codes and Events) ---
            try {
                LocalDateTime nowDsc = LocalDateTime.now();
                // Seed Discount Codes (3 variants: expired, current, upcoming)
                if (discountCodeRepository.count() < 3) {
                    DiscountCode codeExpired = DiscountCode.builder()
                            .code("EXPIRED10")
                            .description("Expired 10% off")
                            .discountType(DiscountType.PERCENT)
                            .discountValue(new BigDecimal("10"))
                            .minOrderAmount(new BigDecimal("100000"))
                            .startsAt(nowDsc.minusDays(10))
                            .endsAt(nowDsc.minusDays(3))
                            .totalUsageLimit(100)
                            .perUserLimit(2)
                            .active(true)
                            .usageCount(0)
                            .createdAt(nowDsc)
                            .build();
                    DiscountCode codeCurrent = DiscountCode.builder()
                            .code("ACTIVE15K")
                            .description("Active 15,000 VND off")
                            .discountType(DiscountType.AMOUNT)
                            .discountValue(new BigDecimal("15000"))
                            //.minOrderAmount(new BigDecimal("50000"))
                            .startsAt(nowDsc.minusDays(2))
                            .endsAt(nowDsc.minusDays(2).plusDays(7))
                            .totalUsageLimit(500)
                            .perUserLimit(3)
                            .active(true)
                            .usageCount(0)
                            .createdAt(nowDsc)
                            .build();
                    DiscountCode codeUpcoming = DiscountCode.builder()
                            .code("UPCOMING20")
                            .description("Upcoming 20% off")
                            .discountType(DiscountType.PERCENT)
                            .discountValue(new BigDecimal("20"))
                            .minOrderAmount(new BigDecimal("50000"))
                            .startsAt(nowDsc.plusDays(7))
                            .endsAt(nowDsc.plusDays(14))
                            .totalUsageLimit(200)
                            .perUserLimit(1)
                            .active(true)
                            .usageCount(0)
                            .createdAt(nowDsc)
                            .build();
                    discountCodeRepository.saveAll(Arrays.asList(codeExpired, codeCurrent, codeUpcoming));
                }

                // Seed Discount Events (3 variants), each mapping to products 1,2,3
                if (discountEventRepository.count() < 3) {
                    List<Product> firstThree = productRepository.findAll().stream()
                            .filter(p -> p.getProductId() != null && p.getProductId() <= 3)
                            .sorted(Comparator.comparing(Product::getProductId))
                            .limit(3)
                            .toList();

                    DiscountEvent evExpired = DiscountEvent.builder()
                            .name("Expired Event 10%")
                            .description("Expired event discount for first 3 products")
                            .discountType(DiscountType.PERCENT)
                            .discountValue(new BigDecimal("10"))
                            .startsAt(nowDsc.minusDays(10))
                            .endsAt(nowDsc.minusDays(3))
                            .active(true)
                            .createdAt(nowDsc)
                            .build();
                    for (Product p : firstThree) {
                        evExpired.getProducts().add(DiscountEventProduct.builder().discountEvent(evExpired).product(p).build());
                    }

                    DiscountEvent evCurrent = DiscountEvent.builder()
                            .name("Active Event 12%")
                            .description("Active event discount for first 3 products")
                            .discountType(DiscountType.PERCENT)
                            .discountValue(new BigDecimal("12"))
                            .startsAt(nowDsc.minusDays(2))
                            .endsAt(nowDsc.minusDays(2).plusDays(7))
                            .active(true)
                            .createdAt(nowDsc)
                            .build();
                    for (Product p : firstThree) {
                        evCurrent.getProducts().add(DiscountEventProduct.builder().discountEvent(evCurrent).product(p).build());
                    }

                    DiscountEvent evUpcoming = DiscountEvent.builder()
                            .name("Upcoming Event 15%")
                            .description("Upcoming event discount for first 3 products")
                            .discountType(DiscountType.PERCENT)
                            .discountValue(new BigDecimal("15"))
                            .startsAt(nowDsc.plusDays(7))
                            .endsAt(nowDsc.plusDays(14))
                            .active(true)
                            .createdAt(nowDsc)
                            .build();
                    for (Product p : firstThree) {
                        evUpcoming.getProducts().add(DiscountEventProduct.builder().discountEvent(evUpcoming).product(p).build());
                    }

                    discountEventRepository.saveAll(Arrays.asList(evExpired, evCurrent, evUpcoming));
                }
            } catch (Exception ignored) {}
				}
			}
		};
	}
}