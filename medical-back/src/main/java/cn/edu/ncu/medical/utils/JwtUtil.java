package cn.edu.ncu.medical.utils;


import cn.edu.ncu.medical.exception.LoginException;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {
    private static final long tokenExpiration = 60 * 60 * 1000*24L;
    private static final SecretKey tokenSignKey = Keys.hmacShaKeyFor("M0PKKI6pYGVWWfDZw90a0lTpGYX1d4AQ".getBytes());

    public static String createToken(Long userId, String username) {
        String token = Jwts.builder().
                setSubject("USER_INFO").
                setExpiration(new Date(System.currentTimeMillis() + tokenExpiration)).
                claim("userId", userId).
                claim("username", username).
                signWith(tokenSignKey).
                compact();
        return token;
    }

    public static Claims parseToken(String token){
        if(token == null || "".equals(token)){
           throw new LoginException(ResultCodeEnum.FRONT_LOGIN_AUTH);
        }

        try {
            JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(tokenSignKey).build();
            Jws<Claims> claimsJws = jwtParser.parseClaimsJws(token);
            Claims body = claimsJws.getBody();
            return body;
        } catch (ExpiredJwtException e) {
            throw new LoginException(ResultCodeEnum.TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new LoginException(ResultCodeEnum.TOKEN_INVALID);
        }

    }
    // 检查是否需要刷新Token（剩余时间小于15分钟时刷新）
    public static boolean shouldRefreshToken(Date expiration) {
        long expireTime = expiration.getTime();
        long currentTime = System.currentTimeMillis();
        return (expireTime - currentTime) < (15 * 60 * 1000); // 15分钟
    }

    public static String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return String.valueOf(claims.get("userId", Integer.class));
    }
}
