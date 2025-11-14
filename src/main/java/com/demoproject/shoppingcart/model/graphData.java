package com.demoproject.shoppingcart.model;

public class graphData {
	public graphData() {
		super();
		this.order_month=0;
		this.count=0;
		// TODO Auto-generated constructor stub
	}
	int order_month;
	long count;
	public int getOrder_month() {
		return order_month;
	}
	public void setOrder_month(int order_month) {
		this.order_month = order_month;
	}
	public long getCount() {
		return count;
	}
	public void setCount(long count) {
		this.count = count;
	}
	
	public graphData(int order_month, long count) {
		super();
		this.order_month = order_month;
		this.count = count;
	}
	
}
