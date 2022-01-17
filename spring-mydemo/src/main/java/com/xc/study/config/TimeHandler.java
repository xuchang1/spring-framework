package com.xc.study.config;

/**
 * @author changxu13
 * @date 2022/1/17 15:27
 */
public class TimeHandler {

	public void printBeforeTime() {
		System.out.println("beforeTime:" + System.currentTimeMillis());
	}

	public void printAfterTime() {
		System.out.println("afterTime:" + System.currentTimeMillis());
	}
}
