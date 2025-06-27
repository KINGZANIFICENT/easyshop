package org.yearup.data.mysql;

import org.springframework.stereotype.Component;
import org.yearup.models.Profile;
import org.yearup.data.ProfileDao;

import javax.sql.DataSource;
import java.sql.*;

@Component
public class MySqlProfileDao extends MySqlDaoBase implements ProfileDao
{
    public MySqlProfileDao(DataSource dataSource)
    {
        super(dataSource);
    }

    @Override
    public Profile create(Profile profile)
    {
        String sql = "INSERT INTO profiles (user_id, first_name, last_name, phone, email, address, city, state, zip) " +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try(Connection connection = getConnection();
            PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS))
        {
            ps.setInt(1, profile.getUserId());
            ps.setString(2, profile.getFirstName());
            ps.setString(3, profile.getLastName());
            ps.setString(4, profile.getPhone());
            ps.setString(5, profile.getEmail());
            ps.setString(6, profile.getAddress());
            ps.setString(7, profile.getCity());
            ps.setString(8, profile.getState());
            ps.setString(9, profile.getZip());

            ps.executeUpdate();

            return profile;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves a user profile by their user ID.
     *
     * This method executes a SQL SELECT statement to get profile information
     * from the 'profiles' table based on the provided user ID. It then maps the
     * result set to a {@link Profile} object.
     *
     * @param userId The unique identifier of the user whose profile is to be retrieved.
     * @return A {@link Profile} object containing the user's profile information if found,
     * or {@code null} if no profile is associated with the given user ID.
     * @throws RuntimeException If a SQL exception occurs during the database operation.
     */
    @Override
    public Profile getByUserId(int userId) {
        String query = """
                SELECT
                user_id, first_name, last_name, phone, email, address, city, state, zip
                FROM 
                profiles
                WHERE
                user_id = ?
                """;

        try (Connection connection = getConnection();
        PreparedStatement ps = connection.prepareStatement(query)){
            ps.setInt(1, userId);

            try (ResultSet results = ps.executeQuery()){
                if (results.next()) {
                    return mapRow(results);

                }
            }
        }catch (SQLException e) {
            throw new RuntimeException("Error getting profile for user " + userId, e);
        }
        return null;
    }

    /**
     * Updates an existing user profile in the database.
     *
     * This method constructs and executes a SQL UPDATE statement to modify the profile
     * details for a specific user ID. If the update is successful (one or more rows
     * are affected), it then retrieves and returns the updated profile using
     * the {@code getByUserId} method.
     *
     * @param userId The unique identifier of the user whose profile is to be updated.
     * @param profile A {@link Profile} object containing the new profile data. The
     * user ID within this object is not used for the update condition;
     * the {@code userId} parameter is used instead.
     * @return The updated {@link Profile} object if the update was successful,
     * or {@code null} if no rows were affected (e.g., the user ID was not found).
     * @throws RuntimeException If a SQL exception occurs during the database operation.
     */
    @Override
    public Profile update(int userId, Profile profile) {
        String query = """
                UPDATE profiles
                SET
                first_name = ?,
                last_name = ?,
                phone = ?,
                email = ?,
                address = ?,
                city = ?,
                state = ?,
                zip = ?
                WHERE user_id = ?
                """;

        try (Connection connection = getConnection();
        PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, profile.getFirstName());
            ps.setString(2, profile.getLastName());
            ps.setString(3, profile.getPhone());
            ps.setString(4, profile.getEmail());
            ps.setString(5, profile.getAddress());
            ps.setString(6, profile.getCity());
            ps.setString(7, profile.getState());
            ps.setString(8, profile.getZip());
            ps.setInt(9, userId);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                return getByUserId(userId);
            }
        }catch (SQLException e) {
            throw new RuntimeException("Error updating profile for user " + userId, e);
        }

        return null;
    }

    /**
     * Maps a {@link ResultSet} row to a {@link Profile} object.
     *
     * This helper method takes a {@link ResultSet}
     * and extracts the column values to populate a new {@link Profile} object.
     *
     * @param row The {@link ResultSet} containing the profile data for a single row.
     * @return A new {@link Profile} object populated with data from the provided {@link ResultSet}.
     * @throws SQLException If a database access error occurs or this method is called on a closed result set.
     */
    private Profile mapRow(ResultSet row) throws SQLException {
        Profile profile = new Profile();
        profile.setUserId(row.getInt("user_id"));
        profile.setFirstName(row.getString("first_name"));
        profile.setLastName(row.getString("last_name"));
        profile.setPhone(row.getString("phone"));
        profile.setEmail(row.getString("email"));
        profile.setAddress(row.getString("address"));
        profile.setCity(row.getString("city"));
        profile.setState(row.getString("state"));
        profile.setZip(row.getString("zip"));
        return profile;
    }
}
