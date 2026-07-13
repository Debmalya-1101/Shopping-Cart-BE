# <img src="https://nexis-store-sigma.vercel.app/logo.png" alt="Nexis Store Logo" height="30" style="vertical-align: middle;" /> Nexis Store Backend API

An enterprise-grade, feature-rich E-Commerce RESTful API built with **Java 21** and **Spring Boot 3.5**. This backend powers a complete modern shopping experience, including product scraping, inventory management, secure payments, and delivery tracking.

🌐 **Live Application:** The frontend application consuming this deployed Render backend is live at: [https://nexis-store-sigma.vercel.app/](https://nexis-store-sigma.vercel.app/)

## 🌟 Key Highlights for Recruiters & Project Managers
- **Modern Tech Stack**: Leverages the latest Java 21 features and Spring Boot 3.5 for high performance and maintainability.
- **Advanced Concurrency Handling**: Implements Optimistic Locking with Spring Retry & AOP to prevent race conditions during inventory checkout.
- **Integrated Web Scraping**: Utilizes Microsoft Playwright to scrape real-time product data (Amazon/Flipkart).
- **Enterprise Security**: Secure stateless authentication using robust JWTs alongside OAuth2 Client for Google/Facebook social logins.
- **Reliable Email System**: Replaces basic SMTP with the resilient Gmail REST API (Google API Client) for transactional emails.
- **Cloud & Production Ready**: Deployed on **Render** with a **TiDB Cloud Serverless** database. Includes Flyway for database versioning, Spring Boot Actuator for health monitoring, and Razorpay for seamless payment processing.
- **Comprehensive Logistics**: Dedicated modules for managing Delivery Partners, Shipments, and tracking Delivery Feedback.

## 🚀 Features

### 🛍️ Core E-Commerce
- **Product & Category Management**: Dynamic attributes, product images, and hierarchical categories.
- **Cart & Wishlist**: Real-time shopping cart management and user wishlists.
- **Order Management**: Comprehensive order lifecycle tracking (status updates, order items).
- **Inventory Management**: Transactional inventory tracking (restock, reserve, release) with optimistic locking.
- **Reviews & Ratings**: Product reviews and detailed delivery feedback mechanism.

### 🚚 Delivery & Logistics
- **Delivery Partner Portal**: Manage delivery agents, their status, and assigned shipments.
- **Shipment Tracking**: Track orders from dispatch to delivery.
- **Feedback System**: Granular delivery partner feedback and status monitoring.

### 💳 Payments & Checkout
- **Razorpay Integration**: Secure and seamless payment gateway integration (currently using **Razorpay Test API**).

### 🔐 Security & Users
- **Multi-Role Authentication**: Admin, User, and Delivery Partner roles.
- **OAuth2 Social Login**: Fully integrated **Google** and **Facebook** authentication for frictionless user onboarding.
- **JWT Authentication**: Token-based security with robust refresh token management.

### 🕸️ Scraper Module
- **Playwright Headless Browser**: Built-in scraper for Amazon and Flipkart to analyze competitor pricing or import products.

## 🛠️ Technology Stack
- **Core Framework**: Spring Boot 3.5.x, Java 21
- **Database ORM**: TiDB Cloud Serverless (MySQL Compatible), Spring Data JPA, Hibernate
- **Database Migrations**: Flyway
- **Security**: Spring Security, JWT (JJWT), OAuth2
- **Payments**: Razorpay Java SDK
- **API Documentation**: Springdoc OpenAPI (Swagger UI)
- **Web Scraping**: Microsoft Playwright
- **Emailing**: Google API Client (Gmail REST API)
- **Tools**: Lombok, Spring Retry, Actuator

## 📦 Getting Started

### Prerequisites
- Java 21
- Maven 3.8+
- MySQL Database

### Setup Instructions
1. **Clone the repository**:
   ```bash
   git clone <repo-url>
   cd Shopping-Cart-BE
   ```
2. **Configure Environment Variables**:
   Ensure you configure your `application.properties` or environment variables for:
   - MySQL Credentials
   - JWT Secret
   - Razorpay API Keys
   - Google OAuth/Gmail API Credentials

3. **Run the Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
4. **Access API Documentation**:
   Once the server is running, explore the API endpoints using Swagger UI:
   `http://localhost:8080/swagger-ui.html`

## 🏗️ Architecture & Documentation

### 1. Interactive API Documentation (Swagger UI)
![Swagger UI](docs/images/swagger-ui.png)

### 2. Database Schema (Entity-Relationship Diagram)

```mermaid
erDiagram
    APP_USER {
        Long id PK
    }
    ORDER {
        Long id PK
    }
    CART {
        Long id PK
    }
    CART_ITEM {
        Long id PK
    }
    WISHLIST_ITEM {
        Long id PK
    }
    ORDER_ITEM {
        Long id PK
    }
    SHIPMENT {
        Long id PK
    }
    PRODUCT {
        Long id PK
    }
    CATEGORY {
        Long id PK
    }
    INVENTORY {
        Long id PK
    }
    INVENTORY_TRANSACTION {
        Long id PK
    }
    DELIVERY_PARTNER {
        Long id PK
    }
    PRODUCT_REVIEW {
        Long id PK
    }
    DELIVERY_FEEDBACK {
        Long id PK
    }

    APP_USER ||--o| CART : "owns"
    APP_USER ||--o{ WISHLIST_ITEM : "adds"
    APP_USER ||--o{ ORDER : "places"
    CART ||--o{ CART_ITEM : "contains"
    ORDER ||--o{ ORDER_ITEM : "contains"
    ORDER ||--o| SHIPMENT : "tracked by"
    PRODUCT ||--o{ ORDER_ITEM : "ordered in"
    PRODUCT ||--o{ CART_ITEM : "added to"
    CATEGORY ||--o{ PRODUCT : "categorizes"
    PRODUCT ||--o| INVENTORY : "managed by"
    INVENTORY ||--o{ INVENTORY_TRANSACTION : "logged in"
    DELIVERY_PARTNER ||--o{ SHIPMENT : "delivers"
    APP_USER ||--o{ PRODUCT_REVIEW : "reviews"
    PRODUCT ||--o{ PRODUCT_REVIEW : "receives"
    DELIVERY_PARTNER ||--o{ DELIVERY_FEEDBACK : "receives"
```

> *(Note: Only primary keys are shown to keep the diagram clean and easy to read at a glance).*
