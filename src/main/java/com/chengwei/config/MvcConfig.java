package com.chengwei.config;

import com.chengwei.utils.LoginInterceptor;
import com.chengwei.utils.AdminLoginInterceptor;
import com.chengwei.utils.ClerkLoginInterceptor;
import com.chengwei.utils.RefreshInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Autowired
    LoginInterceptor loginInterceptor;
    @Autowired
    ClerkLoginInterceptor clerkLoginInterceptor;
    @Autowired
    AdminLoginInterceptor adminLoginInterceptor;
    @Autowired
    RefreshInterceptor refreshInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor).order(1)
                .excludePathPatterns(
                        "/shop/**",
                        "/shop-comments/of/shop/**",
                        "/voucher/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/blog/hot",
                        "/user/code",
                        "/user/login",
                        "/clerk/**",
                        "/admin/login"
                );
        registry.addInterceptor(clerkLoginInterceptor).addPathPatterns("/clerk/**")
                .excludePathPatterns("/clerk/login")
                .order(1);
        registry.addInterceptor(adminLoginInterceptor).addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login")
                .order(1);
        registry.addInterceptor(refreshInterceptor).addPathPatterns("/**").order(0);
    }
}
