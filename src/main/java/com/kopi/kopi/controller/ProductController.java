package com.kopi.kopi.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.kopi.kopi.entity.Category;
import com.kopi.kopi.entity.Product;
import com.kopi.kopi.repository.CategoryRepository;
import com.kopi.kopi.repository.ProductRepository;

@RestController
@RequestMapping("/apiv1/products")
public class ProductController {
	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;

	public ProductController(ProductRepository productRepository, CategoryRepository categoryRepository) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
	}

	@GetMapping
	public Map<String, Object> list(
		@RequestParam(name = "category", required = false) Integer categoryId,
		@RequestParam(name = "orderBy", required = false) String orderBy,
		@RequestParam(name = "sort", required = false) String sort,
		@RequestParam(name = "searchByName", required = false, defaultValue = "") String searchByName,
		@RequestParam(name = "limit", required = false, defaultValue = "8") Integer limit,
		@RequestParam(name = "page", required = false, defaultValue = "1") Integer page
	) {
		Sort s = Sort.unsorted();
		if (orderBy != null && !orderBy.isBlank()) {
			Sort.Direction dir = (sort != null && sort.equalsIgnoreCase("desc")) ? Sort.Direction.DESC : Sort.Direction.ASC;
			String prop = switch (orderBy) {
				case "price" -> "price";
				case "category" -> "category.categoryId";
				case "id" -> "productId";
				default -> "productId";
			};
			s = Sort.by(dir, prop);
		}
		Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(limit, 1), s);

		Page<Product> pageData;
		if (categoryId != null && categoryId > 0) {
			pageData = productRepository.findByCategory_CategoryIdAndNameContainingIgnoreCase(categoryId, searchByName == null ? "" : searchByName, pageable);
		} else {
			pageData = productRepository.findByNameContainingIgnoreCase(searchByName == null ? "" : searchByName, pageable);
		}

		List<Map<String, Object>> items = new ArrayList<>();
		for (Product p : pageData.getContent()) {
			Map<String, Object> m = new HashMap<>();
			m.put("id", p.getProductId());
			m.put("name", p.getName());
			m.put("img", p.getImgUrl());
			m.put("price", p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
			m.put("category_id", p.getCategory() != null ? p.getCategory().getCategoryId() : null);
			items.add(m);
		}

		Map<String, Object> meta = Map.of(
			"currentPage", pageData.getNumber() + 1,
			"totalPage", pageData.getTotalPages(),
			"prev", pageData.hasPrevious(),
			"next", pageData.hasNext()
		);

		return Map.of(
			"data", items,
			"meta", meta
		);
	}

	@GetMapping("/{id}")
	public Map<String, Object> detail(@PathVariable("id") Integer id) {
		Product p = productRepository.findById(id).orElseThrow();
		Map<String, Object> item = new HashMap<>();
		item.put("id", p.getProductId());
		item.put("name", p.getName());
		item.put("img", p.getImgUrl());
		item.put("price", p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
		item.put("desc", "");
		item.put("category_id", p.getCategory() != null ? p.getCategory().getCategoryId() : null);
		return Map.of("data", List.of(item));
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> create(
		@RequestPart(value = "image", required = false) MultipartFile image,
		@RequestPart("name") String name,
		@RequestPart("category_id") Integer categoryId,
		@RequestPart("desc") String desc,
		@RequestPart("price") BigDecimal price
	) {
		Category category = categoryRepository.findById(categoryId).orElseThrow();
		Product p = new Product();
		p.setName(name);
		p.setCategory(category);
		p.setPrice(price);
		productRepository.save(p);
		return ResponseEntity.ok(Map.of("message", "created"));
	}

	@PatchMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> update(
		@PathVariable("id") Integer id,
		@RequestPart(value = "image", required = false) MultipartFile image,
		@RequestPart("name") String name,
		@RequestPart("category_id") Integer categoryId,
		@RequestPart("desc") String desc,
		@RequestPart("price") BigDecimal price
	) {
		Product p = productRepository.findById(id).orElseThrow();
		Category category = categoryRepository.findById(categoryId).orElseThrow();
		p.setName(name);
		p.setCategory(category);
		p.setPrice(price);
		productRepository.save(p);
		return ResponseEntity.ok(Map.of("message", "updated"));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
		productRepository.deleteById(id);
		return ResponseEntity.ok(Map.of("message", "deleted"));
	}
} 