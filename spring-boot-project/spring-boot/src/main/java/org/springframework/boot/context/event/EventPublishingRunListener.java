/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ErrorHandler;

/**
 * {@link SpringApplicationRunListener} to publish {@link SpringApplicationEvent}s.
 * <p>
 * Uses an internal {@link ApplicationEventMulticaster} for the events that are fired
 * before the context is actually refreshed.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @since 1.0.0
 */
public class EventPublishingRunListener implements SpringApplicationRunListener, Ordered {

	private final SpringApplication application;

	private final String[] args;

	private final SimpleApplicationEventMulticaster initialMulticaster;

	public EventPublishingRunListener(SpringApplication application, String[] args) {
		this.application = application;
		this.args = args;
		this.initialMulticaster = new SimpleApplicationEventMulticaster();
		for (ApplicationListener<?> listener : application.getListeners()) {
			this.initialMulticaster.addApplicationListener(listener);
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}

	/**
	 * 广播 ApplicationStartingEvent 事件，通知监听器 SpringApplication 启动事件给监听器。
	 */
	@Override
	public void starting() {
		/**
		 * initialMulticaster 就是 SimpleApplicationEventMulticaster 对象，称为广播器。
		 * 它主要由两个方法，一个是用来广播事件，一个是执行监听器的onApplicationEvent方法。
		 *
		 * 这个类的工作方式有点抽象：
		 * 1、首先它会广播一个事件
		 * 		对应代码为 for (ApplicationListener<?> listener : getApplicationListeners(event, type))。
		 * 		getApplicationListeners(event, type)这个方法干了两件事，首先传递了两个参数事件和事件类型。意思就
		 * 		是告诉监听器现在有了一个type类型的event，你订阅了吗？
		 * 2、通知所有的监听器
		 * 		通过getApplicationListeners(event, type)方法通知所有的监听器（遍历所有的监听器），然后监听器会接
		 * 		收到事件，继而会判断自己是否订阅了这个事件。
		 * 3、监听器判断是否订阅了该事件
		 * 		Spring做的比较复杂，但是源码很简单。通过调用监听器内部提供的两个方法supportsEvent(eventType)和
		 * 		supportsSourceType(sourceType)来判断。这两个方法可以简单理解为通过传入一个类型返回一个boolean，
		 * 		只要有一个方法的boolean值为false就表示监听器没有订阅当前事件。如果事件被订阅了，会将监听器放入一个
		 * 		List集合中返回，后序代码中依次执行方法调用。
		 * 4、对事件做出响应处理
		 * 		步骤3中获取到订阅了事件的监听器List集合后，再对List中的监听器依次执行onApplicationEvent()方法调用，
		 * 		对事件做出响应处理。
		 *
		 * initialMulticaster
		 */
		this.initialMulticaster.multicastEvent(new ApplicationStartingEvent(this.application, this.args));
	}

	/**
	 * 广播 ApplicationEnvironmentPreparedEvent 事件，通知 ApplicationEnvironment 应用环境准备完毕事件，
	 * 此时 ApplicationContext 还没有被创建，只是应用所需的环境准备完毕。
	 * 例如：加载属性文件前时，需要构建好Spring环境
	 */
	@Override
	public void environmentPrepared(ConfigurableEnvironment environment) {
		this.initialMulticaster
				.multicastEvent(new ApplicationEnvironmentPreparedEvent(this.application, this.args, environment));
	}

	/**
	 * 广播 ApplicationContextInitializedEvent 事件，通知 ApplicationContext 准备完毕。
	 * 仅仅是准备完毕，ApplicationContext被创建并准备好，但是其中资源还没有被加载。
	 */
	@Override
	public void contextPrepared(ConfigurableApplicationContext context) {
		this.initialMulticaster
				.multicastEvent(new ApplicationContextInitializedEvent(this.application, this.args, context));
	}

	/**
	 * 广播 ApplicationPreparedEvent 事件，通知监听器 ApplicationContext 已经被加载。
	 * 此时ApplicationContext还没有被refresh刷新，只是加载了需要的资源。
	 */
	@Override
	public void contextLoaded(ConfigurableApplicationContext context) {
		for (ApplicationListener<?> listener : this.application.getListeners()) {
			if (listener instanceof ApplicationContextAware) {
				((ApplicationContextAware) listener).setApplicationContext(context);
			}
			context.addApplicationListener(listener);
		}
		this.initialMulticaster.multicastEvent(new ApplicationPreparedEvent(this.application, this.args, context));
	}

	/**
	 * 广播 ApplicationStartedEvent 事件，通知 ApplicationContext 已经被刷新，并且Spring应用已经被启动。
	 * 此时CommandLineRunners和ApplicationRunner还没有被调用。
	 * CommandLineRunners和ApplicationRunner是函数式接口，用来处理运行bean的回调。
	 */
	@Override
	public void started(ConfigurableApplicationContext context) {
		context.publishEvent(new ApplicationStartedEvent(this.application, this.args, context));
	}

	/**
	 * 在run()方法运行结束后，调用。
	 * 广播 ApplicationReadyEvent 事件，通知应用已经启动完成。
	 * ApplicationContext已经被refresh，并且所有的CommandLineRunner和ApplicationRunner函数式接口已经被调用。
	 *
	 * Called immediately before the run method finishes, when the application context has
	 * 	 * been refreshed and all {@link CommandLineRunner CommandLineRunners} and
	 * 	 * {@link ApplicationRunner ApplicationRunners} have been called.
	 */
	@Override
	public void running(ConfigurableApplicationContext context) {
		context.publishEvent(new ApplicationReadyEvent(this.application, this.args, context));
	}

	/**
	 * 广播 ApplicationFailedEvent 事件，当在应用启动过程中出现错误时广播
	 */
	@Override
	public void failed(ConfigurableApplicationContext context, Throwable exception) {
		ApplicationFailedEvent event = new ApplicationFailedEvent(this.application, this.args, context, exception);
		if (context != null && context.isActive()) {
			// Listeners have been registered to the application context so we should
			// use it at this point if we can
			context.publishEvent(event);
		}
		else {
			// An inactive context may not have a multicaster so we use our multicaster to
			// call all of the context's listeners instead
			if (context instanceof AbstractApplicationContext) {
				for (ApplicationListener<?> listener : ((AbstractApplicationContext) context)
						.getApplicationListeners()) {
					this.initialMulticaster.addApplicationListener(listener);
				}
			}
			this.initialMulticaster.setErrorHandler(new LoggingErrorHandler());
			this.initialMulticaster.multicastEvent(event);
		}
	}

	private static class LoggingErrorHandler implements ErrorHandler {

		private static final Log logger = LogFactory.getLog(EventPublishingRunListener.class);

		@Override
		public void handleError(Throwable throwable) {
			logger.warn("Error calling ApplicationEventListener", throwable);
		}

	}

}
