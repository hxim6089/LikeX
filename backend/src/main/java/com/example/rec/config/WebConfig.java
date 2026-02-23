package com.example.rec.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /images/** to the uploads directory in the project root
        // Use absolute path to ensure it works regardless of CWD
        String location = "file:./uploads/";
        
        registry.addResourceHandler("/images/**")
                .addResourceLocations(location);
    }
}
