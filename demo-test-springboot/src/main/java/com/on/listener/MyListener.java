package com.on.listener;

import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;

/**
 * 配置一个自定义监听器ApplicationListener，
 * 在resources目录的META-INF目录下的spring.factories配置文件中引入它。
 *
 * 监听器应该实现ApplicationListener接口，但是只能重写onApplicationEvent()方法，
 * 所以实现它的子类GenericApplicationListener，还可以重写supportsEventType()方法和supportsSourceType()方法，来订阅事件。
 * Spring底层也是实现了GenericApplicationListener接口，而不是ApplicationListener接口。
 */
public class MyListener implements GenericApplicationListener {

	/**
	 * 订阅事件表示：supportsEventType()方法和supportsSourceType()方法都返回true。
	 * 当supportsEventType()方法和supportsSourceType()方法都返回true时，才会对事件做出处理。
	 */

	// 监听事件。对感兴趣的事件，返回true，不感兴趣的返回false。
	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		if(eventType.getType().getClass().isInstance(ApplicationStartingEvent.class)) // 订阅ApplicationStartingEvent
			return true;
		else
			return  false;
	}

	// 监听资源的类型，对感兴趣的资源，返回true，不感兴趣的返回false。
	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}

	// 配置监听器的顺序
	@Override
	public int getOrder() {
		return 0;
	}

	// 对监听到的感兴趣的事件，做出的处理
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if(event instanceof ApplicationStartingEvent) // 订阅ApplicationStartingEvent事件
			System.out.println("****************MyListener ：Application is starting.****************");
	}
}
