package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.InsufficientBalanceException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {
    public static final long ZERO = 0L;

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable,
                        PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint detailUserPoint(long id) {
        return userPointTable.selectById(id);
    }

    public UserPoint chargeUserPoint(long id, long amount) {
        UserPoint userPoint = detailUserPoint(id);
        long chargedAmount = userPoint.point() + amount;
        return userPointTable.insertOrUpdate(id, chargedAmount);
    }

    public UserPoint useUserPoint(long id, long amount) {
        UserPoint userPoint = detailUserPoint(id);
        long leftPoint = userPoint.point() - amount;
        if (leftPoint < ZERO) {
            throw new InsufficientBalanceException(id, userPoint.point(), amount);
        }
        return userPointTable.insertOrUpdate(id, leftPoint);
    }

    public List<PointHistory> listsAllPointHistory(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }
}

