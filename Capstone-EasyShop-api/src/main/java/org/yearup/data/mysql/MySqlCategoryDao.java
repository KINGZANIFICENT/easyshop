package org.yearup.data.mysql;

import org.apache.ibatis.jdbc.SQL;
import org.springframework.stereotype.Component;
import org.yearup.data.CategoryDao;
import org.yearup.models.Category;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class MySqlCategoryDao extends MySqlDaoBase implements CategoryDao
{
    public MySqlCategoryDao(DataSource dataSource)
    {
        super(dataSource);
    }

    @Override
    public List<Category> getAllCategories()
    {
        List<Category> categories = new ArrayList<>();

        String query = """
                SELECT
                category_id, name, description
                FROM
                categories
                """;

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query);
             ResultSet results = ps.executeQuery()){

            while (results.next()) {
                Category category = mapRow(results);
                categories.add(category);
            }
        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return categories;
    }

    @Override
    public Category getById(int categoryId)
    {
        String query = """
                SELECT
                category_id, name, description
                FROM
                categories
                WHERE
                category_id = ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query);
        ) {
            ps.setInt(1, categoryId);
            try (ResultSet results = ps.executeQuery()) {
                {
                    if (results.next()) {
                        return mapRow(results);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error in getById" + e.getMessage());
            throw new RuntimeException("Database error while getting category" + categoryId, e);
        }

        return null;
    }

    @Override
    public Category create(Category category)
    {

        String query = """
                INSERT INTO
                categories(name, description)
                VALUES
                (?, ?) 
                """;

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);){

            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                try(ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int categoryId = keys.getInt(1);

                        return getById(categoryId);
                    }
                }
            }
        }catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    @Override
    public void update(int categoryId, Category category)
    {
        String query = """
                UPDATE
                categories
                SET
                name = ?, description = ?
                WHERE
                category_id = ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query);){

            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, categoryId);

            ps.executeUpdate();
        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(int categoryId)
    {
        String query = """
                DELETE FROM
                categories
                WHERE
                category_id = ?
                """;

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query);){


            ps.setInt(1, categoryId);

            ps.executeUpdate();

        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Category mapRow(ResultSet row) throws SQLException
    {
        int categoryId = row.getInt("category_id");
        String name = row.getString("name");
        String description = row.getString("description");

        Category category = new Category()
        {{
            setCategoryId(categoryId);
            setName(name);
            setDescription(description);
        }};

        return category;
    }
}