package com.maracana.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(false);
        
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .resourceChain(false);
                
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .resourceChain(false);
                
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .resourceChain(false);
    }
} 