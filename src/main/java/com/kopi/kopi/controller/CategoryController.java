package com.kopi.kopi.controller;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kopi.kopi.entity.Category;
import com.kopi.kopi.repository.CategoryRepository;

@RestController
@RequestMapping("/apiv1/categories")
public class CategoryController {
    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public Map<String, Object> list() {
        List<Map<String, Object>> items = categoryRepository.findAll().stream()
            .sorted(Comparator.comparing(cat -> cat.getDisplayOrder() == null ? 0 : cat.getDisplayOrder()))
            .map(cat -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", cat.getCategoryId());
                m.put("name", cat.getName());
                m.put("display_order", cat.getDisplayOrder());
                return m;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", items);
        return response;
    }
}


