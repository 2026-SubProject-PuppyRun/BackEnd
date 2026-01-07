package org.zerock.puppyrun.common.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zerock.puppyrun.common.auth.jwt.exception.InvalidTokenException;
import org.zerock.puppyrun.common.auth.jwt.exception.TokenExpirationException;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.member.DTO.MemberDTO;
import org.zerock.puppyrun.member.entity.UserRole;

@Slf4j
@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // .env에 넣은 비밀키가 일반 문자열이라면 UTF-8 바이트로 변환
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 토큰 생성
     */
    private String createToken(UUID MemberId, String email, UserRole role, long expiration) {
        return Jwts.builder()
                .subject(String.valueOf(MemberId))
                .claim("ROLE_", role)
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration)) // 만료 시각
                .signWith(key)
                .compact();
    }

    /**
     * JWT 토큰에서 Claims 추출
     */
    private Claims getClaims(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenExpirationException("만료된 토큰입니다.", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("유효하지 않은 토큰입니다.", e);
        }
        return claims;
    }

    /**
     * 토큰에서 유저 Principal 추출
     *
     * @param token
     * @return UserPrincipal
     */
    public UserPrincipal getUserPrincipal(String token) {
        Claims claims = getClaims(token);

        String userId = claims.getSubject();
        log.info("userId: {}", userId);
        String email = claims.get("email", String.class);
        log.info("email: {}", email);
        String userRole = claims.get("ROLE_", String.class);
        log.info("userRole: {}", userRole);

        return new UserPrincipal(UUID.fromString(userId), email, UserRole.valueOf(userRole));
    }


    public String generateAccessToken(MemberDTO memberDTO) {
        return createToken(memberDTO.id(), memberDTO.email(), memberDTO.userRole(), accessTokenExpiration);
    }

    public String generateRefreshToken(MemberDTO memberDTO) {
        return createToken(memberDTO.id(), memberDTO.email(), memberDTO.userRole(), refreshTokenExpiration);
    }

}
