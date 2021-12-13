package com.xc.study;

import com.xc.study.config.MyConfig;
import com.xc.study.entity.*;
import com.xc.study.service.HelloService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author changxu13
 * @date 2021/7/11 13:04
 */
public class MyMain {
	public static void main(String[] args) {
		ApplicationContext context = new AnnotationConfigApplicationContext(MyConfig.class);
//		Person person = context.getBean(Person.class);
		Person person = (Person) context.getBean("personFactoryBean");
		System.out.println(person);

		PersonAInterface personA = context.getBean(PersonAInterface.class);
		personA.say();

		// 当前PersonA bean通过jdk动态代理生成了代理对象缓存在了beanFactory中,无法直接通过直接按PersonA类型的方式来获取,因为缓存的bean是接口类型,而不是PersonA类型
//		PersonA personA = context.getBean(PersonA.class);
//		personA.say();

//		PersonBInterface personB = context.getBean(PersonBInterface.class);
//		personB.say();
//
//		HelloService helloService = context.getBean(HelloService.class);
//		helloService.say();
	}
}
