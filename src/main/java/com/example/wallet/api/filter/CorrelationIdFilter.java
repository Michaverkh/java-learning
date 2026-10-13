package com.example.wallet.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String ATTRIBUTE_NAME = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));

        // Устанавливаем до передачи запроса дальше: даже ранняя ошибка получит идентификатор.
        request.setAttribute(ATTRIBUTE_NAME, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        filterChain.doFilter(request, response);
    }

    private String resolveCorrelationId(String header) {
        if (header != null && header.length() == 36) {
            try {
                UUID id = UUID.fromString(header);
                // fromString допускает некоторые сокращённые записи; принимаем полный UUID.
                if (id.toString().equalsIgnoreCase(header)) {
                    return header;
                }
            } catch (IllegalArgumentException exception) {
                // Неверный служебный заголовок не мешает выполнению запроса.
            }
        }
        return UUID.randomUUID().toString();
    }
}
