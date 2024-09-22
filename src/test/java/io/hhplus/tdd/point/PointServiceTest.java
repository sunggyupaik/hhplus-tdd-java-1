package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PointServiceTest {
    private final PointService pointService;
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointServiceTest(@Autowired PointService pointService,
                            @Autowired UserPointTable userPointTable,
                            @Autowired PointHistoryTable pointHistoryTable) {
        this.pointService = pointService;
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    private static final long EXISTED_USER_ID = 10L;
    private static final long NOT_EXISTED_USER_ID = 99L;
    private static final long POINT_3000 = 3000L;
    private static final long POINT_0 = 0L;
    private static final long SYSTEM_CURRENT_TIME_MILLIS = 3000L;

    private static final long EXISTED_POINT_1_ID = 1L;
    private static final long EXISTED_POINT_2_ID = 2L;
    private static final long AMOUNT_3000 = 3000L;
    private static final long AMOUNT_5000 = 5000L;
    private static final long AMOUNT_8000 = 8000L;
    private static final TransactionType TRANSACTION_TYPE_CHARGE = TransactionType.CHARGE;

    private UserPoint userPoint;
    private PointHistory pointHistory_1;
    private PointHistory pointHistory_2;

    @BeforeEach
    public void setup() {
        userPoint = new UserPoint(EXISTED_USER_ID, AMOUNT_8000, SYSTEM_CURRENT_TIME_MILLIS);

        pointHistory_1 = new PointHistory(EXISTED_POINT_1_ID, EXISTED_USER_ID, AMOUNT_3000,
                TRANSACTION_TYPE_CHARGE, SYSTEM_CURRENT_TIME_MILLIS);
        pointHistory_2 = new PointHistory(EXISTED_POINT_2_ID, EXISTED_USER_ID, AMOUNT_5000,
                TRANSACTION_TYPE_CHARGE, SYSTEM_CURRENT_TIME_MILLIS);

        userPointTable.insertOrUpdate(EXISTED_USER_ID, AMOUNT_8000);
        pointHistoryTable.insert(EXISTED_USER_ID, AMOUNT_3000, TRANSACTION_TYPE_CHARGE, SYSTEM_CURRENT_TIME_MILLIS);
        pointHistoryTable.insert(EXISTED_USER_ID, AMOUNT_5000, TRANSACTION_TYPE_CHARGE, SYSTEM_CURRENT_TIME_MILLIS);
    }

    @Test
    @DisplayName("주어진 유저 식별자에 해당하는 유저 포인트를 조회하여 반환한다")
    void detailUserPointWithExistedId() {
        UserPoint detailedUserPoint = pointService.detailUserPoint(EXISTED_USER_ID);

        assertThat(detailedUserPoint.id()).isEqualTo(EXISTED_USER_ID);
        assertThat(detailedUserPoint.point()).isEqualTo(POINT_3000);
    }

    @Test
    @DisplayName("주어진 유저 식별자에 해당하는 유저 포인트가 없으면 포인트가 0인 유저 포인트를 반환한다")
    void detailUserPointWithNotExistedId() {
        UserPoint emptyUserPoint = pointService.detailUserPoint(NOT_EXISTED_USER_ID);

        assertThat(emptyUserPoint.id()).isEqualTo(NOT_EXISTED_USER_ID);
        assertThat(emptyUserPoint.point()).isEqualTo(POINT_0);
    }

    @Test
    @DisplayName("주어진 유저 식별자에 해당하는 포인트 내역을 조회하여 목록을 반환한다")
    void listsAllPointHistoryWithExistedId() {
        List<PointHistory> pointHistories = pointService.listsAllPointHistory(EXISTED_USER_ID);

        assertThat(pointHistories).hasSize(2);
        assertThat(pointHistories).contains(pointHistory_1, pointHistory_2);
    }

    @Test
    @DisplayName("주어진 유저 식별자에 해당하는 포인트 내역이 없으면 빈 목록을 반환한다")
    void listsAllPointHistoryWithNotExistedId() {
        List<PointHistory> pointHistories = pointService.listsAllPointHistory(NOT_EXISTED_USER_ID);

        assertThat(pointHistories).hasSize(0);
    }

    @Test
    @DisplayName("주어진 유저 식별자와 금액으로 해당 유저의 포인트를 충전하고 반환한다")
    void chargePointWithExistedId() {
        UserPoint baseUserPoint = pointService.detailUserPoint(EXISTED_USER_ID);
        UserPoint chargedUserPoint = pointService.chargeUserPoint(EXISTED_USER_ID, AMOUNT_3000);

        assertThat(baseUserPoint.point() + AMOUNT_3000).isEqualTo(chargedUserPoint.point());
    }

    @Test
    @DisplayName("주어진 유저 식별자와 금액으로 해당 유저의 포인트를 차감하고 반환한다")
    void usePointWithExistedId() {
        UserPoint baseUserPoint = pointService.detailUserPoint(EXISTED_USER_ID);
        UserPoint chargedUserPoint = pointService.useUserPoint(EXISTED_USER_ID, AMOUNT_3000);

        assertThat(baseUserPoint.point() - AMOUNT_3000).isEqualTo(chargedUserPoint.point());
    }
}
