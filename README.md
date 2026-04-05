# FashionShop Backend

FashionShop is a modular ecommerce backend built with Java Spring Boot 3 for a Software Engineering course project.

## Tech Stack
- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security + JWT
- Jakarta Validation
- Lombok
- Maven
- MySQL

## Package Structure
```text
com.example.fashionshop
├── common
├── config
├── security
└── modules
    ├── auth
    ├── user
    ├── category
    ├── product
    ├── cart
    ├── wishlist
    ├── order
    ├── payment
    ├── invoice
    ├── notification
    └── dashboard
```

## Database Setup
1. Create database:
```sql
CREATE DATABASE ecommerce_db;
```
2. Update credentials in `src/main/resources/application.properties`.
3. Set `spring.jpa.hibernate.ddl-auto=update` for auto-creation, or `none` if importing SQL dump.

## Import SQL Dump (Optional)
```bash
mysql -u root -p ecommerce_db < your_dump.sql
```

## Run Project
```bash
./mvnw spring-boot:run
```

## Default Roles
- CUSTOMER
- STAFF
- ADMIN

## Main API Groups
- `/api/auth/*`
- `/api/users/*`
- `/api/admin/users/*`
- `/api/categories/*`
- `/api/products/*`
- `/api/cart/*`
- `/api/wishlist/*`
- `/api/orders/*`
- `/api/payments/*`
- `/api/invoices/*`
- `/api/dashboard`
- `/api/home`

## Staff/Admin Order List (UC-16)
- **Endpoint:** `GET /api/orders`
- **Roles:** `STAFF`, `ADMIN`
- **Purpose:** Return paginated order summaries for order management screens.
- **Query params (optional):**
  - `page` (default: `0`)
  - `size` (default: `10`, max: `100`)
  - `status` (`PENDING`, `CONFIRMED`, `SHIPPING`, `COMPLETED`, `CANCELLED`)
  - `keyword` (search by customer/order contact info)
  - `sortBy` (`id`, `status`, `totalPrice`, `createdAt`, `updatedAt`)
  - `sortDir` (`asc`, `desc`)
- **Typical responses:**
  - Success with data: message `Order list fetched successfully`
  - Empty list: message `No orders found`
  - Forbidden: message `Access denied`
  - Load error: message `Failed to load order list`

Example dashboard navigation wiring (frontend):
```text
Dashboard Sidebar
└── Order Management
    ├── Order List  -> /admin/orders
    └── Order Detail -> /admin/orders/:orderId
```

## Notes
- API responses follow `{ success, message, data }` format.
- Passwords are encoded with BCrypt.
- JWT authentication is stateless.
