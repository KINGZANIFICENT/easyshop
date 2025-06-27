package org.yearup.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.ProfileDao;
import org.yearup.data.UserDao;
import org.yearup.models.Profile;
import org.yearup.models.User;

import java.security.Principal;

@RestController
@RequestMapping("/profile")
@CrossOrigin
@PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
public class ProfileController {

    private ProfileDao profileDao;
    private UserDao userDao;

    public ProfileController(ProfileDao profileDao, UserDao userDao) {
        this.profileDao = profileDao;
        this.userDao = userDao;
    }

    /**
     * Retrieves the profile for the authenticated user.
     *
     * This endpoint is accessible via an HTTP GET request. It extracts the username
     * from the injected {@link Principal} object, then uses the {@code userDao} to
     * find the corresponding {@link User}. If the user is found, it proceeds to
     * retrieve their profile using the {@code profileDao}.
     *
     * @param principal The authenticated user's principal object, providing access to the username.
     * @return The {@link Profile} object associated with the authenticated user.
     * @throws ResponseStatusException If the user or their profile is not found (HTTP 404),
     * or if an unexpected error occurs during processing (HTTP 500).
     */
    @GetMapping
    public Profile getProfile(Principal principal) {
        try {
            String userName = principal.getName();
            User user = userDao.getByUserName(userName);

            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }

            int userId = user.getId();

            Profile profile = profileDao.getByUserId(userId);

            if (profile == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
            }

            return profile;
        }catch (ResponseStatusException ex) {
            throw ex;
        }catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
        }
    }

    /**
     * Updates the profile for the authenticated user.
     *
     * This endpoint is accessible via an HTTP PUT request and expects a {@link Profile}
     * object in the request body. It retrieves the authenticated username from the
     * {@link Principal}, finds the corresponding {@link User}, and then uses the
     * {@code profileDao} to update the user's profile information in the database.
     * The user ID for the update operation is derived from the authenticated user,
     * ensuring that a user can only update their own profile.
     *
     * @param profile The {@link Profile} object containing the updated profile data.
     * @param principal The authenticated user's principal object, providing access to the username.
     * @return The updated {@link Profile} object after a successful update.
     * @throws ResponseStatusException If the user or their profile is not found (HTTP 404),
     * or if an unexpected error occurs during processing (HTTP 500).
     */
    @PutMapping
    public Profile updateProfile(@RequestBody Profile profile, Principal principal) {
        try {
            String userName = principal.getName();
            User user = userDao.getByUserName(userName);

            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }

            int userId = user.getId();

            profile.setUserId(userId);

            Profile updatedProfile = profileDao.update(userId, profile);

            if (updatedProfile == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
            }

            return updatedProfile;
        }catch (ResponseStatusException ex) {
            throw ex;
        }catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad");
        }
    }
}
