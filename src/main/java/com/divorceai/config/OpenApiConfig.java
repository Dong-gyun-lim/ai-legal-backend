package com.divorceai.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI baseOpenAPI() {
        return new OpenAPI().info(
            new Info()
                .title("Divorce-AI Backend API")
                .version("v1.0")
                .description("Spring → Flask AI 연동 API 문서 (dev)")
        );
    }
}
