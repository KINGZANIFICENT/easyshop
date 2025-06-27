package org.yearup.data.mysql;

import org.springframework.stereotype.Component;
import org.yearup.data.ProductDao;
import org.yearup.data.ShoppingCartDao;
import org.yearup.models.Product;
import org.yearup.models.ShoppingCart;
import org.yearup.models.ShoppingCartItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class MySqlShoppingCartDao extends MySqlDaoBase implements ShoppingCartDao {

    private ProductDao productDao;

    public MySqlShoppingCartDao(DataSource dataSource, ProductDao productDao) {
        super(dataSource);
        this.productDao = productDao;
    }

    /**
     * Retrieves the shopping cart for a specific user.
     *
     * This method queries the database to get all items and their quantities
     * associated with the given {@code userId}. It then creates a {@link ShoppingCart}
     * object by getting the product details for each item.
     *
     * @param userId The unique identifier of the user whose shopping cart is to be retrieved.
     * @return A {@link ShoppingCart} object containing all items currently in the user's cart.
     * Returns an empty shopping cart if the user has no items.
     * @throws RuntimeException If a SQL exception occurs during the database operation.
     */
    @Override
    public ShoppingCart getByUserId(int userId) {
        ShoppingCart cart = new ShoppingCart();

        String query = """
                SELECT product_id, quantity
                FROM shopping_cart
                WHERE user_id = ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)){

            ps.setInt(1, userId);

            try (ResultSet results = ps.executeQuery()){
                while (results.next()) {
                    int productId = results.getInt("product_id");
                    int quantity = results.getInt("quantity");

                    Product product = productDao.getById(productId);

                    if (product != null) {
                        ShoppingCartItem item = new ShoppingCartItem();
                        item.setProduct(product);
                        item.setQuantity(quantity);
                        cart.add(item);
                    }
                }
            }
        }catch (SQLException e ) {
            throw new RuntimeException("Error getting shopping cart for user " + userId, e);
        }
        return cart;
    }

    /**
     * Adds an item to a user's shopping cart or updates its quantity if already present.
     *
     * This method first checks if the product already exists in the user's cart.
     * If it does, the quantity is increased by the specified amount. If not, a new
     * product is created in the shopping cart.
     *
     * @param userId The unique identifier of the user.
     * @param productId The ID of the product to add or update.
     * @param quantity The amount to add to the product's quantity in the cart.
     * @throws RuntimeException If a SQL exception occurs during the database operation.
     */
    @Override
    public void addItem(int userId, int productId, int quantity) {
        String checkQuery = "SELECT quantity FROM shopping_cart WHERE user_id = ? AND product_id = ?";
        String insertQuery = "INSERT INTO shopping_cart (user_id, product_id, quantity) VALUES (?, ?, ?)";
        String updateQuery = "UPDATE shopping_cart SET quantity = quantity + ? WHERE user_id = ? AND product_id = ?";

        try (Connection connection = getConnection()) {
            try (PreparedStatement checkPs = connection.prepareStatement(checkQuery)){
                checkPs.setInt(1, userId);
                checkPs.setInt(2, productId);

                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        try (PreparedStatement updatePs = connection.prepareStatement(updateQuery)){
                            updatePs.setInt(1, quantity);
                            updatePs.setInt(2, userId);
                            updatePs.setInt(3, productId);
                            updatePs.executeUpdate();

                        }
                    }else {
                        try (PreparedStatement insertPs = connection.prepareStatement(insertQuery)) {
                            insertPs.setInt(1, userId);
                            insertPs.setInt(2, productId);
                            insertPs.setInt(3, quantity);
                            insertPs.executeUpdate();

                        }
                    }
                }
            }
        }catch (SQLException e) {
            throw new RuntimeException("Error adding item to card", e);
        }
    }

    /**
     * Updates the quantity of a specific item in a user's shopping cart.
     *
     * If the {@code quantity} provided is less than or equal to 0, this method
     * will effectively remove the item from the cart by calling {@code removeItem}.
     * Otherwise, it updates the item's quantity to the new specified value.
     *
     * @param userId The unique identifier of the user whose cart item is being updated.
     * @param productId The ID of the product to update.
     * @param quantity The new quantity for the product in the cart. If <= 0, the item is removed.
     * @throws RuntimeException If a SQL exception occurs during the database operation.
     */
    @Override
    public void updateItem(int userId, int productId, int quantity) {
        if (quantity <= 0) {
            removeItem(userId, productId);
            return;
        }

        String query = "UPDATE shopping_cart SET quantity = ? WHERE user_id = ? AND product_id = ?";

        try(Connection connection = getConnection();
        PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, quantity);
            ps.setInt(2, userId);
            ps.setInt(3, productId);
            ps.executeUpdate();

        }catch (SQLException e) {
            throw new RuntimeException("Error updating cart item", e);
        }
    }

    /**
     * Removes a specific item from a user's shopping cart.
     *
     * This method deletes the entry for a given product and user from the shopping cart table.
     *
     * @param userId The unique identifier of the user.
     * @param productId The ID of the product to remove from the cart.
     * @throws RuntimeException If a SQL exception occurs during the database operation.
     */
    @Override
    public void removeItem(int userId, int productId) {
        String query = "DELETE FROM shopping_cart WHERE user_id = ? AND product_id = ?";

        try (Connection connection = getConnection();
        PreparedStatement ps = connection.prepareStatement(query)){

            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.executeUpdate();

        }catch (SQLException e) {
            throw new RuntimeException("Error removing cart item", e);
        }
    }

    /**
     * Clears all items from a user's shopping cart.
     *
     * This method deletes all entries associated with a specific user from the shopping cart table,
     * effectively emptying their cart.
     *
     * @param userId The unique identifier of the user whose shopping cart is to be cleared.
     * @throws RuntimeException If a SQL exception occurs during the database operation.
     */
    @Override
    public void delete(int userId) {
        String query = "DELETE FROM shopping_cart WHERE user_id = ?";

        try (Connection connection = getConnection();
        PreparedStatement ps = connection.prepareStatement(query)){

            ps.setInt(1, userId);
            ps.executeUpdate();

        }catch (SQLException e) {
            throw new RuntimeException("Error clearing cart", e);
        }
    }

    // Will be implemented later
    @Override
    public void save(ShoppingCart cart) {
        throw new UnsupportedOperationException("Use addItem/updateItem/removeItem instead");
    }
}