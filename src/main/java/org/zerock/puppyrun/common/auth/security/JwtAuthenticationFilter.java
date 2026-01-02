package org.zerock.puppyrun.common.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zerock.puppyrun.common.auth.jwt.JwtTokenProvider;
import org.zerock.puppyrun.common.exception.ErrorCode;
import org.zerock.puppyrun.common.exception.ErrorResponse;
import org.zerock.puppyrun.member.entity.UserRole;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken = extractTokenFromRequest(request);

        if (accessToken == null || accessToken.isEmpty()) {
            setErrorResponse(request, response, ErrorCode.MISSING_AUTHORIZATION_HEADER);
            return;
        }

        // 토큰 검증
        if (!jwtTokenProvider.validateToken(accessToken)) {
            setErrorResponse(request, response, ErrorCode.INVALID_TOKEN);
            return;
        }

        try {
            UUID userId = jwtTokenProvider.getUserId(accessToken);
            UserRole userRole = jwtTokenProvider.getUserRole(accessToken);

            if (userId != null && userRole != null) {
                String role = "ROLE_" + userRole.name();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("JWT 인증 성공 - User ID: {}, Role: {}", userId, role);
            }
        } catch (Exception e) {
            log.warn("JWT 토큰 파싱 실패: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            setErrorResponse(request, response, ErrorCode.INTERNAL_SERVER_ERROR); // 파싱 실패 시에도 에러 응답
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출 Authorization: Bearer {token} 형태에서 토큰 부분만 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void setErrorResponse(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode)
            throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        // ErrorResponse 객체 생성
        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode.getCode(),
                errorCode.getDescription(),
                "",
                request.getRequestURI()
        );

        //ErrorResponse 객체를 JSON으로 변환하여 응답
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}
