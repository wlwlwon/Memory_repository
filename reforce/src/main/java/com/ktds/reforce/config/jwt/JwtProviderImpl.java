package com.ktds.reforce.config.jwt;

import com.ktds.reforce.config.UserPrincipal;
import com.ktds.reforce.config.exception.CustomException;
import com.ktds.reforce.config.utils.SecurityUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtProviderImpl implements JwtProvider{

    @Value("${app.jwt.secret-Access}")
    private String JWT_Access_SECRET;

    @Value("${app.jwt.secret-Refresh}")
    private String JWT_Refresh_SECRET;

    @Value("${app.jwt.Access-expiration-in-ms}")
    private Long JWT_Access_EXPIRATION_IN_MS;

    @Value("${app.jwt.Refresh-expiration-in-ms}")
    private Long JWT_Refresh_EXPIRATION_IN_MS;


    public String generateAccessOrRefreshToken(UserPrincipal auth, JwtType jwtType) {

        String authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        switch (jwtType) {

            case Access:
                Key accessKey = Keys.hmacShaKeyFor(JWT_Access_SECRET.getBytes(StandardCharsets.UTF_8));
                return Jwts.builder()
                        .setSubject(auth.getId())
                        .claim("userId", auth.getId())
                        .claim("userName", auth.getUsername())
                        .claim("roles", authorities)
                        .setExpiration(new Date(System.currentTimeMillis() + JWT_Access_EXPIRATION_IN_MS))
                        .signWith(accessKey, SignatureAlgorithm.HS512)
                        .compact();
            case Refresh:
                Key refreshKey = Keys.hmacShaKeyFor(JWT_Refresh_SECRET.getBytes(StandardCharsets.UTF_8));
                return Jwts.builder()
                        .setSubject(auth.getId())
                        .claim("userName", auth.getUsername())
                        .claim("roles", authorities)
                        .claim("userId", auth.getId())
                        .setExpiration(new Date(System.currentTimeMillis() + JWT_Refresh_EXPIRATION_IN_MS))
                        .signWith(refreshKey, SignatureAlgorithm.HS512)
                        .compact();
        }

        throw new CustomException("token이 생성되지 않았습니다.", HttpStatus.BAD_REQUEST);
    }

    public Authentication getAuthentication(HttpServletRequest request) {
        Claims claims = extractAccessClaims(request);
        if (claims == null) {
            return null;
        }

        String id = claims.getSubject();

        if (id == null) {
            return null;
        }

        Set<GrantedAuthority> authorities = getRoles(claims);
        UserDetails userDetails = getUserDetails(id, authorities);

        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    public Authentication getAccessAuthentication(String token) {
        Claims claims = extractAccessClaims(token);

        String username = claims.getSubject();

        if (username == null) {
            return null;
        }

        Set<GrantedAuthority> authorities = getRoles(claims);
        UserDetails userDetails = getUserDetails(username, authorities);


        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    public Authentication getRefreshAuthentication(String token) {
        Claims claims = extractRefreshClaims(token);

        String username = claims.getSubject();

        if (username == null) {
            return null;
        }

        Set<GrantedAuthority> authorities = getRoles(claims);
        UserDetails userDetails = getUserDetails(username, authorities);

        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }


    public boolean isAccessTokenValid(HttpServletRequest request) {
        Claims claims = extractAccessClaims(request);
        if (claims == null) {
            return false;
        }

        if (claims.getExpiration().before(new Date())) {
            return false;
        }

        return true;
    }

    private Claims extractAccessClaims(HttpServletRequest request) {
        String token = SecurityUtils.extractAuthTokenFromRequest(request);

        if (token == null) {
            return null;
        }

        Key key = Keys.hmacShaKeyFor(JWT_Access_SECRET.getBytes(StandardCharsets.UTF_8));

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (AuthenticationException e) {
            e.printStackTrace();
            throw new CustomException("Invalid access token supplied", HttpStatus.BAD_REQUEST);
        }
    }

    public boolean isAccessTokenValid(String token) {
        Claims claims = extractAccessClaims(token);

        if (claims.getExpiration().before(new Date())) {
            return false;
        }

        return true;
    }

    private Claims extractAccessClaims(String token) {
        Key key = Keys.hmacShaKeyFor(JWT_Access_SECRET.getBytes(StandardCharsets.UTF_8));

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (AuthenticationException e) {
            e.printStackTrace();
            throw new CustomException("Invalid access token supplied", HttpStatus.BAD_REQUEST);
        }
    }

    public boolean isRefreshTokenValid(String token) {
        Claims claims = extractRefreshClaims(token);

        if (claims.getExpiration().before(new Date())) {
            return false;
        }

        return true;
    }

    private Claims extractRefreshClaims(String token) {
        Key key = Keys.hmacShaKeyFor(JWT_Refresh_SECRET.getBytes(StandardCharsets.UTF_8));

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (AuthenticationException e) {
            e.printStackTrace();
            throw new CustomException("Invalid refresh token supplied", HttpStatus.BAD_REQUEST);
        }
    }

    private UserPrincipal getUserDetails(String username, Set<GrantedAuthority> authorities) {
        return UserPrincipal.builder()
                .username(username)
                .authorities(authorities)
                .build();
    }

    private Set<GrantedAuthority> getRoles(Claims claims) {
        return Arrays.stream(claims.get("roles").toString().split(","))
                .map(SecurityUtils::convertToAuthority)
                .collect(Collectors.toSet());
    }


    public Long getExpiration(String token) {
        Key key = Keys.hmacShaKeyFor(JWT_Access_SECRET.getBytes(StandardCharsets.UTF_8));
        Date expiration = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getExpiration();
        long now = new Date().getTime();
        return expiration.getTime() - now;
    }
}
