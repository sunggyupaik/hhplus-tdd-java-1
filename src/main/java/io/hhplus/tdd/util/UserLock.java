package io.hhplus.tdd.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class UserLock {
    private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();

    public Lock userLock(Long id) {
        return userLocks.computeIfAbsent(id, key -> new ReentrantLock(true));
    }
}
