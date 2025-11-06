package com.example.demo.framework.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * リクエスト毎にMDCを必ず初期化・クリアするフィルタ。
 */
@Component
public class MdcClearFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            MDC.clear();
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}