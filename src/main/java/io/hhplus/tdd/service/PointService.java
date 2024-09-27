package io.hhplus.tdd.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.common.util.UserLock;
import io.hhplus.tdd.domain.point.PointHistory;
import io.hhplus.tdd.domain.point.TransactionType;
import io.hhplus.tdd.domain.point.UserPoint;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.Lock;

@Service
public class PointService {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final UserLock userLock;

    public PointService(UserPointTable userPointTable,
                        PointHistoryTable pointHistoryTable,
                        UserLock userLock) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
        this.userLock = userLock;
    }

    public UserPoint detailUserPoint(long id) {
        return userPointTable.selectById(id);
    }

    public UserPoint chargeUserPoint(long id, long amount) {
        Lock reentrantLockFair = userLock.userLock(id);

        reentrantLockFair.lock();
        try {
            UserPoint userPoint = detailUserPoint(id);
            long chargedPoint = userPoint.charge(amount);
            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
            return userPointTable.insertOrUpdate(id, chargedPoint);
        } finally {
            reentrantLockFair.unlock();
        }
    }

    public UserPoint useUserPoint(long id, long amount) {
        Lock reentrantLockFair = userLock.userLock(id);

        reentrantLockFair.lock();
        try {
            UserPoint userPoint = detailUserPoint(id);
            long leftPoint = userPoint.use(amount);
            pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());
            return userPointTable.insertOrUpdate(id, leftPoint);
        } finally {
            reentrantLockFair.unlock();
        }
    }

    public List<PointHistory> listsAllPointHistory(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }
}

