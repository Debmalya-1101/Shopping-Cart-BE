package com.demoproject.shoppingcart.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "products") 
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
    private String description;
    private Long price;
    private String imageUrl;
    //for product details
    private String details;
    private String os;
    private String processor;
    private String ram_rom;
    private String rear_camera;
    private String front_camera;
    private String display;
    private String battery;
    
	public Product() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	
	public Product(Long id, String name, String description, Long price, String imageUrl, String details, String os,
			String processor, String ram_rom, String rear_camera, String front_camera, String display, String battery) {
		super();
		this.id = id;
		this.name = name;
		this.description = description;
		this.price = price;
		this.imageUrl = imageUrl;
		this.details = details;
		this.os = os;
		this.processor = processor;
		this.ram_rom = ram_rom;
		this.rear_camera = rear_camera;
		this.front_camera = front_camera;
		this.display = display;
		this.battery = battery;
	}


	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Long getPrice() {
		return price;
	}
	public void setPrice(Long price) {
		this.price = price;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}


	public String getDetails() {
		return details;
	}


	public void setDetails(String details) {
		this.details = details;
	}


	public String getOs() {
		return os;
	}


	public void setOs(String os) {
		this.os = os;
	}


	public String getProcessor() {
		return processor;
	}


	public void setProcessor(String processor) {
		this.processor = processor;
	}


	public String getRam_rom() {
		return ram_rom;
	}


	public void setRam_rom(String ram_rom) {
		this.ram_rom = ram_rom;
	}


	public String getRear_camera() {
		return rear_camera;
	}


	public void setRear_camera(String rear_camera) {
		this.rear_camera = rear_camera;
	}


	public String getFront_camera() {
		return front_camera;
	}


	public void setFront_camera(String front_camera) {
		this.front_camera = front_camera;
	}


	public String getDisplay() {
		return display;
	}


	public void setDisplay(String display) {
		this.display = display;
	}


	public String getBattery() {
		return battery;
	}


	public void setBattery(String battery) {
		this.battery = battery;
	}
}
