package com.kopi.kopi.dto.promo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromoDetailDTO {
	private Integer id;
	private String kind; // CODE | EVENT
	private String title;
	private String couponCode; // for code
	private String description;
	private String discountType;
	private BigDecimal discountValue;
	private BigDecimal minOrderAmount; // for code
	private Integer totalUsageLimit; // for code
	private Integer perUserLimit; // for code
	private LocalDateTime startsAt;
	private LocalDateTime endsAt;
	private Boolean active;
	private Boolean shippingFee;
	private List<Integer> productIds; // for event
	private List<PromoProductDTO> products; // for event
}


