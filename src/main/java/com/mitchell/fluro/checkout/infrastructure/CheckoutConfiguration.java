package com.mitchell.fluro.checkout.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration(proxyBeanMethods = false)
@ImportResource("${checkout.context:classpath:ApplicationContext.xml}")
public class CheckoutConfiguration {
}
