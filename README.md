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

## Staff/Admin Invoice Management (UC-19)
- **Invoice list endpoint:** `GET /api/invoices/manage`
- **Invoice detail endpoint:** `GET /api/invoices/manage/{invoiceId}`
- **Roles:** `STAFF`, `ADMIN`
- **Purpose:** Return paginated invoice summaries and read-only invoice details for internal management screens.
- **Query params (invoice list):**
  - `page` (default: `0`)
  - `size` (default: `10`, max: `100`)
  - `paymentStatus` (`PENDING`, `PAID`, `FAILED`, `REFUNDED`)
  - `keyword` (search by invoice number, customer name/email/phone)
  - `sortBy` (`id`, `invoiceNumber`, `totalAmount`, `paymentStatus`, `issuedAt`)
  - `sortDir` (`asc`, `desc`)
- **Typical responses:**
  - Success with data: message `Invoice list fetched successfully`
  - Empty list: message `No invoices available`
  - Invoice list load error: message `Unable to load invoices`
  - Invoice detail missing: message `Invoice not found`

Example dashboard navigation wiring (frontend):
```text
Dashboard Sidebar
└── Invoice Management
    ├── Invoice List   -> /admin/invoices
    └── Invoice Detail -> /admin/invoices/:invoiceId
```

## Customer Product Browsing (UC-23)
- **Browse endpoint:** `GET /api/store/products`
- **Public access:** Yes (no auth required)
- **Purpose:** Return customer-visible products for storefront browse page in a paginated format.
- **Query params:**
  - `page` (default: `0`)
  - `size` (default: `12`, max: `50`)
- **Response payload:**
  - `items[]` with `id`, `name`, `price`, `imageUrl`, `categoryName`, `shortDescription`, `inStock`, `productDetailUrl`
  - `page`, `size`, `totalItems`, `totalPages`
- **Typical responses:**
  - Success with data: message `Products fetched successfully`
  - Empty list: message `No products available`
  - Invalid paging: message `Invalid pagination parameters`
  - Retrieval failure: message `Unable to load products`

Example storefront navigation wiring (frontend):
```text
Header
└── Shop / Browse Products -> /products
    ├── Product Grid Page calls GET /api/store/products?page=0&size=12
    └── Product Card click -> /products/:productId
```

## Notes
- API responses follow `{ success, message, data }` format.
- Passwords are encoded with BCrypt.
- JWT authentication is stateless.

## Customer Add to Cart (UC-24)
- **Endpoint:** `POST /api/cart/items`
- **Role:** `CUSTOMER`
- **Request body:**
  ```json
  {
    "productId": 101,
    "quantity": 2
  }
  ```
- **Behavior:**
  - Validates `quantity > 0` (message: `Invalid quantity`)
  - Validates product exists and is active/in stock
  - Merges quantity when the same product already exists in cart
  - Validates requested quantity does not exceed stock (message: `Insufficient stock available`)
  - Returns full cart payload with `totalItems` for instant cart badge updates

### Cart badge/header integration sample
Use either:
- `GET /api/cart/summary` to fetch cart badge count only
- or `POST /api/cart/items` response `data.totalItems` right after adding to cart

Example add-to-cart success response:
```json
{
  "success": true,
  "message": "Added to cart successfully",
  "data": {
    "cartId": 12,
    "totalItems": 5,
    "distinctItemCount": 2,
    "totalPrice": 319.95,
    "items": [
      {
        "itemId": 41,
        "productId": 101,
        "productName": "Classic Shirt",
        "price": 79.99,
        "quantity": 3,
        "lineTotal": 239.97
      }
    ]
  }
}
```

Frontend mapping recommendation for product detail page:
1. Render quantity selector (`+`/`-` + numeric input).
2. Disable **Add to cart** button while request is pending.
3. On submit:
   - if quantity invalid, show `Invalid quantity`
   - if API returns stock conflict, show `Insufficient stock available`
   - if API returns 500/cart update error, show `Unable to add item to cart`
4. On success, show toast and update header badge with `data.totalItems`.

## Customer Adjust Quantity In Cart (UC-27)
- **Endpoints:** `PUT /api/cart/items/{itemId}` or `PUT /api/cart/items/{itemId}/quantity`
- **Role:** `CUSTOMER`
- **Request body:**
  ```json
  {
    "quantity": 3
  }
  ```
- **Behavior:**
  - Validates `quantity > 0` (message: `Invalid quantity`)
  - Validates product still active and checks stock availability
  - If quantity exceeds stock, returns `Insufficient stock available`
  - Recalculates line subtotal + cart totals and returns updated cart state
  - Cart page can stay in place and immediately rerender totals/header badge from `data`

Example response highlights:
- `data.items[].quantity` for each row quantity selector
- `data.items[].lineTotal` for per-item subtotal updates
- `data.subtotal` and `data.totalPrice` for cart summary updates
- `data.totalItems` for header cart badge sync
## Customer Storefront Search (UC-22)
- **Endpoint:** `GET /api/products/search?keyword=...`
- **Role:** Public customer storefront endpoint.
- **Purpose:** Search active products by product name, category/type, and description keyword.
- **Behavior:**
  - Trims incoming keyword
  - Returns **top 5** most relevant products only
  - Empty keyword -> `400` with message `Please enter a keyword`
  - No matching products -> `200` with message `No results found` and empty array
  - Query failure -> `500` with message `Unable to load search results`
- **Search item fields:** id, slug, name, category/type, price, thumbnail, short description snippet, stock status, detail URL.

Example storefront integration point (frontend header/navbar):
```text
Storefront Header
├── Logo
├── Category Menu
├── Search Bar (Enter key or Search button)
│   └── Calls GET /api/products/search?keyword={query}
└── Cart Icon
```
