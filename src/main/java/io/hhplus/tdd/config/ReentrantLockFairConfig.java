package io.hhplus.tdd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.locks.ReentrantLock;

@Configuration
public class ReentrantLockFairConfig {
    @Bean
    public ReentrantLock reentrantLockFair() {
        return new ReentrantLock(true);
    }
}
