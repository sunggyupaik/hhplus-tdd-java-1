package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.common.exception.ExceededBalanceException;
import io.hhplus.tdd.common.exception.InsufficientBalanceException;
import io.hhplus.tdd.controller.PointController;
import io.hhplus.tdd.domain.point.PointHistory;
import io.hhplus.tdd.domain.point.TransactionType;
import io.hhplus.tdd.domain.point.UserPoint;
import io.hhplus.tdd.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PointController.class)
public class PointControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    @Autowired
    private ObjectMapper objectMapper;

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
    private static final TransactionType TRANSACTION_TYPE_CHARGE = TransactionType.CHARGE;

    private UserPoint userPoint;
    private UserPoint chargedUserPoint;
    private UserPoint usedUserPoint;
    private UserPoint emptyUserPoint;
    private PointHistory pointHistory_1;
    private PointHistory pointHistory_2;
    private List<PointHistory> pointHistories;

    @BeforeEach
    void setup() {
        userPoint = new UserPoint(EXISTED_USER_ID, POINT_5000, SYSTEM_CURRENT_TIME_MILLIS);
        chargedUserPoint = new UserPoint(EXISTED_USER_ID, POINT_8000, SYSTEM_CURRENT_TIME_MILLIS);
        usedUserPoint = new UserPoint(EXISTED_USER_ID, POINT_2000, SYSTEM_CURRENT_TIME_MILLIS);
        emptyUserPoint = new UserPoint(NOT_EXISTED_USER_ID, POINT_0, SYSTEM_CURRENT_TIME_MILLIS);

        pointHistory_1 = new PointHistory(EXISTED_POINT_1_ID, EXISTED_USER_ID, AMOUNT_3000,
                TRANSACTION_TYPE_CHARGE, SYSTEM_CURRENT_TIME_MILLIS);
        pointHistory_2 = new PointHistory(EXISTED_POINT_2_ID, EXISTED_USER_ID, AMOUNT_5000,
                TRANSACTION_TYPE_CHARGE, SYSTEM_CURRENT_TIME_MILLIS);
        pointHistories = List.of(pointHistory_1, pointHistory_2);
    }

    @Nested
    @DisplayName("point 메서드는")
    class Describe_point {
        @Nested
        @DisplayName("존재하는 유저 식별자가 주어진다면")
        class Context_WithExistedUserId {
            private final Long EXISTED_ID = EXISTED_USER_ID;

            @Test
            @DisplayName("해당 유저 포인트를 반환한다")
            void itReturnsUserPoint() throws Exception {
                given(pointService.detailUserPoint(EXISTED_ID)).willReturn(userPoint);

                mockMvc.perform(
                        get("/point/{id}", EXISTED_ID)
                                .accept(MediaType.APPLICATION_JSON)
                )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(EXISTED_ID));
            }
        }

        @Nested
        @DisplayName("존재하지 않는 유저 식별자가 주어진다면")
        class Context_WithNotExistedUserId {
            private final Long NOT_EXISTED_ID = NOT_EXISTED_USER_ID;

            @Test
            @DisplayName("포인트가 0인 빈 유저 포인트를 반환한다")
            void itReturnsUserPoint() throws Exception {
                given(pointService.detailUserPoint(NOT_EXISTED_ID)).willReturn(emptyUserPoint);

                mockMvc.perform(
                                get("/point/{id}", NOT_EXISTED_ID)
                                        .accept(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(NOT_EXISTED_ID))
                        .andExpect(jsonPath("$.point").value(POINT_0));
            }
        }
    }

    @Nested
    @DisplayName("history 메서드는")
    class Describe_history {
        @Nested
        @DisplayName("존재하는 유저 식별자가 주어진다면")
        class Context_WithExistedUserId {
            private final Long EXISTED_ID = EXISTED_USER_ID;

            @Test
            @DisplayName("해당 포인트 내역을 반환한다")
            void itReturnsPointHistories() throws Exception {
                given(pointService.listsAllPointHistory(EXISTED_ID)).willReturn(pointHistories);

                mockMvc.perform(
                                get("/point/{id}/histories", EXISTED_ID)
                                        .accept(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.[0].id").value(EXISTED_POINT_1_ID))
                        .andExpect(jsonPath("$.[1].id").value(EXISTED_POINT_2_ID));
            }
        }

        @Nested
        @DisplayName("존재하지 않는 유저 식별자가 주어진다면")
        class Context_WithNotExistedUserId {
            private final Long NOT_EXISTED_ID = NOT_EXISTED_USER_ID;

            @Test
            @DisplayName("빈 목록을 반환한다")
            void itReturnsEmpty() throws Exception {
                given(pointService.listsAllPointHistory(NOT_EXISTED_ID)).willReturn(List.of());

                mockMvc.perform(
                                get("/point/{id}/histories", NOT_EXISTED_ID)
                                        .accept(MediaType.APPLICATION_JSON)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().string("[]"));
            }
        }
    }

    @Nested
    @DisplayName("charge 메서드는")
    class Describe_charge {
        @Nested
        @DisplayName("존재하는 유저 식별자와 금액이 주어진다면")
        class Context_WithExistedUserIdAndAmount {
            private final Long EXISTED_ID = EXISTED_USER_ID;
            private final Long AMOUNT = AMOUNT_3000;

            @Test
            @DisplayName("해당하는 유저 포인트를 충전하고 반환한다.")
            void itReturnsChargedUserPoint() throws Exception {
                given(pointService.chargeUserPoint(EXISTED_ID, AMOUNT)).willReturn(chargedUserPoint);

                mockMvc.perform(
                                patch("/point/{id}/charge", EXISTED_ID)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .characterEncoding("UTF-8")
                                        .content(objectMapper.writeValueAsBytes(AMOUNT))
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(EXISTED_ID))
                        .andExpect(jsonPath("$.point").value(POINT_8000));
            }
        }

        @Nested
        @DisplayName("존재하는 유저 식별자와 잔고 최대를 초과하는 금액이 주어진다면")
        class Context_WithExistedUserIdAndAmountThatMakesExceed {
            private final Long EXISTED_ID = EXISTED_USER_ID;
            private final Long AMOUNT = POINT_8000;

            @Test
            @DisplayName("잔고를 초과했다는 예외를 반환한다")
            void itReturnsEmpty() throws Exception {
                given(pointService.chargeUserPoint(EXISTED_ID, AMOUNT)).willThrow(ExceededBalanceException.class);

                mockMvc.perform(
                                patch("/point/{id}/charge", EXISTED_ID)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .characterEncoding("UTF-8")
                                        .content(objectMapper.writeValueAsBytes(AMOUNT))
                        )
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("use 메서드는")
    class Describe_use {
        @Nested
        @DisplayName("존재하는 유저 식별자와 금액이 주어진다면")
        class Context_WithExistedUserIdAndAmount {
            private final Long EXISTED_ID = EXISTED_USER_ID;
            private final Long AMOUNT = AMOUNT_3000;

            @Test
            @DisplayName("해당하는 유저 포인트를 사용하고 반환한다.")
            void itReturnsUsedUserPoint() throws Exception {
                given(pointService.chargeUserPoint(EXISTED_ID, AMOUNT)).willReturn(usedUserPoint);

                mockMvc.perform(
                                patch("/point/{id}/charge", EXISTED_ID)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .characterEncoding("UTF-8")
                                        .content(objectMapper.writeValueAsBytes(AMOUNT))
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(EXISTED_ID))
                        .andExpect(jsonPath("$.point").value(POINT_2000));
            }
        }

        @Nested
        @DisplayName("존재하는 유저 식별자와 잔고가 음수가 되는 금액이 주어진다면")
        class Context_WithExistedUserIdAndAmountThatMakesInsufficient {
            private final Long EXISTED_ID = EXISTED_USER_ID;
            private final Long AMOUNT = POINT_8000;

            @Test
            @DisplayName("잔고가 부족하다는 예외를 반환한다")
            void itReturnsEmpty() throws Exception {
                given(pointService.chargeUserPoint(EXISTED_ID, AMOUNT)).willThrow(InsufficientBalanceException.class);

                mockMvc.perform(
                                patch("/point/{id}/charge", EXISTED_ID)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .characterEncoding("UTF-8")
                                        .content(objectMapper.writeValueAsBytes(AMOUNT))
                        )
                        .andExpect(status().isBadRequest());
            }
        }
    }
}
