package com.kopi.kopi.dto.promo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePromoDTO {
	private String name;
	private String desc;
	private String discount_type;
	private String discount_value;
	private String coupon_code;
	private String min_order_amount;
	private String total_usage_limit;
	private String per_user_limit;
	private String start_date;
	private String end_date;
	private List<Integer> product_ids;
	@JsonProperty("is_shipping_fee")
	private Boolean shippingFee;
}


