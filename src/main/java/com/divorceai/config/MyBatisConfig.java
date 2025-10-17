package com.divorceai.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.divorceai.mapper")
public class MyBatisConfig {
}
