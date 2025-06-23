package onemg.analytics.dump.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import onemg.analytics.dump.controller.mockcontroller.MockAdminController;

import java.security.Key;
import java.util.Date;

import org.apache.log4j.Logger;

public class JWTUtils {
        private static final Logger LOGGER = Logger.getLogger(JWTUtils.class);

    private static final long EXPIRATION_TIME_MS = 30 * 1000L; // 30 seconds

    private static Key getSigningKey(String base64Secret) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static String generateToken(String base64Secret) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME_MS);
        return Jwts.builder()
                .setSubject("mock-admin-auth")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(base64Secret), SignatureAlgorithm.HS256)
                .compact();
    }

    public static boolean validateToken(String token, String base64Secret) {
        try {
            Date now = new Date();
            Claims claim = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey(base64Secret))
                    .build()
                    .parseClaimsJws(token).getBody();
            if(claim.getExpiration().before(now)){
                return false;
            }
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
    
}
