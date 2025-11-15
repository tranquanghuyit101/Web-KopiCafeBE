package com.kopi.kopi.dto.promo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromoProductDTO {
	private Integer id;
	private String name;
	private BigDecimal price;
	private String category_name;
}


