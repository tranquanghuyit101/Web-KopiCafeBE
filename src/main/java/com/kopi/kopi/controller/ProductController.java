package com.kopi.kopi.controller;

import java.math.BigDecimal;
import java.util.Map;

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

import com.kopi.kopi.service.ProductService;

@RestController
@RequestMapping("/apiv1/products")
public class ProductController {
	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
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
		return productService.list(categoryId, orderBy, sort, searchByName, limit, page);
	}

	@GetMapping("/{id}")
	public Map<String, Object> detail(@PathVariable("id") Integer id) {
		return productService.detail(id);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> create(
		@RequestParam(value = "image", required = false) MultipartFile image,
		@RequestParam(value = "img", required = false) String imgUrl,
		@RequestParam("name") String name,
		@RequestParam("category_id") Integer categoryId,
		@RequestParam(value = "desc", required = false) String desc,
		@RequestParam("price") BigDecimal price
	) {
		return productService.create(image, imgUrl, name, categoryId, desc, price);
	}

	@PatchMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(
        @PathVariable("id") Integer id,
        @RequestParam(value = "image", required = false) MultipartFile image,
        @RequestParam(value = "img", required = false) String imgUrl,
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "category_id", required = false) Integer categoryId,
        @RequestParam(value = "desc", required = false) String desc,
        @RequestParam(value = "price", required = false) BigDecimal price
    ) {
		return productService.update(id, image, imgUrl, name, categoryId, desc, price);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
		return productService.delete(id);
	}
} 