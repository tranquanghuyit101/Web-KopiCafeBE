package com.kopi.kopi.controller;

import com.kopi.kopi.entity.Category;
import com.kopi.kopi.repository.CategoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/apiv1/categories")
public class CategoryController {
    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public Map<String, Object> list() {
        List<Category> list = categoryRepository.findAll();
        List<Map<String, Object>> items = list.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getCategoryId());
            m.put("name", c.getName());
            m.put("is_active", c.getActive());
            m.put("display_order", c.getDisplayOrder());
            return m;
        }).collect(Collectors.toList());
        return Map.of("data", items);
    }
}


