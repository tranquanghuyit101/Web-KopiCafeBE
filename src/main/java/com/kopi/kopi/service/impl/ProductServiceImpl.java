package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.Category;
import com.kopi.kopi.entity.Product;
import com.kopi.kopi.entity.ProductAddOn;
import com.kopi.kopi.entity.ProductSize;
import com.kopi.kopi.repository.CategoryRepository;
import com.kopi.kopi.repository.ProductAddOnRepository;
import com.kopi.kopi.repository.ProductRepository;
import com.kopi.kopi.repository.ProductSizeRepository;
import com.kopi.kopi.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductSizeRepository productSizeRepository;
    private final ProductAddOnRepository productAddOnRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository, ProductSizeRepository productSizeRepository, ProductAddOnRepository productAddOnRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productSizeRepository = productSizeRepository;
        this.productAddOnRepository = productAddOnRepository;
    }

    @Override
    public Map<String, Object> list(Integer categoryId, String orderBy, String sort, String searchByName, Integer limit, Integer page) {
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
            pageData = productRepository.findByAvailableTrueAndCategory_CategoryIdAndNameContainingIgnoreCase(categoryId, searchByName == null ? "" : searchByName, pageable);
        } else {
            pageData = productRepository.findByAvailableTrueAndNameContainingIgnoreCase(searchByName == null ? "" : searchByName, pageable);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Product p : pageData.getContent()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getProductId());
            m.put("name", p.getName());
            m.put("img", p.getImgUrl());
            m.put("price", p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
            m.put("stock", p.getStockQty());
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

    @Override
    public Map<String, Object> detail(Integer id) {
        Product p = productRepository.findById(id).orElseThrow();
        Map<String, Object> item = new HashMap<>();
        item.put("id", p.getProductId());
        item.put("name", p.getName());
        item.put("img", p.getImgUrl());
        item.put("price", p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
        item.put("stock", p.getStockQty());
        item.put("desc", p.getDescription());
        item.put("category_id", p.getCategory() != null ? p.getCategory().getCategoryId() : null);
        // sizes from DB (available only), include delta and computed price
        try {
            List<Map<String, Object>> sizes = new ArrayList<>();
            for (ProductSize ps : productSizeRepository.findByProduct_ProductIdAndAvailableTrue(p.getProductId())) {
                var s = ps.getSize();
                Map<String, Object> m = new HashMap<>();
                m.put("size_id", s != null ? s.getSizeId() : null);
                m.put("name", s != null ? s.getName() : null);
                m.put("code", s != null ? s.getCode() : null);
                BigDecimal base = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
                BigDecimal delta = ps.getPrice() != null ? ps.getPrice() : BigDecimal.ZERO;
                m.put("price_delta", delta);
                m.put("price", base.add(delta));
                m.put("available", Boolean.TRUE.equals(ps.getAvailable()));
                sizes.add(m);
            }
            item.put("sizes", sizes);
        } catch (Exception ignored) {
            item.put("sizes", List.of());
        }
        // add-ons from DB (available only)
        try {
            List<Map<String, Object>> addons = new ArrayList<>();
            for (ProductAddOn pa : productAddOnRepository.findByProduct_ProductIdAndAvailableTrue(p.getProductId())) {
                var a = pa.getAddOn();
                Map<String, Object> m = new HashMap<>();
                m.put("add_on_id", a != null ? a.getAddOnId() : null);
                m.put("name", a != null ? a.getName() : null);
                m.put("price", pa.getPrice() != null ? pa.getPrice() : BigDecimal.ZERO);
                m.put("available", Boolean.TRUE.equals(pa.getAvailable()));
                addons.add(m);
            }
            item.put("add_ons", addons);
        } catch (Exception ignored) {
            item.put("add_ons", List.of());
        }
        return Map.of("data", List.of(item));
    }

    @Override
    public ResponseEntity<?> create(org.springframework.web.multipart.MultipartFile image, String imgUrl, String name, Integer categoryId, String desc, BigDecimal price) {
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setPrice(price);
        if (imgUrl != null && !imgUrl.isBlank()) {
            p.setImgUrl(imgUrl);
        }
        p.setDescription(desc);
        // Set required defaults for non-nullable columns
        if (p.getAvailable() == null) p.setAvailable(true);
        if (p.getStockQty() == null) p.setStockQty(0);
        if (p.getCreatedAt() == null) p.setCreatedAt(java.time.LocalDateTime.now());
        if (p.getUpdatedAt() == null) p.setUpdatedAt(java.time.LocalDateTime.now());
        productRepository.save(p);

        Map<String, Object> item = new HashMap<>();
        item.put("id", p.getProductId());
        item.put("name", p.getName());
        item.put("img", p.getImgUrl());
        item.put("price", p.getPrice());
        item.put("desc", p.getDescription());
        item.put("category_id", p.getCategory() != null ? p.getCategory().getCategoryId() : null);
        return ResponseEntity.ok(Map.of("data", List.of(item)));
    }

    @Override
    public ResponseEntity<?> update(Integer id, org.springframework.web.multipart.MultipartFile image, String imgUrl, String name, Integer categoryId, String desc, BigDecimal price) {
        Product p = productRepository.findById(id).orElseThrow();
        if (name != null && !name.isBlank()) {
            p.setName(name);
        }
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId).orElseThrow();
            p.setCategory(category);
        }
        if (price != null) {
            p.setPrice(price);
        }
        if (imgUrl != null && !imgUrl.isBlank()) {
            p.setImgUrl(imgUrl);
        }
        p.setDescription(desc);
        productRepository.save(p);

        Map<String, Object> item = new HashMap<>();
        item.put("id", p.getProductId());
        item.put("name", p.getName());
        item.put("img", p.getImgUrl());
        item.put("price", p.getPrice());
        item.put("desc", p.getDescription());
        item.put("category_id", p.getCategory() != null ? p.getCategory().getCategoryId() : null);
        return ResponseEntity.ok(Map.of("data", List.of(item)));
    }

    @Override
    public ResponseEntity<?> delete(Integer id) {
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) {
            return ResponseEntity.status(404).body(Map.of("message", "not_found"));
        }
        // Soft delete: mark unavailable
        p.setAvailable(false);
        // Optional: also zero out stock or keep it; we keep stock as-is for audit
        productRepository.save(p);
        return ResponseEntity.ok(Map.of("message", "soft_deleted"));
    }
}

