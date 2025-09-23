package com.kopi.kopi.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kopi.kopi.entity.Category;
import com.kopi.kopi.entity.Product;
import com.kopi.kopi.repository.CategoryRepository;

@RestController
@RequestMapping("/api/menu")
public class MenuController {
	private final CategoryRepository categoryRepository;

	public MenuController(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	@GetMapping
	public List<MenuCategoryDto> getMenu() {
		List<Category> categories = categoryRepository.findAll();
		return categories.stream().map(cat -> {
			List<MenuProductDto> products = cat.getProducts().stream()
					.map(p -> new MenuProductDto(p.getName(), p.getPrice()))
					.collect(Collectors.toList());
			return new MenuCategoryDto(cat.getName(), products);
		}).collect(Collectors.toList());
	}

	public static class MenuCategoryDto {
		private String name;
		private List<MenuProductDto> products;

		public MenuCategoryDto(String name, List<MenuProductDto> products) {
			this.name = name;
			this.products = products;
		}

		public String getName() { return name; }
		public List<MenuProductDto> getProducts() { return products; }
	}

	public static class MenuProductDto {
		private String name;
		private java.math.BigDecimal price;

		public MenuProductDto(String name, java.math.BigDecimal price) {
			this.name = name;
			this.price = price;
		}

		public String getName() { return name; }
		public java.math.BigDecimal getPrice() { return price; }
	}
} 