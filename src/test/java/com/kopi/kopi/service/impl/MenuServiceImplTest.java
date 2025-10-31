package com.kopi.kopi.service.impl;

import com.kopi.kopi.controller.MenuController;
import com.kopi.kopi.entity.Category;
import com.kopi.kopi.entity.Product;
import com.kopi.kopi.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    private MenuServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MenuServiceImpl(categoryRepository);
    }

    @Test
    void getMenu_should_map_categories_and_products() {
        // Given
        Category c = new Category("Drinks", true, 1);
        c.setProducts(new java.util.ArrayList<>());
        Product p = new Product(c, "Coffee", "SKU1", new BigDecimal("3.50"), "img");
        c.getProducts().add(p);

        when(categoryRepository.findAll()).thenReturn(List.of(c));

        // When
        List<MenuController.MenuCategoryDto> menu = service.getMenu();

        // Then
        assertThat(menu).hasSize(1);
        var catDto = menu.get(0);
        assertThat(catDto.getName()).isEqualTo("Drinks");
        assertThat(catDto.getProducts()).hasSize(1);
        var prod = catDto.getProducts().get(0);
        assertThat(prod.getName()).isEqualTo("Coffee");
        assertThat(prod.getPrice()).isEqualTo(new BigDecimal("3.50"));
    }

    @Test
    void getMenu_should_handle_empty_products() {
        Category c = new Category("Empty", true, 2);
        c.setProducts(new java.util.ArrayList<>());
        when(categoryRepository.findAll()).thenReturn(List.of(c));

        List<MenuController.MenuCategoryDto> menu = service.getMenu();
        assertThat(menu).hasSize(1);
        assertThat(menu.get(0).getProducts()).isEmpty();
    }
}
