package com.figueroa.security;

import java.util.HashMap;

import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * TODO: This implementation of Spring Security’s UserDetailsService is for 
 * demonstration purposes only! It uses a map to maintain the set of 
 * available users and it isn’t even thread safe. If you are using 
 * this tutorial you will most certainly want to replace this with 
 * a user lookup service that uses a database, LDAP, or some other
 *  more legitimate means of keeping track of users.
 *
 */
public class UserService implements org.springframework.security.core.userdetails.UserDetailsService {

    private final AccountStatusUserDetailsChecker detailsChecker = new AccountStatusUserDetailsChecker();
    private final HashMap<String, User> userMap = new HashMap<String, User>();

    @Override
    public final User loadUserByUsername(String username) throws UsernameNotFoundException {
        final User user = userMap.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found!");
        }
        detailsChecker.check(user);
        return user;
    }

    public void addUser(User user) {
        userMap.put(user.getUsername(), user);
    }
}