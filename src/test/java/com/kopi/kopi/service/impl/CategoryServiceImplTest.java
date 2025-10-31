package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.Category;
import com.kopi.kopi.repository.CategoryRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CategoryServiceImpl(categoryRepository);
    }

    // helper
    private Category cat(Integer id, String name, Boolean active, Integer order) {
        Category c = new Category();
        c.setCategoryId(id);
        c.setName(name);
        c.setActive(active);
        c.setDisplayOrder(order);
        return c;
    }

    @Test
    void should_ReturnMappedList_When_CategoriesExist() {
        // Given
        var c1 = cat(1, "Coffee", true, 1);
        var c2 = cat(2, "Tea", false, 2);
        when(categoryRepository.findAll()).thenReturn(List.of(c1, c2));

        // When
        Map<String, Object> result = service.list();

        // Then
        verify(categoryRepository, times(1)).findAll();
        assertThat(result).containsKey("data");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");

        assertThat(data).hasSize(2);
        assertThat(data.get(0)).containsEntry("id", 1)
                .containsEntry("name", "Coffee")
                .containsEntry("is_active", true)
                .containsEntry("display_order", 1);
        assertThat(data.get(1)).containsEntry("id", 2)
                .containsEntry("name", "Tea")
                .containsEntry("is_active", false)
                .containsEntry("display_order", 2);
    }

    @Test
    void should_ReturnEmptyList_When_NoCategories() {
        // Given
        when(categoryRepository.findAll()).thenReturn(List.of());

        // When
        Map<String, Object> result = service.list();

        // Then
        assertThat(result).containsKey("data");
        assertThat((List<?>) result.get("data")).isEmpty();
    }

    @Test
    void should_PreserveOrder_AsReturnedByRepository() {
        // Given
        var c1 = cat(10, "Zebra", true, 10);
        var c2 = cat(5, "Alpha", true, 20);
        when(categoryRepository.findAll()).thenReturn(List.of(c1, c2));

        // When
        var data = (List<Map<String, Object>>) service.list().get("data");

        // Then
        assertThat(data).extracting(m -> m.get("id")).containsExactly(10, 5);
    }

    @Test
    void should_HandleNullFields_WithoutError() {
        // Given
        var c = cat(3, null, null, null);
        when(categoryRepository.findAll()).thenReturn(List.of(c));

        // When
        var data = (List<Map<String, Object>>) service.list().get("data");

        // Then
        assertThat(data).hasSize(1);
        var item = data.get(0);
        assertThat(item.get("id")).isEqualTo(3);
        assertThat(item.get("name")).isNull();
        assertThat(item.get("is_active")).isNull();
        assertThat(item.get("display_order")).isNull();
    }
}
