package com.example.demo.framework.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Webアプリケーション向け共通ログ出力ユーティリティ。
 *
 * <p>目的
 * <ul>
 *   <li>アプリケーションの各クラスから一貫したログ出力を簡素に行うための軽量ラッパーを提供します。</li>
 *   <li>SLF4J に委譲しつつ、遅延評価（Supplier）とパラメータ化ログの利便性を提供します。</li>
 * </ul>
 *
 * <p>主要仕様
 * <ul>
 *   <li>{@link #getLogger(Class)} は指定クラスに紐づく WebLogger インスタンスを返します。呼び出し側は
 *       {@code private static final WebLogger log = WebLogger.getLogger(MyClass.class);} の形で保持することを推奨します。</li>
 *   <li>パラメータ化ログ（"{}"）と遅延評価（Supplier）をサポートします。Supplier 版は該当レベルが有効な場合にのみ評価されます。</li>
 *   <li>引数 {@code clazz} が {@code null} の場合、ログ出力のフォーマットを統一するために内部 Logger として
 *       {@code WebLogger.class} の Logger を使用し、エラーメッセージ（初回のみ）を SLF4J 経由で出力します（EMITTED フラグで一度だけ出力）。</li>
 *   <li>MDC の操作や追加コンテキストの注入はこのクラスでは行いません。必要なら別ユーティリティを利用してください。</li>
 *   <li>WARN / ERROR の単純版（String 引数）は常に出力する方針です（内部での二重判定は行いません）。</li>
 * </ul>
 *
 * <p>フェールセーフ動作（clazz が null の場合）
 * <ol>
 *   <li>{@code WebLogger.class} の Logger を内部的に使ってエラーログを出します（ログ出力フォーマットは他のクラスと統一されます）。</li>
 *   <li>このエラーログは同一 JVM 起動中に一度だけ出力されます（過剰なノイズを防ぐため）。</li>
 *   <li>以降返される WebLogger インスタンスは通常モード（WebLogger.class に紐づく Logger）で動作します。</li>
 * </ol>
 *
 * <p>スレッド安全性: インスタンスは不変（immutable）であり、複数スレッドから安全に再利用可能です。
 *
 * <p>利用例
 * <pre>{@code
 * public class MyController {
 *     private static final WebLogger log = WebLogger.getLogger(MyController.class);
 *
 *     public void handle() {
 *         log.info("処理開始");
 *         
 *         //引数の遅延評価ありのログ出力。DEBUGレベルが無効の場合はexpensiveToString()自体が実行されない。
 *         //引数を組み立てるための処理が重い場合はこちらの呼び出し方法を推奨。
 *         log.debug(() -> expensiveToString());
 *
 *         try {
 *             // 処理本体
 *         } catch (Exception e) {
 *             // 例外オブジェクトを指定してログ出力（%msg と %ex に出力される）
 *             log.error("致命的な例外が発生しました", e);
 *         }
 *     }
 * }
 * }</pre>
 */
public final class WebLogger {

    private static final String FALLBACK_MSG =
            "[WebLogger] getLogger called with null clazz — using WebLogger.class as fallback logger. "
            + "Please initialize loggers using WebLogger.getLogger(YourClass.class).";

    // Ensure we emit fallback error only once to avoid spamming logs
    private static final AtomicBoolean FALLBACK_EMITTED = new AtomicBoolean(false);

    private final Logger logger;

    private WebLogger(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
    }

    /**
     * 指定されたクラスに紐づく WebLogger インスタンスを返します。
     *
     * <p>仕様:
     * <ul>
     *   <li>clazz が非 null の場合、{@code LoggerFactory.getLogger(clazz)} を内部で使用します。</li>
     *   <li>clazz が null の場合、{@code LoggerFactory.getLogger(WebLogger.class)} を内部で使用し、
     *       一度だけエラーログ（指定ミスの通知）を出力します。</li>
     * </ul>
     *
     * @param clazz ロガー名として使うクラス（null 可）
     * @return WebLogger インスタンス（null 指定時は WebLogger.class に紐づくインスタンスを返す）
     */
    public static WebLogger getLogger(Class<?> clazz) {
        if (clazz == null) {
            Logger fallback = LoggerFactory.getLogger(WebLogger.class);
            emitFallbackErrorOnce(fallback);
            return new WebLogger(fallback);
        }
        return new WebLogger(LoggerFactory.getLogger(clazz));
    }

    private static void emitFallbackErrorOnce(Logger fallback) {
        if (FALLBACK_EMITTED.compareAndSet(false, true)) {
            // Build a helpful stack trace message to assist debugging, but log via SLF4J so format is unified
            StringWriter sw = new StringWriter();
            new Exception("WebLogger.getLogger(null) initialization trace").printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();
            // Use error so it is always visible under the project's error policy
            fallback.error(FALLBACK_MSG + "\n" + trace);
        }
    }

    /* --- isEnabled --- */
    public boolean isTraceEnabled() { return logger.isTraceEnabled(); }
    public boolean isDebugEnabled() { return logger.isDebugEnabled(); }
    public boolean isInfoEnabled()  { return logger.isInfoEnabled(); }
    public boolean isWarnEnabled()  { return logger.isWarnEnabled(); }
    public boolean isErrorEnabled() { return logger.isErrorEnabled(); }

    /* --- TRACE --- */
    public void trace(String msg) { if (logger.isTraceEnabled()) logger.trace(msg); }
    public void trace(String format, Object... args) { if (logger.isTraceEnabled()) logger.trace(format, args); }
    public void trace(Supplier<String> msgSupplier) { if (logger.isTraceEnabled() && msgSupplier != null) logger.trace(msgSupplier.get()); }
    public void trace(String msg, Throwable t) { if (logger.isTraceEnabled()) logger.trace(msg, t); }

    /* --- DEBUG --- */
    public void debug(String msg) { if (logger.isDebugEnabled()) logger.debug(msg); }
    public void debug(String format, Object... args) { if (logger.isDebugEnabled()) logger.debug(format, args); }
    public void debug(Supplier<String> msgSupplier) { if (logger.isDebugEnabled() && msgSupplier != null) logger.debug(msgSupplier.get()); }
    public void debug(String msg, Throwable t) { if (logger.isDebugEnabled()) logger.debug(msg, t); }

    /* --- INFO --- */
    public void info(String msg) { if (logger.isInfoEnabled()) logger.info(msg); }
    public void info(String format, Object... args) { if (logger.isInfoEnabled()) logger.info(format, args); }
    public void info(Supplier<String> msgSupplier) { if (logger.isInfoEnabled() && msgSupplier != null) logger.info(msgSupplier.get()); }
    public void info(String msg, Throwable t) { if (logger.isInfoEnabled()) logger.info(msg, t); }

    /* --- WARN --- */
    public void warn(String msg) { logger.warn(msg); }
    public void warn(String format, Object... args) { if (logger.isWarnEnabled()) logger.warn(format, args); }
    public void warn(Supplier<String> msgSupplier) { if (logger.isWarnEnabled() && msgSupplier != null) logger.warn(msgSupplier.get()); }
    public void warn(String msg, Throwable t) { logger.warn(msg, t); }

    /* --- ERROR --- */
    public void error(String msg) { logger.error(msg); }
    public void error(String format, Object... args) { if (logger.isErrorEnabled()) logger.error(format, args); }
    public void error(Supplier<String> msgSupplier) { if (logger.isErrorEnabled() && msgSupplier != null) logger.error(msgSupplier.get()); }
    public void error(String msg, Throwable t) { logger.error(msg, t); }
    public void error(Supplier<String> msgSupplier, Throwable t) { if (logger.isErrorEnabled() && msgSupplier != null) logger.error(msgSupplier.get(), t); }
}