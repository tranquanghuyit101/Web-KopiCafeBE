package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.Category;
import com.kopi.kopi.entity.Product;
import com.kopi.kopi.repository.CategoryRepository;
import com.kopi.kopi.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;

    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(productRepository, categoryRepository);
    }

    @AfterEach
    void tearDown() {
        // no-op
    }

    // -------- Helpers --------

    private Product product(Integer id, String name, BigDecimal price, Integer stock, Integer catId, String img, String desc) {
        Product p = new Product();
        p.setProductId(id);
        p.setName(name);
        p.setPrice(price);
        p.setStockQty(stock);
        p.setImgUrl(img);
        p.setDescription(desc);
        if (catId != null) {
            Category c = new Category();
            c.setCategoryId(catId);
            p.setCategory(c);
        }
        return p;
    }

    // =================== list ===================

    @Test
    void should_ListByCategory_WithSearch_AndPriceDescSort() {
        // Given
        Integer catId = 5;
        String search = "latte";
        Sort sort = Sort.by(Sort.Direction.DESC, "price");
        Pageable pageable = PageRequest.of(0, 10, sort);

        Product p1 = product(1, "Latte A", new BigDecimal("5.50"), 10, catId, "img1", "d1");
        Product p2 = product(2, "Latte B", new BigDecimal("4.00"), 8, catId, null, "d2");
        Page<Product> page = new PageImpl<>(List.of(p1, p2), pageable, 2);

        when(productRepository.findByCategory_CategoryIdAndNameContainingIgnoreCase(eq(catId), eq(search), any(Pageable.class)))
                .thenReturn(page);

        // When
        Map<String, Object> res = service.list(catId, "price", "desc", search, 10, 1);

        // Then
        verify(productRepository).findByCategory_CategoryIdAndNameContainingIgnoreCase(eq(catId), eq(search), argThat(pr ->
                pr.getSort().getOrderFor("price").getDirection() == Sort.Direction.DESC
                        && pr.getPageNumber() == 0 && pr.getPageSize() == 10));
        List<Map<String, Object>> data = (List<Map<String, Object>>) res.get("data");
        assertThat(data).hasSize(2);
        assertThat(data.get(0).get("id")).isEqualTo(1);
        assertThat(data.get(0).get("category_id")).isEqualTo(catId);
    }

    @Test
    void should_ListAll_When_CategoryNull() {
        // Given
        Pageable pageable = PageRequest.of(0, 5, Sort.unsorted());
        Product p = product(10, "Capuchino", new BigDecimal("3.20"), 5, null, null, null);
        Page<Product> page = new PageImpl<>(List.of(p), pageable, 1);
        when(productRepository.findByNameContainingIgnoreCase(eq("cap"), any(Pageable.class))).thenReturn(page);

        // When
        Map<String, Object> res = service.list(null, null, null, "cap", 5, 1);

        // Then
        verify(productRepository).findByNameContainingIgnoreCase(eq("cap"), any(Pageable.class));
        List<Map<String, Object>> data = (List<Map<String, Object>>) res.get("data");
        assertThat(data.get(0).get("name")).isEqualTo("Capuchino");
    }

    @Test
    void should_ReturnCorrectMeta_ForPagination() {
        // Given: page index 1 (page=2), size 1, total 3 items → 3 pages
        Pageable pageable = PageRequest.of(1, 1);
        Page<Product> pg = new PageImpl<>(List.of(product(2, "B", BigDecimal.ONE, 1, null, null, null)), pageable, 3);
        when(productRepository.findByNameContainingIgnoreCase(eq(""), any(Pageable.class))).thenReturn(pg);

        // When
        Map<String, Object> res = service.list(null, "id", "asc", "", 1, 2);

        // Then
        Map<String, Object> meta = (Map<String, Object>) res.get("meta");
        assertThat(meta.get("currentPage")).isEqualTo(2);
        assertThat(meta.get("totalPage")).isEqualTo(3);
        assertThat(meta.get("prev")).isEqualTo(true);
        assertThat(meta.get("next")).isEqualTo(true);
    }

    @Test
    void should_DefaultPriceZero_When_ProductPriceNull() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Product p = product(3, "NoPrice", null, 0, null, null, null);
        Page<Product> pg = new PageImpl<>(List.of(p), pageable, 1);
        when(productRepository.findByNameContainingIgnoreCase(anyString(), any(Pageable.class))).thenReturn(pg);

        // When
        Map<String, Object> res = service.list(null, null, null, null, 10, 1);

        // Then
        List<Map<String, Object>> data = (List<Map<String, Object>>) res.get("data");
        assertThat(data.get(0).get("price")).isEqualTo(BigDecimal.ZERO);
    }

    // =================== detail ===================

    @Test
    void should_ReturnDetail_When_ProductExists() {
        // Given
        Product p = product(100, "Mocha", new BigDecimal("7.77"), 12, 9, "img", "desc");
        when(productRepository.findById(100)).thenReturn(Optional.of(p));

        // When
        Map<String, Object> res = service.detail(100);

        // Then
        List<?> data = (List<?>) res.get("data");
        Map<String, Object> item = (Map<String, Object>) data.get(0);
        assertThat(item.get("id")).isEqualTo(100);
        assertThat(item.get("desc")).isEqualTo("desc");
        assertThat(item.get("category_id")).isEqualTo(9);
    }

    @Test
    void should_Throw_When_Detail_NotFound() {
        // Given
        when(productRepository.findById(404)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.detail(404))
                .isInstanceOf(NoSuchElementException.class);
    }

    // =================== create ===================

    @Test
    void should_CreateProduct_WithImgUrl_AndReturnPayload() {
        // Given
        Category c = new Category(); c.setCategoryId(7);
        when(categoryRepository.findById(7)).thenReturn(Optional.of(c));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product pr = inv.getArgument(0);
            pr.setProductId(999);
            return pr;
        });

        // When
        ResponseEntity<?> resp = service.create(null, "https://img", "Cold Brew", 7, "fresh", new BigDecimal("12.34"));

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        Map<String, Object> item = (Map<String, Object>) ((List<?>) body.get("data")).get(0);
        assertThat(item.get("id")).isEqualTo(999);
        assertThat(item.get("name")).isEqualTo("Cold Brew");
        assertThat(item.get("img")).isEqualTo("https://img");
        assertThat(item.get("price")).isEqualTo(new BigDecimal("12.34"));
        assertThat(item.get("category_id")).isEqualTo(7);
    }

    @Test
    void should_NotSetImg_When_ImgUrlBlank() {
        // Given
        Category c = new Category(); c.setCategoryId(1);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(c));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ResponseEntity<?> resp = service.create(null, "   ", "Espresso", 1, "desc", new BigDecimal("2.5"));

        // Then
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        Map<String, Object> item = (Map<String, Object>) ((List<?>) body.get("data")).get(0);
        assertThat(item.get("img")).isNull();
    }

    @Test
    void should_Throw_When_CategoryNotFound_OnCreate() {
        // Given
        when(categoryRepository.findById(123)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.create(null, null, "Name", 123, null, BigDecimal.ONE))
                .isInstanceOf(NoSuchElementException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void should_ReturnPayloadWithAllFields_OnCreate() {
        // Given
        Category c = new Category(); c.setCategoryId(2);
        when(categoryRepository.findById(2)).thenReturn(Optional.of(c));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ResponseEntity<?> resp = service.create(null, "x", "NameX", 2, "D", BigDecimal.TEN);

        // Then
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        Map<String, Object> item = (Map<String, Object>) ((List<?>) body.get("data")).get(0);
        assertThat(item.keySet()).contains("id","name","img","price","desc","category_id");
    }

    // =================== update ===================

    @Test
    void should_UpdateSelectiveFields_AndSave() {
        // Given
        Product p = product(50, "Old", new BigDecimal("1.11"), 5, 3, "oldImg", "oldDesc");
        when(productRepository.findById(50)).thenReturn(Optional.of(p));

        Category newCat = new Category(); newCat.setCategoryId(9);
        when(categoryRepository.findById(9)).thenReturn(Optional.of(newCat));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ResponseEntity<?> resp = service.update(50, null, "newImg", "NewName", 9, "newDesc", new BigDecimal("9.99"));

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(p.getName()).isEqualTo("NewName");
        assertThat(p.getCategory().getCategoryId()).isEqualTo(9);
        assertThat(p.getPrice()).isEqualTo(new BigDecimal("9.99"));
        assertThat(p.getImgUrl()).isEqualTo("newImg");
        assertThat(p.getDescription()).isEqualTo("newDesc");
        verify(productRepository).save(p);
    }

    @Test
    void should_Throw_When_ProductNotFound_OnUpdate() {
        // Given
        when(productRepository.findById(404)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.update(404, null, null, null, null, null, null))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void should_Throw_When_CategoryNotFound_OnUpdate() {
        // Given
        Product p = product(60, "Name", BigDecimal.ONE, 1, 1, null, null);
        when(productRepository.findById(60)).thenReturn(Optional.of(p));
        when(categoryRepository.findById(999)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.update(60, null, null, null, 999, null, null))
                .isInstanceOf(NoSuchElementException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void should_NotChangeImg_When_ImgUrlBlank_OnUpdate() {
        // Given
        Product p = product(70, "Name", BigDecimal.ONE, 2, 4, "IMG", "desc");
        when(productRepository.findById(70)).thenReturn(Optional.of(p));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        service.update(70, null, "   ", "New", null, null, null);

        // Then
        assertThat(p.getImgUrl()).isEqualTo("IMG"); // unchanged
    }

    // =================== delete ===================

    @Test
    void should_DeleteById_AndReturnDeletedMessage() {
        // Given
        doNothing().when(productRepository).deleteById(10);

        // When
        ResponseEntity<?> resp = service.delete(10);

        // Then
        verify(productRepository).deleteById(10);
        assertThat(((Map<?,?>)resp.getBody()).get("message")).isEqualTo("deleted");
    }

    @Test
    void should_CallDeleteOnce() {
        // When
        service.delete(11);

        // Then
        verify(productRepository, times(1)).deleteById(11);
    }
}
