package io.hhplus.tdd.common.exception;

public class ExceededBalanceException extends RuntimeException {
    public ExceededBalanceException(long id, long point, long amount) {
        super("insufficient balance id:" + id + ", point: " + point + ", amount:" + amount);
    }
}
