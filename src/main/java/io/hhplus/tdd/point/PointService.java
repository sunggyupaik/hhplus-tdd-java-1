package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.InsufficientBalanceException;
import io.hhplus.tdd.exception.ExceededBalanceException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {
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
        long chargedPoint = userPoint.charge(amount);
        return userPointTable.insertOrUpdate(id, chargedPoint);
    }

    public UserPoint useUserPoint(long id, long amount) {
        UserPoint userPoint = detailUserPoint(id);
        long leftPoint = userPoint.use(amount);
        return userPointTable.insertOrUpdate(id, leftPoint);
    }

    public List<PointHistory> listsAllPointHistory(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }
}

