package com.xc.study.entity;

import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author changxu13
 * @date 2021/7/13 14:58
 */
@Component
public class PersonFactoryBean implements SmartFactoryBean<Person> {
	@Override
	public Person getObject() throws Exception {
		return new Person("xc", 18, "男");
	}

	@Override
	public Class<?> getObjectType() {
		return Person.class;
	}

	@Override
	public boolean isEagerInit() {
		return true;
	}
}
