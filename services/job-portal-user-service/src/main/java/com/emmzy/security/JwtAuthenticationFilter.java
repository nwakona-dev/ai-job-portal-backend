package com.emmzy.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey secretKey =
            Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        // No Authorization header
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        try {

            // Remove "Bearer " from the token
            String token =
                    authorizationHeader.substring(7);

            // Validate and decode JWT
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Read information from JWT
            String email =
                    claims.get("email", String.class);

            String authorities =
                    claims.get("authorities", String.class);

            /*
             * Convert authorities stored in the JWT into
             * Spring Security authorities.
             *
             * Example:
             *
             * ROLE_JOB_SEEKER
             * ROLE_EMPLOYER
             * ROLE_ADMIN
             */

            List<SimpleGrantedAuthority> grantedAuthorities =
                    authorities == null ||
                            authorities.isBlank()
                            ? List.of()
                            : Arrays.stream(authorities.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

            // Create authenticated user
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            grantedAuthorities
                    );

            // Store authentication in Spring Security
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

        } catch (Exception e) {

            /*
             * Invalid or expired JWT.
             *
             * Do not authenticate the request.
             */
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}