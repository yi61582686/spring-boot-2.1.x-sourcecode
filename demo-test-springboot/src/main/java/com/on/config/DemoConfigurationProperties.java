package com.on.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SpringBoot自动配置：从配置文件中获取属性值自动装配
 */
@Component
@ConfigurationProperties(prefix = "on", ignoreInvalidFields = true)
public class DemoConfigurationProperties {

	private String name;
	private Integer age;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

}
