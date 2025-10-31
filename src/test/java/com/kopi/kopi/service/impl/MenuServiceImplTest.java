package com.kopi.kopi.service.impl;

import com.kopi.kopi.controller.MenuController;
import com.kopi.kopi.entity.Category;
import com.kopi.kopi.entity.Product;
import com.kopi.kopi.repository.CategoryRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    private MenuServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MenuServiceImpl(categoryRepository);
    }

    @AfterEach
    void tearDown() {
        // no-op
    }

    // ---------- helpers ----------
    private Product product(String name, BigDecimal price) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        return p;
    }

    private Category category(String name, List<Product> products) {
        Category c = new Category();
        c.setName(name);
        // đảm bảo list không null
        c.setProducts(products != null ? products : new ArrayList<>());
        return c;
    }

    // ================= getMenu =================

    @Test
    void should_MapCategoriesAndProducts_Correctly() {
        // Given
        var coffee = category("Coffee", List.of(
                product("Americano", new BigDecimal("3.50")),
                product("Latte", new BigDecimal("4.20"))
        ));
        var tea = category("Tea", List.of(
                product("Earl Grey", new BigDecimal("2.80"))
        ));
        when(categoryRepository.findAll()).thenReturn(List.of(coffee, tea));

        // When
        List<MenuController.MenuCategoryDto> menu = service.getMenu();

        // Then
        assertThat(menu).hasSize(2);
        // Category 1
        assertThat(menu.get(0).name()).isEqualTo("Coffee");
        assertThat(menu.get(0).products()).hasSize(2);
        assertThat(menu.get(0).products().get(0).name()).isEqualTo("Americano");
        assertThat(menu.get(0).products().get(0).price()).isEqualByComparingTo("3.50");
        assertThat(menu.get(0).products().get(1).name()).isEqualTo("Latte");
        assertThat(menu.get(0).products().get(1).price()).isEqualByComparingTo("4.20");
        // Category 2
        assertThat(menu.get(1).name()).isEqualTo("Tea");
        assertThat(menu.get(1).products()).hasSize(1);
        assertThat(menu.get(1).products().get(0).name()).isEqualTo("Earl Grey");
        assertThat(menu.get(1).products().get(0).price()).isEqualByComparingTo("2.80");

        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    void should_ReturnEmptyProducts_When_CategoryHasNoProducts() {
        // Given
        var snacks = category("Snacks", List.of());
        when(categoryRepository.findAll()).thenReturn(List.of(snacks));

        // When
        var menu = service.getMenu();

        // Then
        assertThat(menu).hasSize(1);
        assertThat(menu.get(0).name()).isEqualTo("Snacks");
        assertThat(menu.get(0).products()).isEmpty();
    }

    @Test
    void should_ReturnEmptyList_When_NoCategories() {
        // Given
        when(categoryRepository.findAll()).thenReturn(List.of());

        // When
        var menu = service.getMenu();

        // Then
        assertThat(menu).isEmpty();
        verify(categoryRepository).findAll();
    }

    @Test
    void should_PreserveOrder_AndAllowNullPrice() {
        // Given
        var cat = category("Specials", List.of(
                product("Mystery Drink", null),           // null price should flow through
                product("Signature", new BigDecimal("5"))
        ));
        when(categoryRepository.findAll()).thenReturn(List.of(cat));

        // When
        var menu = service.getMenu();

        // Then
        assertThat(menu).hasSize(1);
        var products = menu.get(0).products();
        assertThat(products).hasSize(2);
        assertThat(products.get(0).name()).isEqualTo("Mystery Drink");
        assertThat(products.get(0).price()).isNull();     // chấp nhận null
        assertThat(products.get(1).name()).isEqualTo("Signature");
        assertThat(products.get(1).price()).isEqualByComparingTo("5");
    }
}
