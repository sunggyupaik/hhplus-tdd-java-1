package io.hhplus.tdd.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(long id, long point, long amount) {
        super("insufficient balance id:" + id + ", point:" + point + ", amount:" + amount);
    }
}
