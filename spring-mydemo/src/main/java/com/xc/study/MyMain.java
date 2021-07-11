package com.xc.study;

import com.xc.study.config.MyConfig;
import com.xc.study.entity.Person;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author changxu13
 * @date 2021/7/11 13:04
 */
public class MyMain {
	public static void main(String[] args) {
		ApplicationContext context = new AnnotationConfigApplicationContext(MyConfig.class);
		Person person = context.getBean(Person.class);
		System.out.println(person);
	}
}
