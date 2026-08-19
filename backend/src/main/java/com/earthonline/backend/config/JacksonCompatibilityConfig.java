package com.earthonline.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonCompatibilityConfig {
    @Bean
    public ObjectMapper jackson2ObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
