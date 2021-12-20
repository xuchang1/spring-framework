package com.xc.study.xml;

/**
 * @author changxu13
 * @date 2021/12/20 14:40
 */
public class BeanB {

	private BeanA beanA;

	public void say() {
		System.out.println("BBBB");
		beanA.say();
	}

	public void setBeanA(BeanA beanA) {
		this.beanA = beanA;
	}
}
