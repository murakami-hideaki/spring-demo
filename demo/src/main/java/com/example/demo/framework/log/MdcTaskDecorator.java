package com.example.demo.framework.log;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * <h2>MDC伝搬用TaskDecorator</h2>
 * <p>
 * 非同期実行時（SpringのTaskExecutor等）において、
 * 親スレッド（呼び出し元）のMDC（Mapped Diagnostic Context）コンテキストを
 * 子スレッド（実際にRunnableが実行されるスレッド）へ安全に伝搬します。
 * </p>
 *
 * <h3>主な用途</h3>
 * <ul>
 *   <li>主にSpringの{@code @Async}等で利用されるTaskExecutorにセットし、
 *       非同期タスク実行時にリクエストID等のMDC情報を伝搬する用途</li>
 *   <li>ログ出力時にリクエスト単位の識別子やユーザー情報等のトレーサビリティ確保</li>
 * </ul>
 *
 * <h3>動作仕様</h3>
 * <ol>
 *   <li>{@link #decorate(Runnable)}が呼ばれた時点で、親スレッドのMDCコンテキスト（Map）をコピー</li>
 *   <li>戻り値のRunnableでは、run()実行直前にMDCをセットし、実行前に<b>開始ログ</b>、正常終了時に<b>終了ログ</b>、例外発生時に<b>例外ログ</b>を出力する</li>
 *   <li>実行後（例外時も含む）に必ずMDCを元の状態に戻す</li>
 *   <li>MDCが未設定（null）の場合、子スレッドではMDC.clear()のみ実施</li>
 * </ol>
 *
 * <h3>ログ出力内容</h3>
 * <ul>
 *   <li>【ASYNC START】非同期タスク実行開始（MDCでセットされていればクラス・メソッド名を含める）</li>
 *   <li>【ASYNC END】 　非同期タスク実行正常終了（MDCでセットされていればクラス・メソッド名・経過時間を含める）</li>
 *   <li>【ASYNC EX】  　非同期タスク実行中例外発生（MDCでセットされていればクラス・メソッド名・例外内容を含める）</li>
 * </ul>
 *
 * <h3>注意点・適用されないケース</h3>
 * <ul>
 *   <li>このTaskDecoratorは、<b>Spring管理下のTaskExecutor</b>に対してのみ有効です。
 *     <ul>
 *       <li>たとえば、{@code @Async}アノテーションによる非同期実行や、
 *           {@code ThreadPoolTaskExecutor}等にセットした場合のみMDCが伝搬されます。</li>
 *     </ul>
 *   </li>
 *   <li>以下のようなケースでは<b>MDC伝搬はされません</b>ので注意してください:
 *     <ul>
 *       <li>自前で生成した{@code ExecutorService}や{@code new Thread()}等、Spring管理外のスレッド/Executorを利用した場合</li>
 *       <li>Java標準の{@code Executors.newFixedThreadPool()}等で作成したExecutorを直接利用した場合</li>
 *       <li>{@code @Async}でexecutor名を明示的に変え、TaskDecorator未設定のExecutorを利用する場合</li>
 *       <li>Spring Batch、WebFlux、Reactive Streamsなど、TaskDecorator非対応の非同期実行フレームワーク</li>
 *       <li>ApplicationEventPublisher等の非同期イベント処理（別途対策が必要）</li>
 *     </ul>
 *   </li>
 *   <li>MDC自体はスレッドローカル(ThreadLocal)管理のため、<b>巨大な値や重いオブジェクトを入れないこと</b></li>
 *   <li>クラス名・メソッド名はMDCContextUtil#putAsyncMethodを利用し呼び出し側でセットしてください</li>
 * </ul>
 *
 */
public class MdcTaskDecorator implements TaskDecorator {

    private static final WebLogger log = WebLogger.getLogger(MdcTaskDecorator.class);

    /**
     * 親スレッドのMDCコンテキストを子スレッドに伝搬しつつ、
     * 実行開始時・終了時・例外発生時にログを出力します。
     *
     * クラス名・メソッド名は呼び出し側でMDCキー("asyncMethod"等)へセットしてください。
     * 
     * @param runnable 元の非同期タスク
     * @return MDCコンテキストが伝搬されたRunnable
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        // 親スレッドのMDCコンテキスト（Map<String, String>）を取得（nullの場合もあり）
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            // 実行スレッドの既存MDCを保管
            final Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                if (contextMap != null) {
                    // 子スレッドに親のMDCコンテキストをセット
                    MDC.setContextMap(contextMap);
                } else {
                    MDC.clear();
                }

                // クラス名・メソッド名（MDCにセットされていれば付与）
                String asyncMethod = MDC.get("asyncMethod");
                if (asyncMethod != null && !asyncMethod.isEmpty()) {
                    log.info("【ASYNC START】thread={} method={}", Thread.currentThread().getName(), asyncMethod);
                } else {
                    log.info("【ASYNC START】thread={}", Thread.currentThread().getName());
                }

                final long start = System.currentTimeMillis();
                try {
                    runnable.run();
                    final long elapsed = System.currentTimeMillis() - start;
                    if (asyncMethod != null && !asyncMethod.isEmpty()) {
                        log.info("【ASYNC END】thread={} method={} elapsed={}ms", Thread.currentThread().getName(), asyncMethod, elapsed);
                    } else {
                        log.info("【ASYNC END】thread={} elapsed={}ms", Thread.currentThread().getName(), elapsed);
                    }
                } catch (Throwable t) {
                    if (asyncMethod != null && !asyncMethod.isEmpty()) {
                        log.error("【ASYNC EX】thread={} method={} ex={}", Thread.currentThread().getName(), asyncMethod, t.toString(), t);
                    } else {
                        log.error("【ASYNC EX】thread={} ex={}", Thread.currentThread().getName(), t.toString(), t);
                    }
                    throw t;
                }

            } finally {
	            	// スレッドプール再利用を考慮。MDCを「タスク実行前（前回の状態）」に戻す。
	            	// MDC.clear()だけでは、もともとスレッドにセットされていたMDC情報まで消してしまうため
	            	// 実行前状態へ復元する。
	            	if (previous != null) {
	            	    MDC.setContextMap(previous);
	            	} else {
	            	    MDC.clear();
	            	}
            }
        };
    }
}