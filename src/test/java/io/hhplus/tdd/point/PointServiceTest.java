package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PointServiceTest {
    private static final long EXISTED_USER_ID = 1L;
    private static final long NOT_EXISTED_USER_ID = 99L;
    private static final long POINT_3000 = 3000L;
    private static final long POINT_0 = 0L;
    private static final long SYSTEM_CURRENT_TIME_MILLIS = 3000L;

    private UserPoint userPoint;

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    @BeforeEach
    public void setup() {
        userPoint = new UserPoint(EXISTED_USER_ID, POINT_3000, SYSTEM_CURRENT_TIME_MILLIS);

        userPointTable.insertOrUpdate(EXISTED_USER_ID, POINT_3000);
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
}
