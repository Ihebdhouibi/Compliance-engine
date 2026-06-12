package com.devteam.aiauditserver.Tools.util;


import com.devteam.aiauditserver.responses.Response.TokenResponse;
import com.devteam.aiauditserver.services.Oathloginservice.CustomUserDetailsService;
import io.jsonwebtoken.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class TokenUtil {
    private final String CLAIMS_SUBJECT = "sub";
    private final String CLAIMS_CREATED = "created";
    @Value("${auth.jwtExpirationMs}")
    private Long TOKEN_VALIDITY = 86400000L;


    @Value("${auth.secret}")
    private String TOKEN_SECRET;
    @Autowired
    private CustomUserDetailsService customUserDetailsService; // Inject UserDetailsService

    private static final Logger logger = LoggerFactory.getLogger(TokenUtil.class);
    public UserDetails getUserDetailsFromToken(String token) {
        String username = getUserNameFromToken(token);
        logger.error("user name find it  : "+username);
        if (username == null) {
            return null;
        }
        return customUserDetailsService.loadUserByUsername(username);
    }
    public TokenResponse generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIMS_SUBJECT, userDetails.getUsername());
        claims.put(CLAIMS_CREATED, new Date());
        Date expiry = generateExpirationDate();
        String token =  Jwts.builder()
                .setClaims(claims)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS512, TOKEN_SECRET)
                .compact();

        TokenResponse response = new TokenResponse(token,expiry);
        return  response;

    }

    public String getUserNameFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            if (claims != null) {
                String subject = claims.getSubject();
                return subject;
            } else {
                String[] parts = token.split("\\.");
                if (parts.length > 1) {
                    String payload = new String(Base64.getDecoder().decode(parts[1]));
                    String email = new JSONObject(payload).getString("email");
                    return email;
                } else {
                    logger.error("Token does not contain enough parts to decode payload: " + token);
                }
            }
        } catch (Exception ex) {
            logger.error("Failed to extract username from token: " + token, ex);
        }
        return null;
    }



    private Date generateExpirationDate() {
        return new Date(System.currentTimeMillis() + TOKEN_VALIDITY*1000);
    }

    public boolean isTokenExpired(String token) {
        logger.debug("Entering isTokenExpired with token: " + token);
        try {
            Claims claims = getClaims(token);
            if (claims != null) {
                Date expiration = claims.getExpiration();
                Date now = new Date();
                logger.debug("Claims extracted successfully: " + claims);
                logger.debug("Token expiration date: " + expiration + ", Current date: " + now);
                boolean expired = expiration.before(now);
                logger.debug("Token expired: " + expired);
                return expired;
            } else {
                logger.warn("Claims are null, falling back to manual expiration parsing for token: " + token);
                boolean manualExpired = parseManualExpiration(token);
                logger.debug("Manual expiration parsing result: " + manualExpired);
                return manualExpired;
            }
        } catch (Exception ex) {
            logger.error("Error checking token expiration for token: " + token, ex);
            return true;
        }
    }



    private boolean parseManualExpiration(String token) throws JSONException {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            logger.error("Invalid token format: less than 2 parts");
            return true;
        }
        String payload = new String(Base64.getDecoder().decode(token.split("\\.")[1]));
        long exp = new JSONObject(payload).getLong("exp") * 1000L;
        return new Date(exp).before(new Date());
    }


    public boolean isTokenValid(String token, UserDetails userDetails) {
        return !isTokenExpired(token);
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parser().setSigningKey(TOKEN_SECRET).parseClaimsJws(token).getBody();
        } catch (Exception ex) {
            return null;
        }
    }


    public Claims getClaimsdd(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().setSigningKey(TOKEN_SECRET).parseClaimsJws(token).getBody();

        }catch (Exception ex) {
            claims = Jwts.parser().parseClaimsJws(token).getBody();
        }

        return claims;
    }

    public Date getTokenExpiryFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(TOKEN_SECRET)
                .parseClaimsJws(token)
                .getBody();

        return claims.getExpiration();
    }

}
