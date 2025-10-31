package com.kopi.kopi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kopi.kopi.service.MenuService;

@RestController
@RequestMapping("/api/menu")
public class MenuController {
	private final MenuService menuService;

	public MenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	@GetMapping
	public List<MenuCategoryDto> getMenu() {
		return menuService.getMenu();
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