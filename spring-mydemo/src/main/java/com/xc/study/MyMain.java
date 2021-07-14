package com.xc.study;

import com.xc.study.config.MyConfig;
import com.xc.study.entity.*;
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

		PersonAInterface personA = context.getBean(PersonAInterface.class);
		personA.say();

		PersonBInterface personB = context.getBean(PersonBInterface.class);
		personB.say();

		System.out.println(person);
	}
}
