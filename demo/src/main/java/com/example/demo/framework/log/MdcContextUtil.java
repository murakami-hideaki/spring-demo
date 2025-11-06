package com.example.demo.framework.log;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.slf4j.MDC;

/**
 * MDCコンテキスト操作のための共通ユーティリティクラス。
 * <p>
 * 主な用途:
 * <ul>
 *   <li>現在のMDCコンテキストの取得・復元</li>
 *   <li>MDCの明示的なクリア</li>
 *   <li>スレッド間（非同期）でのMDC伝搬支援</li>
 *   <li>asyncMethod（非同期処理識別子）のMDCセット</li>
 *   <li>リクエストIDの生成</li>
 * </ul>
 */
public final class MdcContextUtil {

    private MdcContextUtil() {
        // インスタンス化防止
    }

    /**
     * システム独自のリクエストID（UUID + 日付(yyyyMMddHHmmss)）を生成します。
     * 例: c8551cd1-4f93-47c3-bb6b-4eb63e99fdcb_20251101020719
     *
     * @return 独自リクエストID
     */
    public static String generateCustomRequestId() {
        String uuid = UUID.randomUUID().toString();
        String ymdhms = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return uuid + "_" + ymdhms;
    }  
    
    /**
     * 指定されたクラス・メソッド名情報から MdcKeyNames.ASYNC_METHOD をMDCに明示的にセットします。
     *
     * @param clazz クラス
     * @param methodName メソッド名
     */
    public static void putAsyncMethod(Class<?> clazz, String methodName) {
        if (clazz == null || methodName == null) {
            throw new IllegalArgumentException("clazz/methodName must not be null");
        }
        MDC.put(MdcKeyNames.ASYNC_METHOD, clazz.getName() + "." + methodName);
    }

    /**
     * 現在のMDCコンテキストを取得します。
     * @return MDC内容（変更不可Map）。MDCが空の場合は空Mapを返す。
     */
    public static Map<String, String> capture() {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return context != null ? context : Collections.emptyMap();
    }

    /**
     * 指定したMDCコンテキストを現在のスレッドに復元します。
     * 既存のMDC内容はクリアされます。
     * @param context 復元するMDCコンテキスト（nullの場合はMDCをクリアのみ）
     */
    public static void restore(Map<String, String> context) {
        MDC.clear();
        if (context != null) {
            MDC.setContextMap(context);
        }
    }

    /**
     * MDCを全クリアします。明示的な終了処理等に利用してください。
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * 指定されたRunnableを、呼び出し元のMDCコンテキストごとラップします。
     * スレッドが切り替わる非同期処理で利用してください。
     * @param task ラップするRunnable
     * @return MDCコンテキストを伝搬するRunnable
     */
    public static Runnable wrapWithMdcContext(Runnable task) {
        Map<String, String> context = capture();
        return () -> {
            restore(context);
            try {
                task.run();
            } finally {
                clear();
            }
        };
    }

    /**
     * 指定されたCallableを、呼び出し元のMDCコンテキストごとラップします。
     * @param task ラップするCallable
     * @param <T> 戻り値の型
     * @return MDCコンテキストを伝搬するCallable
     */
    public static <T> Callable<T> wrapWithMdcContext(Callable<T> task) {
        Map<String, String> context = capture();
        return () -> {
            restore(context);
            try {
                return task.call();
            } finally {
                clear();
            }
        };
    }
}