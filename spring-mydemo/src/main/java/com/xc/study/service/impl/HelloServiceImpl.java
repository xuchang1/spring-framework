package com.xc.study.service.impl;

import com.xc.study.service.HelloService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author changxu13
 * @date 2021/7/15 16:29
 */
@Component
public class HelloServiceImpl implements HelloService {
	@Override
	@Async
	public void say() {
		System.out.println("hello say!");
	}
}
