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
            Keys.hmacShaKeyFor(
                    JwtConstant.SECRET_KEY.getBytes()
            );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

//        No JWT supplied.
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            System.out.println(
                    "[JWT] No valid Authorization header for: "
                            + request.getRequestURI()
            );

            filterChain.doFilter(request, response);
            return;
        }

        try {
//            Remove "Bearer " from the Authorization header.
            String token =
                    authorizationHeader.substring(7);

//            Validate and decode JWT.
            Claims claims =
                    Jwts.parser()
                            .verifyWith(secretKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

//            Read email from JWT.
            String email = claims.get("email", String.class);
//            Read authorities from JWT.
            String authorities = claims.get("authorities", String.class);
 //           Read user ID from JWT.
            Long userId = claims.get("userId", Long.class);
//            Convert JWT authorities into Spring Security authorities.
            List<SimpleGrantedAuthority> grantedAuthorities =
                    authorities == null ||
                            authorities.isBlank()
                            ? List.of()
                            : Arrays.stream(
                                    authorities.split(",")
                            )
                            .map(String::trim)
                            .filter(
                                    s -> !s.isEmpty()
                            )
                            .map(
                                    SimpleGrantedAuthority::new
                            )
                            .collect(
                                    Collectors.toList()
                            );

//            Create authenticated user.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            grantedAuthorities
                    );

//            Store authentication in Spring Security context.
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(
                            authentication
                    );

//            Development logging.
            System.out.println("[JWT] Authentication successful");
            System.out.println("[JWT] Email: " + email);
            System.out.println("[JWT] User ID: " + userId);
            System.out.println("[JWT] Authorities: " + grantedAuthorities);

        } catch (Exception e) {
            SecurityContextHolder.clearContext();

            System.err.println("[JWT] Authentication failed");
            System.err.println("[JWT] Request: " + request.getRequestURI());
            System.err.println("[JWT] Reason: " + e.getMessage());
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}