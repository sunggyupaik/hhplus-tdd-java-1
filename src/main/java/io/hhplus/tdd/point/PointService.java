package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
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
        long chargedAmount = userPoint.point() + amount;
        return userPointTable.insertOrUpdate(id, chargedAmount);
    }

    public List<PointHistory> listsAllPointHistory(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }
}

