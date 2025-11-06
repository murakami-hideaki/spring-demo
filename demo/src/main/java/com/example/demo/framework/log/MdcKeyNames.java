package com.example.demo.framework.log;

/**
 * MDCキー名を一元管理する定数クラス。
 * 必要に応じて追加してください。
 */
public final class MdcKeyNames {
    private MdcKeyNames() {}

    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_URI = "requestUri";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String ASYNC_METHOD = "asyncMethod"; // 非同期処理の識別子用キー
    // 必要に応じて追加
}