package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.Category;
import com.kopi.kopi.entity.Product;
import com.kopi.kopi.repository.CategoryRepository;
import com.kopi.kopi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    @Mock
    ProductRepository productRepository;
    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    ProductServiceImpl service;

    private Product makeProduct(int id, String name, int stock) {
        Product p = new Product();
        p.setProductId(id);
        p.setName(name);
        p.setImgUrl("/img.png");
        p.setPrice(BigDecimal.valueOf(123));
        p.setStockQty(stock);
        p.setAvailable(true);
        return p;
    }

    @Test
    void list_withCategory_returnsMappedItems() {
        Product p = makeProduct(5, "Latte", 10);
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findByAvailableTrueAndCategory_CategoryIdAndNameContainingIgnoreCase(eq(2), eq(""),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), pageable, 1));

        Map<String, Object> res = service.list(2, "id", "asc", null, 10, 1);
        assertThat(res).containsKey("data");
        List<?> data = (List<?>) res.get("data");
        assertThat(data).hasSize(1);
        Map<?, ?> item = (Map<?, ?>) data.get(0);
        assertThat(item.get("id")).isEqualTo(5);
        assertThat(item.get("name")).isEqualTo("Latte");
        assertThat(item.get("price")).isEqualTo(BigDecimal.valueOf(123));
    }

    @Test
    void detail_whenFound_returnsData() {
        Product p = makeProduct(7, "Mocha", 3);
        when(productRepository.findById(7)).thenReturn(Optional.of(p));

        Map<String, Object> res = service.detail(7);
        assertThat(res).containsKey("data");
        List<?> data = (List<?>) res.get("data");
        assertThat(data).hasSize(1);
        Map<?, ?> item = (Map<?, ?>) data.get(0);
        assertThat(item.get("id")).isEqualTo(7);
        assertThat(item.get("name")).isEqualTo("Mocha");
    }

    @Test
    void create_withCategory_savesAndReturns() {
        Category c = new Category();
        c.setCategoryId(11);
        when(categoryRepository.findById(11)).thenReturn(Optional.of(c));
        when(productRepository.save(any())).thenAnswer(i -> {
            Product arg = i.getArgument(0);
            arg.setProductId(99);
            return arg;
        });

        ResponseEntity<?> r = service.create(null, "/img.png", "NewProduct", 11, "desc", BigDecimal.valueOf(500));
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        Map<?, ?> body = (Map<?, ?>) r.getBody();
        assertThat(body.get("data")).isNotNull();
        List<?> data = (List<?>) body.get("data");
        assertThat(((Map<?, ?>) data.get(0)).get("id")).isEqualTo(99);
    }

    @Test
    void delete_notFound_returns404() {
        when(productRepository.findById(123)).thenReturn(Optional.empty());
        ResponseEntity<?> r = service.delete(123);
        assertThat(r.getStatusCode().value()).isEqualTo(404);
        Map<?, ?> body = (Map<?, ?>) r.getBody();
        assertThat(body.get("message")).isEqualTo("not_found");
    }
}
