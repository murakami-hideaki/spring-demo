package com.example.demo.framework.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.framework.log.MdcContextUtil;
import com.example.demo.framework.log.WebLogger;
import com.example.demo.framework.service.AsyncService;

@RestController
public class AsyncController {

    @Autowired
    private AsyncService asyncService;
    
    private static final WebLogger log = WebLogger.getLogger(AsyncController.class);

    @GetMapping("/asynclog")
    public String asynclog() {

    		MdcContextUtil.putAsyncMethod(AsyncController.class, ".asynclog");
    	
        // 非同期メソッド呼び出し
        asyncService.helloAsync();

        // 応答（レスポンス）
        return "Asyncログ出力テスト完了";
    }
}