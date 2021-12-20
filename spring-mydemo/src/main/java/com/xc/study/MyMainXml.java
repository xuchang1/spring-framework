package com.xc.study;

import com.xc.study.xml.BeanB;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * xml相关加载逻辑
 *
 * @author changxu13
 * @date 2021/12/20 14:36
 */
public class MyMainXml {

	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("demo.xml");
		BeanB beanB = applicationContext.getBean(BeanB.class);
		beanB.say();
	}
}
