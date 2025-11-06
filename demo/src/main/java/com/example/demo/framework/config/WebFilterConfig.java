package com.example.demo.framework.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.framework.filter.LoggingInitializeFilter;

/**
 * 共通Webフィルタの登録・順序制御を集中管理する設定クラス。
 * 複数フィルタの順序を一元的に管理します。
 */
@Configuration
public class WebFilterConfig {

    /**
     * LoggingInitializeFilterの登録（順序1番目）
     */
    @Bean
    public FilterRegistrationBean<LoggingInitializeFilter> loggingFilter() {
        FilterRegistrationBean<LoggingInitializeFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LoggingInitializeFilter());
        registration.setOrder(1); // 実行順序：小さいほど先
        registration.addUrlPatterns("/*"); // 全リクエスト対象
        return registration;
    }

    // 他のフィルタも必要に応じてここに@Bean登録・順序指定
}