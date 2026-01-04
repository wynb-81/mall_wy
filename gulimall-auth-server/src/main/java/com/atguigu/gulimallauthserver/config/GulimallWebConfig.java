package com.atguigu.gulimallauthserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GulimallWebConfig implements WebMvcConfigurer {
    /**
     * 视图映射
     * @author wynb-81
     * @create 2025/6/3
     **/
    @Override
    public void addViewControllers(ViewControllerRegistry registry){
//        registry.addViewController("/login.html").setViewName("login");   不能用这个了，要让其再登录之后自动跳转到首页
        registry.addViewController("/reg.html").setViewName("reg");

    }
}
