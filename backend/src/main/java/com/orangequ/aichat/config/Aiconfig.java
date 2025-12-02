package com.orangequ.aichat.config;

import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class Aiconfig {

    @Value("${ai.apiKey}")
    String apiKey;

    @Bean
    public ArkService arkService(){
        ConnectionPool connectionPool = new ConnectionPool(5, 1, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();
        ArkService service = ArkService.builder().dispatcher(dispatcher).connectionPool(connectionPool)
                .baseUrl("https://ark.cn-beijing.volces.com/api/v3/")
                .apiKey(apiKey)
                .build();
        return service;
    }
}
