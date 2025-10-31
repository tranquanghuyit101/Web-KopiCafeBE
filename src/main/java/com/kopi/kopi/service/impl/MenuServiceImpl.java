package com.kopi.kopi.service.impl;

import com.kopi.kopi.controller.MenuController;
import com.kopi.kopi.entity.Category;
import com.kopi.kopi.repository.CategoryRepository;
import com.kopi.kopi.service.MenuService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {
    private final CategoryRepository categoryRepository;

    public MenuServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<MenuController.MenuCategoryDto> getMenu() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream().map(cat -> {
            List<MenuController.MenuProductDto> products = cat.getProducts().stream()
                    .map(p -> new MenuController.MenuProductDto(p.getName(), p.getPrice()))
                    .collect(Collectors.toList());
            return new MenuController.MenuCategoryDto(cat.getName(), products);
        }).collect(Collectors.toList());
    }
}

