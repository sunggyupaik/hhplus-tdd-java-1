package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.ExceededBalanceException;
import io.hhplus.tdd.exception.InsufficientBalanceException;
import io.hhplus.tdd.util.UserLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest
class PointServiceTest {
    private PointService pointService;
    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;
    private UserLock userLock;

    private static final long EXISTED_USER_ID = 10L;
    private static final long NOT_EXISTED_USER_ID = 99L;
    private static final long POINT_0 = 0L;
    private static final long POINT_2000 = 2000L;
    private static final long POINT_5000 = 5000L;
    private static final long POINT_8000 = 8000L;
    private static final long SYSTEM_CURRENT_TIME_MILLIS = 3000L;

    private static final long EXISTED_POINT_1_ID = 1L;
    private static final long EXISTED_POINT_2_ID = 2L;
    private static final long AMOUNT_3000 = 3000L;
    private static final long AMOUNT_5000 = 5000L;
    private static final long AMOUNT_10000L = 10000L;
    private static final TransactionType TRANSACTION_TYPE_CHARGE = TransactionType.CHARGE;

    private UserPoint userPoint;
    private UserPoint chargedUserPoint;
    private UserPoint usedUserPoint;
    private UserPoint emptyUserPoint;
    private PointHistory pointHistory_1;
    private PointHistory pointHistory_2;
    private List<PointHistory> pointHistories;

    private Lock reentrantLockFair;

    @BeforeEach
    public void setup() {
        userPointTable = mock(UserPointTable.class);
        pointHistoryTable = mock(PointHistoryTable.class);
        userLock = mock(UserLock.class);
        pointService = new PointService(userPointTable, pointHistoryTable, userLock);

        userPoint = new UserPoint(EXISTED_USER_ID, POINT_5000, SYSTEM_CURRENT_TIME_MILLIS);
        chargedUserPoint = new UserPoint(EXISTED_USER_ID, POINT_8000, SYSTEM_CURRENT_TIME_MILLIS);
        usedUserPoint = new UserPoint(EXISTED_USER_ID, POINT_2000, SYSTEM_CURRENT_TIME_MILLIS);
        emptyUserPoint = new UserPoint(NOT_EXISTED_USER_ID, POINT_0, SYSTEM_CURRENT_TIME_MILLIS);

        pointHistory_1 = new PointHistory(EXISTED_POINT_1_ID, EXISTED_USER_ID, AMOUNT_3000,
                TRANSACTION_TYPE_CHARGE, SYSTEM_CURRENT_TIME_MILLIS);
        pointHistory_2 = new PointHistory(EXISTED_POINT_2_ID, EXISTED_USER_ID, AMOUNT_5000,
                TRANSACTION_TYPE_CHARGE, SYSTEM_CURRENT_TIME_MILLIS);
        pointHistories = List.of(pointHistory_1, pointHistory_2);

        reentrantLockFair = new ReentrantLock(true);
    }

    @Test
    @DisplayName("주어진 유저 식별자에 해당하는 유저 포인트를 조회하여 반환한다")
    void detailUserPointWithExistedId() {
        given(userPointTable.selectById(EXISTED_USER_ID)).willReturn(userPoint);

        UserPoint detailedUserPoint = pointService.detailUserPoint(EXISTED_USER_ID);

        assertThat(detailedUserPoint.id()).isEqualTo(EXISTED_USER_ID);
        assertThat(detailedUserPoint.point()).isEqualTo(POINT_5000);
    }

    @Test
    @DisplayName("주어진 유저 식별자에 해당하는 유저 포인트가 없으면 포인트가 0인 유저 포인트를 반환한다")
    void detailUserPointWithNotExistedId() {
        given(userPointTable.selectById(NOT_EXISTED_USER_ID)).willReturn(emptyUserPoint);

        UserPoint emptyUserPoint = pointService.detailUserPoint(NOT_EXISTED_USER_ID);

        assertThat(emptyUserPoint.id()).isEqualTo(NOT_EXISTED_USER_ID);
        assertThat(emptyUserPoint.point()).isEqualTo(POINT_0);
    }

    @Test
    @DisplayName("주어진 유저 식별자에 해당하는 포인트 내역을 조회하여 목록을 반환한다")
    void listsAllPointHistoryWithExistedId() {
        given(pointHistoryTable.selectAllByUserId(EXISTED_USER_ID)).willReturn(pointHistories);

        List<PointHistory> pointHistories = pointService.listsAllPointHistory(EXISTED_USER_ID);

        assertThat(pointHistories).hasSize(2);
        assertThat(pointHistories).contains(pointHistory_1, pointHistory_2);
    }

    @Test
    @DisplayName("주어진 유저 식별자에 해당하는 포인트 내역이 없으면 빈 목록을 반환한다")
    void listsAllPointHistoryWithNotExistedId() {
        given(pointHistoryTable.selectAllByUserId(NOT_EXISTED_USER_ID)).willReturn(List.of());

        List<PointHistory> pointHistories = pointService.listsAllPointHistory(NOT_EXISTED_USER_ID);

        assertThat(pointHistories).hasSize(0);
    }

    @Test
    @DisplayName("주어진 유저 식별자와 금액으로 해당 유저의 포인트를 충전하고 반환한다")
    void chargePointWithExistedId() {
        given(userPointTable.selectById(EXISTED_USER_ID)).willReturn(userPoint);
        given(userPointTable.insertOrUpdate(EXISTED_USER_ID, POINT_8000)).willReturn(chargedUserPoint);
        given(userLock.userLock(EXISTED_USER_ID)).willReturn(reentrantLockFair);

        UserPoint initUserPoint = pointService.detailUserPoint(EXISTED_USER_ID);
        UserPoint updatedUserPoint = pointService.chargeUserPoint(EXISTED_USER_ID, AMOUNT_3000);

        assertThat(initUserPoint.point() + AMOUNT_3000).isEqualTo(updatedUserPoint.point());
    }

    @Test
    @DisplayName("주어진 유저 식별자와 금액으로 해당 유저의 포인트를 차감하고 반환한다")
    void usePointWithExistedId() {
        given(userPointTable.selectById(EXISTED_USER_ID)).willReturn(userPoint);
        given(userPointTable.insertOrUpdate(EXISTED_USER_ID, POINT_2000)).willReturn(usedUserPoint);
        given(userLock.userLock(EXISTED_USER_ID)).willReturn(reentrantLockFair);

        UserPoint initUserPoint = pointService.detailUserPoint(EXISTED_USER_ID);
        UserPoint updatedUserPoint = pointService.useUserPoint(EXISTED_USER_ID, AMOUNT_3000);

        assertThat(initUserPoint.point() - AMOUNT_3000).isEqualTo(updatedUserPoint.point());
    }

    @Test
    @DisplayName("주어진 유저 식별자와 금액으로 해당 유저의 포인트 차감이 0원 미만이면 잔고 부족 예외를 반환한다.")
    void usePointLessThanZero() {
        given(userPointTable.selectById(EXISTED_USER_ID)).willReturn(userPoint);
        given(userLock.userLock(EXISTED_USER_ID)).willReturn(reentrantLockFair);

        UserPoint baseUserPoint = pointService.detailUserPoint(EXISTED_USER_ID);

        assertThatThrownBy(
                () -> pointService.useUserPoint(EXISTED_USER_ID, AMOUNT_10000L)
        )
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    @DisplayName("주어진 유저 식별자와 금액으로 해당 유저의 포인트 충전이 최대를 초과하면 잔고 초과 예외를 반환한다.")
    void chargePointMoreThanMax() {
        given(userPointTable.selectById(EXISTED_USER_ID)).willReturn(userPoint);
        given(userLock.userLock(EXISTED_USER_ID)).willReturn(reentrantLockFair);

        UserPoint baseUserPoint = pointService.detailUserPoint(EXISTED_USER_ID);

        assertThatThrownBy(
                () -> pointService.chargeUserPoint(EXISTED_USER_ID, AMOUNT_10000L)
        )
                .isInstanceOf(ExceededBalanceException.class);
    }
}
