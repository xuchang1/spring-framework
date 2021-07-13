package com.xc.study.config;

import com.xc.study.entity.Person;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author changxu13
 * @date 2021/7/11 13:25
 */
@Configuration
@ComponentScan("com.xc.study")
public class MyConfig {

//	@Bean
	public Person person() {
		return new Person("xc", 18, "男");
	}
}
