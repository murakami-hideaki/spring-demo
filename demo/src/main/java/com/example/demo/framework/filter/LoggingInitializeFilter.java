package com.example.demo.framework.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.framework.log.MdcContextUtil;
import com.example.demo.framework.log.MdcKeyNames;

/**
 * <h2>LoggingInitializeFilter</h2>
 * <p>
 * HTTPリクエスト単位でMDCに共通情報（リクエストID、リクエストURI、HTTPメソッド）を自動設定するフィルタです。<br>
 * ・リクエストIDはUUIDと日付（yyyyMMddss）を結合し、システム独自で毎リクエストごとに生成します。<br>
 * ・外部からのX-Request-Id等のヘッダー値は一切使用しません。<br>
 * ・リクエスト処理後はMDCの内容を完全にクリアします。<br>
 * ・OncePerRequestFilterを継承することで、1リクエストにつき1回のみ実行されることが保証されます。<br>
 * </p>
 */
public class LoggingInitializeFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingInitializeFilter.class);

    /**
     * 1リクエストにつき1回のみ呼び出され、MDCへの共通情報設定・クリアを行う。
     *
     * @param request     HTTPリクエスト
     * @param filterChain フィルタチェーン
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
    	
        try {
	        	String requestId = request.getHeader("X-Request-Id");
	        	if (requestId == null || requestId.isEmpty()) {
	            // システム独自のリクエストID発番（UUID+日付(yyyyMMddss)）
	        	    requestId = MdcContextUtil.generateCustomRequestId();
	        	}
	        	MDC.put(MdcKeyNames.REQUEST_ID, requestId);

            // MDCにリクエストID、URI、HTTPメソッドを格納
            MDC.put(MdcKeyNames.REQUEST_ID, requestId);
            MDC.put(MdcKeyNames.REQUEST_URI, request.getRequestURI());
            MDC.put(MdcKeyNames.HTTP_METHOD, request.getMethod());

            // チェーン継続
            filterChain.doFilter(request, response);

        } finally {
            // 全てのリクエスト終了時にMDCを完全クリア
            MdcContextUtil.clear();
        }
    }
}