package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.Category;
import com.kopi.kopi.repository.CategoryRepository;
import com.kopi.kopi.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
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

