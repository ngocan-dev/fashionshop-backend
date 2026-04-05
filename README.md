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
## Customer View Cart (UC-26)
- **Cart page endpoint:** `GET /api/cart`
- **Cart badge count endpoint:** `GET /api/cart/summary`
- **Role:** `CUSTOMER`
- **Purpose:** Return active cart items and summary data for the dedicated cart page while keeping header badge sync-ready.
- **Response payload (`data`):**
  - `items[]` with `productImage`, `productName`, `quantity`, `price`, `lineTotal`
  - `subtotal`, `totalPrice`, `totalItems`, `distinctItemCount`, `empty`
- **Typical responses:**
  - Success with items: message `Cart fetched successfully`
  - Empty cart state: `empty: true` (frontend shows `Your cart is empty`)
  - Retrieval failure: message `Unable to load cart items`

## Customer View Order History (UC-33)
- **Endpoint:** `GET /api/orders/my/history`
- **Role:** `CUSTOMER`
- **Purpose:** Return a paginated order-history list for the currently authenticated customer only.
- **Auth/ownership rules:**
  - Requires valid customer JWT/session.
  - The API always filters by current authenticated customer and never exposes other customers' orders.
- **Query params (optional):**
  - `page` (default: `0`)
  - `size` (default: `10`, max: `50`)
  - `status` (`PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `COMPLETED`, `CANCELLED`)
  - `sortBy` (`id`, `status`, `totalPrice`, `createdAt`, `updatedAt`)
  - `sortDir` (`asc`, `desc`)
- **History row fields (`data.items[]`):**
  - `orderId`
  - `orderCode` (invoice/order reference when available)
  - `orderDate`
  - `totalAmount`
  - `orderStatus`
  - plus optional enrichments: `paymentStatus`, `paymentMethod`, `itemCount`, `shippingStatus`, `updatedAt`
- **Typical responses:**
  - Success with data: message `Order history fetched successfully`
  - Empty list: message `No order history available`
  - Retrieval failure: message `Unable to load order history`

Example customer account navigation wiring (frontend):
```text
Account Menu
├── Profile         -> /account/profile
├── Order History   -> /account/orders
└── Wishlist        -> /account/wishlist

Order History Page (/account/orders)
└── Calls GET /api/orders/my/history?page=0&size=10
    └── Clicking one row navigates to /account/orders/:orderId
        └── Detail API: GET /api/orders/my/{orderId}
```

Example storefront integration wiring (frontend):
```text
Storefront Header
└── Cart Icon -> /cart
    ├── Cart Page calls GET /api/cart
    ├── Renders item image/name/qty/price/line total
    ├── Renders subtotal + total and Checkout/Buy Now CTA
    └── Uses empty + message states for fallback UI
```
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

## Customer Add to Wishlist (UC-29)
- **Endpoint:** `POST /api/wishlist/items`
- **Role:** `CUSTOMER`
- **Request body:**
  ```json
  {
    "productId": 101
  }
  ```
- **Behavior:**
  - Requires authenticated customer (JWT)
  - Validates product id and product existence
  - Prevents duplicates for `(user_id, product_id)` pair
  - Returns `alreadyInWishlist` to support duplicate/informational UI state
  - Returns `wishlistCount` for future wishlist badge support
  - Update failures return `500` with message `Unable to add product to wishlist`

Optional helper endpoint for heart state:
- `GET /api/wishlist/items/contains/{productId}`

Example responses:
```json
{
  "success": true,
  "message": "Added to wishlist successfully",
  "data": {
    "alreadyInWishlist": false,
    "wishlistCount": 5,
    "item": {
      "wishlistId": 31,
      "productId": 101,
      "productName": "Classic Blazer",
      "price": 120.00,
      "imageUrl": "https://cdn.example.com/product-101.jpg",
      "createdAt": "2026-04-05T09:40:00"
    }
  }
}
```

```json
{
  "success": true,
  "message": "Product already in wishlist",
  "data": {
    "alreadyInWishlist": true,
    "wishlistCount": 5,
    "item": {
      "wishlistId": 31,
      "productId": 101,
      "productName": "Classic Blazer",
      "price": 120.00,
      "imageUrl": "https://cdn.example.com/product-101.jpg",
      "createdAt": "2026-04-05T09:40:00"
    }
  }
}
```

### Frontend integration samples (product detail + product card)
1. **Product detail page**
   - Place a heart icon button near **Add to Cart / Buy Now**.
   - On click:
     - Disable button while awaiting API.
     - Call `POST /api/wishlist/items` with current `productId`.
     - If response message is `Added to wishlist successfully` -> show success toast and fill heart icon.
     - If response message is `Product already in wishlist` -> show informational toast and keep filled heart.
     - On failure -> show `Unable to add product to wishlist`.

2. **Product card/grid item**
   - Reuse the same wishlist-heart component used by detail page.
   - Keep shared states: `idle`, `loading`, `added`, `duplicate`, `error`.
   - Optionally preload `GET /api/wishlist/items/contains/{productId}` for initial heart fill state.

3. **Unauthenticated behavior**
   - API returns `401` when token is missing/invalid.
   - Frontend should follow existing auth flow: redirect to login page or open login modal.

## Customer Place Order / Checkout (UC-30)
- **Checkout summary endpoint:** `GET /api/orders/checkout-summary`
- **Place order endpoint:** `POST /api/orders`
- **Role:** `CUSTOMER`
- **Purpose:** Support customer checkout page by loading active cart summary, validating shipping + payment info, and creating order transactionally from active cart.

### Place-order request body
```json
{
  "receiverName": "Nguyen Van A",
  "phone": "+84901234567",
  "shippingAddress": "123 Main St",
  "district": "District 1",
  "city": "Ho Chi Minh City",
  "province": "Ho Chi Minh",
  "postalCode": "700000",
  "note": "Please call before delivery",
  "paymentMethod": "COD"
}
```

### Checkout behavior highlights
- Returns `Cart is empty` when active cart has no items.
- Validates required fields: receiver name, valid phone format, shipping address, payment method.
- Re-validates stock and product availability before creating order.
- Creates order + order items snapshot + payment + invoice in one transaction.
- Clears cart items after successful order placement.
- Returns `Order placement failed` on unexpected server-side creation failure.

### Suggested storefront navigation
```text
Storefront Header
└── Cart Icon -> /cart
    └── Checkout CTA ("Proceed to checkout") -> /checkout
        ├── GET /api/orders/checkout-summary
        └── POST /api/orders on Confirm button
            └── Redirect to /orders/:orderId (or /orders/success)
```

## Customer View Payment Status (UC-37)
- **Order detail payment endpoint:** `GET /api/orders/my/{orderId}/payment`
- **Payment confirmation endpoint:** `GET /api/payments/orders/{orderId}/summary`
- **Role:** `CUSTOMER`
- **Purpose:** Return customer-safe payment status/details for an order and enforce ownership.

### Behavior
- Verifies the current authenticated customer owns the order.
- Returns `403` when customer attempts to access another customer's order.
- Returns `404` when order does not exist.
- Returns success with message `Payment information not available` when no payment record exists yet.
- Returns success with message `Payment status fetched successfully` when payment record is found.
- Returns `500` with message `Unable to load payment status` for unexpected retrieval failures.

### Response highlights (`data`)
- `paymentStatus`: `pending`, `paid`, `failed`, `cancelled`
- `paymentMethod`, `paymentDateTime`, `transactionReference`, `paidAmount`
- `orderId`, `orderCode`, `orderTotalAmount`, `orderStatus`
- Optional extension fields: `failureReason`, `gatewayProvider`, `lastUpdatedAt`, `refundStatus`
- `retryAllowed` for future retry-payment CTA (`true` for failed/pending)

### Frontend integration sample
1. **Order detail page** (`/account/orders/:orderId`)
   - Call `GET /api/orders/my/{orderId}/payment`.
   - Render status badge + payment summary card.
   - If API message is `Payment information not available`, show empty state card.

2. **Payment confirmation page** (`/orders/success` or `/orders/:orderId/confirmation`)
   - Call `GET /api/payments/orders/{orderId}/summary` after redirect from checkout/payment callback.
   - Show paid/failed/pending messaging and transaction reference.

3. **Optional order-history badge**
   - Reuse `paymentStatus` from `GET /api/orders/my/history` rows to display compact badge in list.
