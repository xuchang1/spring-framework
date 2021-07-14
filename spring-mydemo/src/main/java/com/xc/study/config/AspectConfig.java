package com.xc.study.config;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

/**
 * @author changxu13
 * @date 2021/7/14 15:44
 */
@Component
@EnableAspectJAutoProxy
@Aspect
public class AspectConfig {

	@Before("bean(personA)")
	public void doBeforeA() {
		System.out.println("do before A");
	}

	@Before("bean(personB)")
	public void doBeforeB() {
		System.out.println("do before B");
	}

}
