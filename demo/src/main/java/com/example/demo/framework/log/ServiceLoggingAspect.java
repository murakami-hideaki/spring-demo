package com.example.demo.framework.log;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * <h2>ServiceLoggingAspect</h2>
 * <p>
 * 全てのServiceクラスのpublicメソッド実行前後にログを出力するAOPアスペクトです。<br>
 * 実行開始・終了・例外発生時に、独自のWebLoggerインスタンスを介してログを記録します。<br>
 * ※個人情報・機微情報保護のため、メソッド引数（パラメータ）はログ出力しません。<br>
 * </p>
 */
@Aspect
@Component
public class ServiceLoggingAspect {

    /**
     * このアスペクト専用のWebLoggerインスタンス。
     */
    private static final WebLogger log = WebLogger.getLogger(ServiceLoggingAspect.class);

    /**
     * Serviceの全publicメソッドをポイントカット。
     * Serviceアノテーションが付与されたクラスを対象とする。
     */
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceMethods() {}

    /**
     * Serviceメソッド実行前にログ出力（引数は出力しない）。
     *
     * @param joinPoint ジョインポイント
     */
    @Before("serviceMethods()")
    public void beforeService(JoinPoint joinPoint) {
        log.info("【START】{}.{}",
            joinPoint.getSignature().getDeclaringTypeName(),
            joinPoint.getSignature().getName());
    }

    /**
     * Serviceメソッド実行後（正常終了時）にログ出力。
     *
     * @param joinPoint ジョインポイント
     * @param result    戻り値
     */
    @AfterReturning(pointcut = "serviceMethods()", returning = "result")
    public void afterReturningService(JoinPoint joinPoint, Object result) {
        log.info("【END】{}.{}",
            joinPoint.getSignature().getDeclaringTypeName(),
            joinPoint.getSignature().getName());
    }

    /**
     * Serviceメソッド実行中に例外発生時、エラーログ出力。
     *
     * @param joinPoint ジョインポイント
     * @param ex        例外
     */
    @AfterThrowing(pointcut = "serviceMethods()", throwing = "ex")
    public void afterThrowingService(JoinPoint joinPoint, Throwable ex) {
        log.error("【EXCEPTION】{}.{} ex={}",
            joinPoint.getSignature().getDeclaringTypeName(),
            joinPoint.getSignature().getName(),
            ex.toString(), ex);
    }
}