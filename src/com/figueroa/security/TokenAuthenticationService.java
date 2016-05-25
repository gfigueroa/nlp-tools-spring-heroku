package com.figueroa.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

/**
 * When a user successfully logs into the web application, 
 * the first public method of this class will be called to create a token for that user. 
 * That token is then added as a header with the key “Authorization” to the outbound response. 
 * The authentication filter is calling getAuthentication(...) though, 
 * which looks for a header that uses the same key “Authorization” and uses that token to 
 * re-establish the authenticating user. If this method throws an exception or returns null 
 * then the user will not be authenticated and the request will ultimately fail.
 *
 */
public class TokenAuthenticationService {

    private static final String AUTH_HEADER_NAME = "Authorization";

    private final TokenHandler tokenHandler;

    public TokenAuthenticationService(String secret, UserService userService) {
        tokenHandler = new TokenHandler(secret, userService);
    }

    public void addAuthentication(HttpServletResponse response, UserAuthentication authentication) {
        final User user = authentication.getDetails();
        response.addHeader(AUTH_HEADER_NAME, tokenHandler.createTokenForUser(user));
    }

    public Authentication getAuthentication(HttpServletRequest request) {
        final String tokenDirty = request.getHeader(AUTH_HEADER_NAME);
        // Clean first 7 chars: "Bearer <token>"
        final String token = tokenDirty == null ? null : tokenDirty.substring(7); 
        if (token != null) {
            final User user = tokenHandler.parseUserFromToken(token);
            if (user != null) {
                return new UserAuthentication(user);
            }
        }
        return null;
    }
}