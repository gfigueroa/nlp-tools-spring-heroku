package com.figueroa.security;

import org.springframework.security.core.userdetails.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * This is where the magic of JJWT happens. 
 * In a single chain of calls to its fluent API, JJWT will create a token 
 * using the subject of our choosing (username) and sign it using the 
 * application’s secret key. All of the other work of generating appropriately 
 * structured JSON according to the even more specific JWT specification, 
 * Base 64 encoding, and compaction are all taken care of behind the scenes. 
 * The reverse is true too. JJWT can also use a secret key to fully decode and 
 * parse a JSON Web Token and provide an object that we can use to later 
 * retrieve the subject (username) needed to verify the presence of a real user.
 *
 */
public final class TokenHandler {

    private final String secret;
    private final UserService userService;
    public static final SignatureAlgorithm SIGNATURE_ALGORITHM = 
    		SignatureAlgorithm.HS256;

    public TokenHandler(String secret, UserService userService) {
        this.secret = secret;
        this.userService = userService;
    }

    public User parseUserFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        
        String username = claims.getSubject();
        return userService.loadUserByUsername(username);
    }

    public String createTokenForUser(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .signWith(SIGNATURE_ALGORITHM, secret)
                .compact();
    }
}