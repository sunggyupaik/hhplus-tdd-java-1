package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.domain.point.PointHistory;
import io.hhplus.tdd.domain.point.UserPoint;
import io.hhplus.tdd.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PointConcurrencyTest {
    private final PointService pointService;
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointConcurrencyTest(@Autowired PointService pointService,
                            @Autowired UserPointTable userPointTable,
                            @Autowired PointHistoryTable pointHistoryTable) {
        this.pointService = pointService;
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    private static final long EXISTED_USER_ID = 10L;

    @Test
    @DisplayName("동일한 사용자에 10번의 포인트 충전을 동시에 실행하면 10번 충전된다.")
    void concurrentChargePointForSameUser10Times() throws InterruptedException {
        final int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<Long> amounts = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        final long amountSum = amounts.stream().reduce(0L, Long::sum);
        UserPoint initUserPoint = pointService.detailUserPoint(EXISTED_USER_ID);

        for (int i = 0; i < threadCount; i++) {
            int index = i;
            executorService.submit(() -> {
                try {
                    pointService.chargeUserPoint(EXISTED_USER_ID, amounts.get(index));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        UserPoint chargedUserPoint = userPointTable.selectById(EXISTED_USER_ID);
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(EXISTED_USER_ID);

        assertThat(chargedUserPoint.point()).isEqualTo(initUserPoint.point() + amountSum);
        assertThat(pointHistories.size()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("동일한 사용자에 10번의 포인트 사용을 동시에 실행하면 10번 사용된다.")
    void concurrentUsePointForSameUser10Times() {
        final int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        List<Long> amounts = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        final long amountSum = amounts.stream().reduce(0L, Long::sum);
        UserPoint initUserPoint = pointService.detailUserPoint(EXISTED_USER_ID);

        List<CompletableFuture<Long>> amountsFutures = amounts.stream()
                .map(amount -> CompletableFuture.supplyAsync(
                        () -> pointService.useUserPoint(EXISTED_USER_ID, amount).point(), executorService)
                )
                .toList();

        CompletableFuture.allOf(amountsFutures.toArray(new CompletableFuture[0]))
                .thenApply(Void -> amountsFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()))
                .join();

        UserPoint usedUserPoint = userPointTable.selectById(EXISTED_USER_ID);
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(EXISTED_USER_ID);

        assertThat(usedUserPoint.point()).isEqualTo(initUserPoint.point() - amountSum);
        assertThat(pointHistories.size()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("동일한 사용자에 10번의 동일한 포인트 충전과 사용을 동시에 실행하면 원금을 유지한다.")
    void concurrentChargeAndUsePointForSameUser10Times() {
        final int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        List<Long> amounts = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        UserPoint initUserPoint = pointService.detailUserPoint(EXISTED_USER_ID);

        List<CompletableFuture<Long>> amountsFutures = amounts.stream()
                .map(amount -> CompletableFuture.supplyAsync(
                        () -> {
                            pointService.chargeUserPoint(EXISTED_USER_ID, amount);
                            UserPoint userPoint = pointService.useUserPoint(EXISTED_USER_ID, amount);

                            return userPoint.point();
                        }, executorService)
                )
                .toList();

        CompletableFuture.allOf(amountsFutures.toArray(new CompletableFuture[0]))
                .thenApply(Void -> amountsFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()))
                .join();

        UserPoint resultUserPoint = userPointTable.selectById(EXISTED_USER_ID);
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(EXISTED_USER_ID);

        assertThat(resultUserPoint.point()).isEqualTo(initUserPoint.point());
        assertThat(pointHistories.size()).isEqualTo(threadCount * 2);
    }
}
