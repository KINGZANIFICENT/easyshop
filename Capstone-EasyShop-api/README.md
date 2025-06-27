# EasyShop API - E-Commerce Backend

## Project Overview

EasyShop is a comprehensive e-commerce API built with **Spring Boot 2.7.3** and **MySQL**. This project represents my backend development work on Version 2 of an existing e-commerce platform, where I implemented new features, fixed critical bugs, and enhanced the overall functionality of the shopping experience.

The application uses JWT authentication, role-based security, and follows RESTful design principles to provide a robust foundation for e-commerce operations.

## 🚀 Features Implemented

### ✅ Categories Management (Phase 1)
- **Full CRUD operations** for product categories with admin-only restrictions
- **Robust error handling** with proper HTTP status codes
- **Category-specific product listing** via `/categories/{id}/products` endpoint
- Complete controller and DAO implementation with transaction safety

### ✅ Product Search Bug Fix (Phase 2)
- **Fixed search functionality** that was returning incorrect results
- **Resolved product duplication bug** where updates created new records instead of modifying existing ones
- **Comprehensive search parameters**: category, price range, and color filtering
- **Unit testing** implemented to validate bug fixes

### ✅ Shopping Cart System (Phase 3)
- **User-specific persistent shopping cart** with JWT-based authentication
- **Smart quantity management** - automatically increments if item exists, creates new if it doesn't
- **Comprehensive cart operations**: add, update quantity, clear entire cart
- **Automatic total calculations** with discount support built-in

### ✅ User Profile Management (Phase 4)
- **Secure profile viewing and editing** with user authentication
- **Complete profile management** including personal details, contact info, and address
- **User-specific access control** ensuring users can only access their own profiles

## 🛠️ Technology Stack

- **Backend**: Spring Boot 2.7.3
- **Database**: MySQL 8.0.33
- **Security**: Spring Security with JWT (jjwt 0.11.1)
- **Build Tool**: Maven
- **Java Version**: 17
- **Testing**: JUnit 5, Spring Boot Test

## 📋 API Endpoints

### Categories
```http
GET    /categories           # Get all categories
GET    /categories/{id}      # Get category by ID
GET    /categories/{id}/products  # Get all products in category
POST   /categories           # Create new category (Admin only)
PUT    /categories/{id}      # Update category (Admin only)
DELETE /categories/{id}      # Delete category (Admin only)
```

### Products (With Bug Fixes Applied)
```http
GET    /products                     # Search/filter products
GET    /products?cat=1               # Filter by category
GET    /products?minPrice=25&maxPrice=100  # Price range filter
GET    /products?color=red           # Color filter
GET    /products/{id}                # Get product by ID
POST   /products                     # Create product (Admin only)
PUT    /products/{id}                # Update product (Admin only)
DELETE /products/{id}                # Delete product (Admin only)
```

### Shopping Cart
```http
GET    /cart                    # Get user's cart
POST   /cart/products/{id}      # Add product to cart
PUT    /cart/products/{id}      # Update quantity in cart
DELETE /cart                    # Clear entire cart
```

### User Profile
```http
GET    /profile          # Get user profile
PUT    /profile          # Update user profile
```

## 🔧 Key Implementation Details

### 1. Categories Controller with Security

```java
@RestController
@RequestMapping("/categories")
@CrossOrigin
public class CategoriesController {
    
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Category addCategory(@RequestBody Category category) {
        try {
            return categoryDao.create(category);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable int id) {
        try {
            Category category = categoryDao.getById(id);
            if (category == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            categoryDao.delete(id);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad");
        }
    }
}
```

### 2. Product Search Bug Fix

**Problem**: The original search functionality was returning incorrect results due to improper parameter handling.

**My Solution**: Implemented a robust search method with proper null handling and parameter binding:

```java
@Override
public List<Product> search(Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String color) {
    List<Product> products = new ArrayList<>();

    String sql = "SELECT * FROM products " +
            "WHERE (category_id = ? OR ? = -1) " +
            "   AND (price >= ? OR ? = -1)" +
            "   AND (price <= ? OR ? = -1) " +
            "   AND (color = ? OR ? = '') ";

    // Handle null parameters by converting to sentinel values
    categoryId = categoryId == null ? -1 : categoryId;
    minPrice = minPrice == null ? new BigDecimal("-1") : minPrice;
    maxPrice = maxPrice == null ? new BigDecimal("-1") : maxPrice;
    color = color == null ? "" : color;

    try (Connection connection = getConnection()) {
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, categoryId);
        statement.setInt(2, categoryId);
        statement.setBigDecimal(3, minPrice);
        statement.setBigDecimal(4, minPrice);
        statement.setBigDecimal(5, maxPrice);
        statement.setBigDecimal(6, maxPrice);
        statement.setString(7, color);
        statement.setString(8, color);
        
        ResultSet row = statement.executeQuery();
        while (row.next()) {
            products.add(mapRow(row));
        }
    }
    return products;
}
```

### 3. Shopping Cart Implementation with Smart Logic

**Highlight**: Item management that automatically handles quantity increments:

```java
@Override
public void addItem(int userId, int productId, int quantity) {
    String checkQuery = "SELECT quantity FROM shopping_cart WHERE user_id = ? AND product_id = ?";
    String insertQuery = "INSERT INTO shopping_cart (user_id, product_id, quantity) VALUES (?, ?, ?)";
    String updateQuery = "UPDATE shopping_cart SET quantity = quantity + ? WHERE user_id = ? AND product_id = ?";

    try (Connection connection = getConnection()) {
        try (PreparedStatement checkPs = connection.prepareStatement(checkQuery)) {
            checkPs.setInt(1, userId);
            checkPs.setInt(2, productId);

            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    // Item exists, increment quantity
                    try (PreparedStatement updatePs = connection.prepareStatement(updateQuery)) {
                        updatePs.setInt(1, quantity);
                        updatePs.setInt(2, userId);
                        updatePs.setInt(3, productId);
                        updatePs.executeUpdate();
                    }
                } else {
                    // New item, insert with specified quantity
                    try (PreparedStatement insertPs = connection.prepareStatement(insertQuery)) {
                        insertPs.setInt(1, userId);
                        insertPs.setInt(2, productId);
                        insertPs.setInt(3, quantity);
                        insertPs.executeUpdate();
                    }
                }
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("Error adding item to cart", e);
    }
}
```

### 4. Profile Management with Principal-Based Security

```java
@GetMapping
public Profile getProfile(Principal principal) {
    try {
        String userName = principal.getName();
        User user = userDao.getByUserName(userName);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        Profile profile = profileDao.getByUserId(user.getId());

        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        }

        return profile;
    } catch (ResponseStatusException ex) {
        throw ex;
    } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
    }
}
```

## 🔐 Security Implementation

- **JWT Authentication**: Secure token-based authentication system
- **Role-Based Authorization**: 
  - `@PreAuthorize("hasRole('ROLE_ADMIN')")` for admin-only operations
  - `@PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")` for user operations
- **Principal Injection**: Secure user context retrieval in all user-specific operations
- **CORS Configuration**: Cross-origin resource sharing enabled for frontend integration

## 📊 Database Design

The application utilizes a well-structured MySQL database with these key tables:
- `users` - User authentication and basic info
- `profiles` - Extended user profile information  
- `categories` - Product categorization
- `products` - Product catalog with search indices
- `shopping_cart` - User cart items with quantity tracking

## 🚦 Getting Started

1. **Clone the repository**
```bash
git clone https://github.com/Jrobinson718/Capstone-EasyShop-api.git
```

2. **Set up MySQL database**
```bash
# Execute the provided database script
mysql -u root -p < database/create_database.sql
```

3. **Configure application properties**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/easyshop
spring.datasource.username=your_username
spring.datasource.password=your_password
```

4. **Run the application**
```bash
mvn spring-boot:run
```

5. **Test with sample users**
```json
// Admin user
{
  "username": "admin",
  "password": "password"
}

// Regular user
{
  "username": "user", 
  "password": "password"
}
```

## 🔍 Bug Fixes Implemented

### Bug 1: Product Search Functionality
- **Issue**: Search parameters weren't being handled correctly, causing incorrect results
- **Root Cause**: Improper null parameter handling in SQL queries
- **Solution**: Implemented sentinel value approach with proper parameter binding
- **Testing**: Created unit tests to validate all search combinations

### Bug 2: Product Update Duplication
- **Issue**: Product updates were creating new records instead of updating existing ones
- **Root Cause**: Missing WHERE clause parameter in UPDATE statement
- **Solution**: Fixed SQL statement with proper parameter binding
- **Validation**: Verified through Postman testing that updates now modify existing records

## 📈 What I Learned

Through this project, I deepened my expertise in:

- **Spring Boot Architecture**: RESTful API design, dependency injection, and configuration
- **Spring Security**: JWT implementation, role-based access control, and method-level security
- **Database Operations**: Complex SQL queries, transaction management, and connection pooling
- **Error Handling**: Comprehensive exception handling with proper HTTP status codes
- **Testing Methodologies**: Integration testing, and API testing with Postman
- **Code Documentation**: Writing clear, maintainable code with proper documentation

## 🎯 Project Highlights

1. **Robust Error Handling**: Consistent error responses with meaningful HTTP status codes
2. **Security-First Design**: All sensitive operations properly secured with role-based access
3. **Database Optimization**: Efficient queries with proper indexing and connection management
4. **Code Quality**: Clean, well-documented code following Spring Boot best practices
5. **Comprehensive Testing**: Both unit tests and integration tests for critical functionality

## 🔮 Future Enhancements

While this version successfully implements core e-commerce functionality, potential future features could include:
- Order checkout and payment processing integration
- Product reviews and rating system
- Real-time inventory management
- Email notification system

---
