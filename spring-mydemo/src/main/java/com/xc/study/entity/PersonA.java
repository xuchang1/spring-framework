package com.xc.study.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author changxu13
 * @date 2021/7/14 15:34
 */
@Component
public class PersonA implements PersonAInterface {

	@Autowired
	private PersonBInterface personB;

	@Value("${profile: 22222}")
	private String value;

	@Override
	public void say() {
		System.out.println("say A");
	}
}
