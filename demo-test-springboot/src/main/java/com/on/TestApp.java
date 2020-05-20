package com.on;

import com.on.config.Config;
import com.on.config.DemoConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;


@SpringBootApplication
public class TestApp {

	public static void main(String[] args) {
		/**
		 * 传入加了@SpringBootApplication注解的类，就可以启动SpringBoot项目，
		 * 注解@SpringBootApplication可以不在当前类。
		 */
//		SpringApplication application = new SpringApplication(Config.class);
		SpringApplication application = new SpringApplication(TestApp.class);
		// SpringBoot 这样获取到context后，可以做很多配置
		ConfigurableApplicationContext context = application.run(args);
		System.out.println(context.getBean(DemoConfigurationProperties.class).getAge());
//		showAllBeans(context);
		showNumsOfBeans(context);

	}

	// 显示所有的bean
	public static void showAllBeans(ConfigurableApplicationContext context) {
		System.out.println("-------------------------------------------------------------------");
		for(String beanDefinitionName : context.getBeanDefinitionNames()) {
			System.out.println(beanDefinitionName);
		}
		System.out.println("-------------------------------------------------------------------");
	}

	public static void showNumsOfBeans(ConfigurableApplicationContext context) {
		System.out.println("The Numbers of Beans are ：" + context.getBeanDefinitionNames().length);
	}
}