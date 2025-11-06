package com.example.demo.framework.log;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * <h2>ControllerLoggingAspect</h2>
 * <p>
 * 全てのControllerクラスのpublicメソッド実行前後にログを出力するAOPアスペクトです。<br>
 * 実行開始・終了・例外発生時に、独自のWebLoggerインスタンスを介してログを記録します。<br>
 * ※個人情報・機微情報保護のため、メソッド引数（パラメータ）はログ出力しません。<br>
 * </p>
 */
@Aspect
@Component
public class ControllerLoggingAspect {

    /**
     * このアスペクト専用のWebLoggerインスタンス。
     */
    private static final WebLogger log = WebLogger.getLogger(ControllerLoggingAspect.class);

    /**
     * Controllerの全publicメソッドをポイントカット。
     * RestController, Controllerアノテーションが付与されたクラスを対象とする。
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) || within(@org.springframework.stereotype.Controller *)")
    public void controllerMethods() {}

    /**
     * Controllerメソッド実行前にログ出力（引数は出力しない）。
     *
     * @param joinPoint ジョインポイント
     */
    @Before("controllerMethods()")
    public void beforeController(JoinPoint joinPoint) {
        log.info("【START】{}.{}",
            joinPoint.getSignature().getDeclaringTypeName(),
            joinPoint.getSignature().getName());
    }

    /**
     * Controllerメソッド実行後（正常終了時）にログ出力。
     *
     * @param joinPoint ジョインポイント
     * @param result    戻り値
     */
    @AfterReturning(pointcut = "controllerMethods()", returning = "result")
    public void afterReturningController(JoinPoint joinPoint, Object result) {
        log.info("【END】{}.{}",
            joinPoint.getSignature().getDeclaringTypeName(),
            joinPoint.getSignature().getName());
    }

    /**
     * Controllerメソッド実行中に例外発生時、エラーログ出力。
     *
     * @param joinPoint ジョインポイント
     * @param ex        例外
     */
    @AfterThrowing(pointcut = "controllerMethods()", throwing = "ex")
    public void afterThrowingController(JoinPoint joinPoint, Throwable ex) {
        log.error("【EXCEPTION】{}.{} ex={}",
            joinPoint.getSignature().getDeclaringTypeName(),
            joinPoint.getSignature().getName(),
            ex.toString(), ex);
    }
}