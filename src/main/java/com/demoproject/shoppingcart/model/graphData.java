package com.demoproject.shoppingcart.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class graphData {
	public graphData() {
		super();
		this.order_month=0;
		this.count=0;
		// TODO Auto-generated constructor stub
	}
	int order_month;
	long count;

    public graphData(int order_month, long count) {
		super();
		this.order_month = order_month;
		this.count = count;
	}
}
