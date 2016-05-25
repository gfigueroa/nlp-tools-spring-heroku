package com.figueroa.security;

import java.util.ArrayList;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Order(2)
/**
 * This class will declare any security-related Spring beans and configure the 
 * base HTTP filter to secure the web application.
 *
 */
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserService userService;
    private final TokenAuthenticationService tokenAuthenticationService;
    // TODO: should be replaced with a value private to your application and ideally loaded from a property file.
    private final static String SECRET = "secret";

    public SpringSecurityConfig() {
        super(true);
        this.userService = new UserService();
        
        // Add testing user
        addTestingUser();
        
        tokenAuthenticationService = 
        		new TokenAuthenticationService(SECRET, userService);
    }
    
    /**
     * Add a testing user for web services.
     */
    private void addTestingUser() {
    	ArrayList<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("test"));
        User user = new User("user", "password", authorities);
        this.userService.addUser(user);
    }

    @Override
    /**
     * Override the default HttpSecurity configuration
     */
    protected void configure(HttpSecurity http) throws Exception {
        
    	http
		.csrf().disable()  // JWT tokens are immune to CSRF
    	.authorizeRequests()
	        
	    	// Allow access to api-docs
	        .antMatchers("/ws/api-docs/**").permitAll()
	        // Request authentication for any web service
	        .antMatchers("/ws/**").authenticated()
	        
	        // All other requests are permitted by anyone
	        .anyRequest().permitAll().and()

	        // Custom Token based authentication based on the header previously given to the client
	        .addFilterBefore(new StatelessAuthenticationFilter(tokenAuthenticationService),
	        		UsernamePasswordAuthenticationFilter.class)
            
	    .exceptionHandling().and()
            .anonymous().and()
            .servletApi().and()
            .headers().cacheControl();
    }

    @Override
    /**
     * Override the default Authentication configuration
     */
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService()).passwordEncoder(new BCryptPasswordEncoder());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    @Override
    public UserService userDetailsService() {
        return userService;
    }

    @Bean
    public TokenAuthenticationService tokenAuthenticationService() {
        return tokenAuthenticationService;
    }
}