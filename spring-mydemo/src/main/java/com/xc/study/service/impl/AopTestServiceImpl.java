package com.xc.study.service.impl;

import com.xc.study.service.AopTestService;

/**
 * @author changxu13
 * @date 2022/1/17 15:25
 */
public class AopTestServiceImpl implements AopTestService {
	@Override
	public String say(String id) {
		System.out.println("执行say方法");
		return id;
	}
}
