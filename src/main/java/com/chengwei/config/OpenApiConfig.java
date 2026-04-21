package com.chengwei.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chengWeiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("城味项目后端接口文档")
                        .description("包含用户端、店员端、店长端、管理端的接口在线文档")
                        .version("1.0.0")
                        .contact(new Contact().name("ChengWei Server")))
                .servers(Collections.singletonList(new Server().url("http://localhost:8081").description("本地开发环境")));
    }
}
