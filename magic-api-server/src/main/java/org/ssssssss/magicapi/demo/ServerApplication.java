package org.ssssssss.magicapi.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class ServerApplication {

	private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);
		logger.info("Magic API Server 启动完成");
	}

	/**
	 * 根路径 / 直接跳转 magic-api 工作台（默认 /magic/web/index.html），
	 * 路径跟随 magic-api.web 配置变化
	 */
	@Bean
	public WebMvcConfigurer workbenchRedirectConfigurer(
			@Value("${magic-api.web:/magic/web}") String webPath) {
		return new WebMvcConfigurer() {
			@Override
			public void addViewControllers(ViewControllerRegistry registry) {
				registry.addRedirectViewController("/", webPath + "/index.html");
			}
		};
	}
}
