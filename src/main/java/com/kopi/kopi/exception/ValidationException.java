package com.kopi.kopi.exception;

import java.util.List;
import java.util.Map;

public class ValidationException extends RuntimeException {
	private final List<Map<String, Object>> invalidProducts;

	public ValidationException(String message, List<Map<String, Object>> invalidProducts) {
		super(message);
		this.invalidProducts = invalidProducts;
	}

	public List<Map<String, Object>> getInvalidProducts() {
		return invalidProducts;
	}
}


