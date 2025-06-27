package org.yearup.data;

import org.yearup.models.ShoppingCart;

public interface ShoppingCartDao
{
    ShoppingCart getByUserId(int userId);
    void save(ShoppingCart cart);
    void delete(int userId);
    void addItem(int userId, int productId, int quantity);
    void updateItem(int userId, int productId, int quantity);
    void removeItem(int userId, int productId);
    // add additional method signatures here
}
