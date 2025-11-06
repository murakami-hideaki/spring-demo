package com.example.demo.framework.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.framework.log.MdcContextUtil;

/**
 * <h2>LoggingWebClientConfig</h2>
 * <p>
 * WebClientのリクエストにフィルターをチェインし、各通信ごとに独自リクエストID（UUID＋日付）でログを詳細に出力する設定クラスです。
 *
 * <h3>ログ出力のタイミングと内容</h3>
 * <ul>
 *   <li><b>リクエスト送信直前（START）:</b><br>
 *     <code>【WEBCLIENT START】requestId=[独自ID] method=[HTTPメソッド] url=[リクエストURL]</code>
 *     <ul>
 *       <li>requestId : UUID＋日付の一意リクエストID（例: b2e08c34-1c37-4498-b550-548965a98c19_20251106083023）</li>
 *       <li>method : GET/POSTなど</li>
 *       <li>url : 外部APIのエンドポイントURL</li>
 *     </ul>
 *   </li>
 *   <li><b>正常レスポンス受信時（END）:</b><br>
 *     <code>【WEBCLIENT END】requestId=[独自ID] status=[HTTPステータス]</code>
 *     <ul>
 *       <li>requestId : リクエストと同じ独自ID</li>
 *       <li>status : HTTPステータス（例: 200 OK, 404 Not Found, ...）</li>
 *     </ul>
 *   </li>
 *   <li><b>例外発生時（ERROR）:</b><br>
 *     <code>【WEBCLIENT ERROR】requestId=[独自ID]. [例外型]: [例外メッセージ]</code>
 *     <ul>
 *       <li>requestId : リクエストと同じ独自ID</li>
 *       <li>例外型 : 例外のクラス名（WebClientResponseException等）</li>
 *       <li>例外メッセージ : エラー内容</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>設計方針・注意点</h3>
 * <ul>
 *   <li>入力パラメータ、HTTPリクエストヘッダーなど機微情報、秘匿情報はログに出力しません</li>
 * </ul>
 */
@Configuration
public class LoggingWebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(LoggingWebClientConfig.class);

    /** ClientRequest#attributesに格納する独自リクエストIDのキー */
    private static final String ATTR_KEY_REQUEST_ID = "KEY-WEBCLIENT-CUSTOMREQUESTID";

    /**
     * WebClient Bean定義（requestId付きロギング用フィルタチェイン）
     *
     * @return ロギング機能付きWebClientインスタンス
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            // リクエスト直前フィルタ: requestId生成→attributes格納→開始ログ出力
            .filter((request, next) -> {
                // 独自リクエストID（UUID+日付）を新規生成
                String requestId = MdcContextUtil.generateCustomRequestId();

                // attributes（リクエスト属性）にrequestIdを格納した新しいClientRequestを生成
                ClientRequest newRequest = ClientRequest.from(request)
                        .attribute(ATTR_KEY_REQUEST_ID, requestId)
                        .build();

                // リクエスト開始ログ（method, URL, requestIdのみ出力。ヘッダー等は出力しない）
                log.info("【WEBCLIENT START】requestId={} method={} url={}", requestId, request.method(), request.url());

                // 次のフィルタ/実処理へ
                return next.exchange(newRequest);
            })
            // レスポンス／エラー時フィルタ: attributesからrequestId取得→終了/エラーログ出力
            .filter((request, next) ->
                next.exchange(request)
                    // 正常レスポンス時
                    .doOnNext(response -> {
                        // attributesからrequestIdを取得
                        String requestId = request.attribute(ATTR_KEY_REQUEST_ID)
                                .map(String.class::cast)
                                .orElse("N/A");
                        // レスポンス終了ログ（requestIdとステータスのみ）
                        log.info("【WEBCLIENT END】requestId={} status={}", requestId, response.statusCode());
                    })
                    // 例外発生時
                    .doOnError(ex -> {
                        // attributesからrequestIdを安全に取得
                        String requestId = request.attribute(ATTR_KEY_REQUEST_ID)
                                .map(String.class::cast)
                                .orElse("N/A");
                        // エラーログ（requestId、例外型、例外メッセージ。ヘッダー等は出力しない）
                        log.error("【WEBCLIENT ERROR】requestId={}. {}: {}", requestId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
                    })
            )
            .build();
    }
}