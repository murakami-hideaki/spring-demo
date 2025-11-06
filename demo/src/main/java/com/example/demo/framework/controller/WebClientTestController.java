package com.example.demo.framework.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

/**
 * <h2>WebClient のログ出力確認用コントローラ</h2>
 * <p>
 * GET /webclientlog にアクセスすると外部API（httpbin.org/get）へWebClientリクエストを送信。
 * ログにはLoggingWebClientConfigで設定されたrequestId付きログが出力される。
 * </p>
 */
@RestController
public class WebClientTestController {

    private final WebClient webClient;

    // DIでLoggingWebClientConfigのWebClient Beanを受け取る
    public WebClientTestController(WebClient webClient) {
        this.webClient = webClient;
    }

    @GetMapping("/webclientlog")
    public Mono<String> helloWorldApi() {
        // 外部APIへリクエスト（GET）し、結果のbody（JSON文字列）をそのまま返す
        return webClient.get()
                .uri("https://jsonplaceholder.typicode.com/todos/1")
                .retrieve()
                .bodyToMono(String.class);
    }
}