package com.example.demo.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.example.demo.framework.log.MdcTaskDecorator;

/**
 * 非同期処理（@Async）を有効化し、MDC伝搬対応のスレッドプールを提供する設定クラスです。
 *
 * <ul>
 *   <li>コアスレッド数（corePoolSize）は外部プロパティから読み込みます。（環境により切り替え）<br>
 *     設定プロパティ名：<b>async.executor.corePoolSize</b><br>
 *   <li>最大スレッド数（maxPoolSize）: プールが一時的に増加できるスレッドの最大数。デフォルト値はInteger.MAX_VALUEです。</li>
 *   <li>タスクキューの最大数（queueCapacity）: スレッドが足りない場合にタスクを一時的に保持するキューの容量。デフォルト値はInteger.MAX_VALUEです。</li>
 *   <li>MDC伝搬: ログのトレース情報等を非同期スレッドにも引き継ぐための仕組みです。</li>
 * </ul>
 *
 * <p>
 * <b>【具体例】</b><br>
 * コアスレッド数: 10, 最大スレッド数: 50, キューサイズ: 1000 の場合の挙動は以下の通りです。<br>
 * <ol>
 *   <li>同時に10件まではスレッド（コア）で即実行される。</li>
 *   <li>11件目～1,010件目まではキューに蓄積され、順次実行される。</li>
 *   <li>1,011件目～1,050件目までは、（キューが満杯なので）スレッド数をコア超えで最大50まで増やして実行される。</li>
 *   <li>1,051件目以降はRejectedExecutionException（デフォルト動作）となり、タスクが拒否される。</li>
 * </ol>
 * </p>
 */
@Configuration
@EnableAsync // 非同期処理を有効化
public class AsyncConfig {

    /**
     * 非同期処理用のThreadPoolTaskExecutorを生成します。
     *
     * <p>
     * コアスレッド数（corePoolSize）は外部プロパティ
     * <b>async.executor.corePoolSize</b> で切り替え可能です。
     * </p>
     *
     * @param corePoolSize スレッドプールのコアスレッド数（外部プロパティで環境ごとに切り替え）
     * @return 非同期タスク実行用のスレッドプールExecutor
     */
    @Bean(name = "asyncTaskExecutor")
    ThreadPoolTaskExecutor taskExecutor(
            @Value("${async.executor.corePoolSize:8}") int corePoolSize 
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // コアスレッド数（corePoolSize）は外部プロパティで設定
        executor.setCorePoolSize(corePoolSize);
        
        // 最大スレッド数。
        // タスクが多く、キューも満杯になった場合に最大どこまでスレッド数を増やすかの上限です。
        // デフォルト値はInteger.MAX_VALUE。
        // executor.setMaxPoolSize(50);

        // タスクキューの最大数。
        // スレッドが足りず即時実行できないタスクを一時的にためておくキューの容量です。
        // デフォルト値はInteger.MAX_VALUE。
        // executor.setQueueCapacity(1000);

        // スレッド名の接頭辞を設定（スレッドダンプやログで識別しやすくするため）
        executor.setThreadNamePrefix("AsyncTask-");

        // MDCの伝搬を有効化（親スレッドのログコンテキストを非同期タスクにも引き継ぐ）
        executor.setTaskDecorator(new MdcTaskDecorator());

        // シャットダウン時に、現在実行中のタスクが完了するまで待機する
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // シャットダウン時の待機最大秒数（30秒まで待機し、未完了タスクは強制終了）
        executor.setAwaitTerminationSeconds(30);

        // スレッドプールの初期化（明示的に呼び出し。Spring管理下なら自動でも呼ばれる）
        executor.initialize();

        return executor;
    }
}