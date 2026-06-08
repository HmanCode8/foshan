package com.example.mgis.untils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    // 自定义秘钥（长度至少256位，保证HS256安全）
    private static final String SECRET_KEY = "MGIS-LOGIN-SECRET-20260521-MGIS-LOGIN-SECRET";
    // 过期时间 1天
    private static final long EXPIRE_TIME = 1000 * 60 * 60 * 24;

    // 生成token
    public String generateToken(Long userId, String username) {
        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + EXPIRE_TIME);
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId) // 这里存入用户ID
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 解析token获取用户名
    public String getUsernameByToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    // 解析token获取用户ID（你拦截器里要用到的方法）
    public Long getUserIdByToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("userId", Long.class);
    }

    // 判断token是否过期
    public boolean isExpire(String token) {
        Claims claims = getClaims(token);
        return claims.getExpiration().before(new Date());
    }

    // 私有解析方法
    private Claims getClaims(String token) {
        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new RuntimeException("token无效或已过期");
        }
    }
}