package com.xc.study.entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author changxu13
 * @date 2021/7/14 15:35
 */
@Component
public class PersonB implements PersonBInterface {

	@Autowired
	private PersonAInterface personA;

	@Override
	public void say() {
		System.out.println("say B");
	}
}
