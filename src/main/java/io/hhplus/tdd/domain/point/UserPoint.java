package io.hhplus.tdd.domain.point;

import io.hhplus.tdd.common.exception.ExceededBalanceException;
import io.hhplus.tdd.common.exception.InsufficientBalanceException;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    public static final long ZERO_POINT = 0L;
    public static final long MAX_POINT = 10000L;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public long charge(long amount) {
        long chargedPoint = this.point + amount;
        if (chargedPoint > MAX_POINT) {
            throw new ExceededBalanceException(id, this.point, amount);
        }

        return chargedPoint;
    }

    public long use(long amount) {
        long leftPoint = this.point - amount;
        if (leftPoint < ZERO_POINT) {
            throw new InsufficientBalanceException(id, this.point, amount);
        }

        return leftPoint;
    }
}
