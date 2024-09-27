# [ 1주차 과제 ] TDD 로 개발하기

---

## 동시성 제어 방식에 대한 분석 및 보고서

---

## 개요
- 동시성 문제를 알아보고 이를 해결하기 위한 다양한 전략을 살펴본다.

<br>

## 동시성 문제의 이해
동시성 문제는 여러 스레드가  동시에 `공유 자원`을 접근할 때 발생하는 문제입니다. 여러 스레드가 동시에 데이터를 읽고 쓰는 과정에서 충돌이 
발생할 수 있어 일관성을 해칠 수 있습니다. 

예를 들어, 은행 계좌에 동시에 출금 요청이 들어올 때, 잔액이 음수가 되는 문제가 발생할 수 있습니다. 
따라서 일관성을 유지하기 위해서 스레드 간에 공유 자원 접근을 조정하는 메커니즘이 필요합니다.

---

## 문제상황: 포인트 충전/사용 동시성 제어하기

포인트 충전 서비스와 테스트는 다음과 같습니다. 만약 10번의 충전을 동시에 요청했을 때, 10번 모두
정상적으로 실행되어 1~10까지 55 포인트가 충전 될 것으로 기대합니다. 


- PointService.java
```java
public UserPoint chargeUserPoint(long id, long amount) {
    UserPoint userPoint = detailUserPoint(id);
    long chargedPoint = userPoint.charge(amount);
    pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
    return userPointTable.insertOrUpdate(id, chargedPoint);
}
```

- PointServiceTest.java
```java
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
```
하지만 기대와 달리, 10 포인트 밖에 충전되지 않았습니다. 포인트 조회 및 쓰기 과정에서 공유 자원 접근 제어가
없었기 때문입니다.

---

## 해결 방안 알아보기



### 어플리케이션 Lock 제어하기

락 메커니즘은 동시성 이슈를 해결하기 위한 대표적인 방법입니다. 공유 자원에 대한 접근을 제어하여 데이터의 일관성을 유지합니다.
락을 통해 한 번에 하나의 스레드만 공유 자원에 접근하는 thread-safe 특성을 가집니다.

### 1. synchronized

```java
public synchronized UserPoint chargeUserPoint(long id, long amount) {
    UserPoint userPoint = detailUserPoint(id);
    long chargedPoint = userPoint.charge(amount);
    pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
    return userPointTable.insertOrUpdate(id, chargedPoint);
}
```
`synchronized`는 한 번의 하나의 스레드만을 사용할 수 있는 것이 가장 큰 장점이지만 여러 스레드가 동시에 실행하지 못하기 때문에 
전체로 보면 성능이 떨어질 수도 있습니다. 특히, `BLOCKED` 상태 스레드는 락이 풀릴 때까지 무한대기하며 공정성이 없습니다.

### 2. ReentrantLock
```java
public synchronized UserPoint chargeUserPoint(long id, long amount) {
    lock.lock();
    try {
        UserPoint userPoint = detailUserPoint(id);
        long chargedPoint = userPoint.charge(amount);
        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
        return userPointTable.insertOrUpdate(id, chargedPoint);
    } finally {
        lock.unlock;
    }
}
```
`ReentrantLock`은 내부에 락과 락을 얻지 못해 대기하는 스레드를 관리하는 큐가 존재합니다. 

- 데드락 해결

`java.util.concurrent` 에서 제공하는 `Lock` 인터페이스와 `ReentrantLock` 구현체는 다양한 기능을 제공합니다. `void unlock()`로 락을
해제할 수 있으며 `boolean tryLock(long time, TimeUnit unit)`로 주어진 시간에 락 획득 여부를 `boolean`으로 반환 할 수도 있습니다.

- 공정성 해결

또한, 락을 요청한 순서대로 스레드를 획득하는 `공정모드(fair)`와 순서를 보장하지 않는 `비공정모드(non-fair)` 2가지를 설정할 수 있습니다.

```java
// 비공정 모드 락
 private final Lock nonFairLock = new ReentrantLock();
 // 공정 모드 락
 private final Lock fairLock = new ReentrantLock(true);
```
객체 생성 시, 매개변수에 `true`를 설정하면 공정 모드 선택이 가능합니다.

### 3. 사용자별 잠금

`ReentrantLock`과 `ConcurrentHashMap`을 합친 경우로, 동일 사용자가 여러번 호출 할 때 동시성 해결을 위해 사용자별로 잠금이 가능합니다.
`ConcurrentHashMap`은 `HashMap`의 대안으로 멀티스레드 환경을 지원하는 고성능 컬렉션입니다.

- PointService.java
```java
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
```
<br>

- UserLock.java
```java
@Component
public class UserLock {
    private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();

    public Lock userLock(Long id) {
        return userLocks.computeIfAbsent(id, key -> new ReentrantLock(true));
    }
}
```
유저 식별자를 key값으로 하며 해당 락 구현체는 `ReentrantLock`를 사용해 동시성을 제어합니다. `userLock`은 key값에 value가 없으면 새로
생성하고, 존재하면 반환합니다. Map 기반으로 `O(1)`의 성능으로 빠르게 관리가 가능합니다.

 이제 영희와 철수가 포인트를 충전하는 경우, 독립적으로 락 경쟁을 하기 때문에 동시에 작업이 가능하며, 영희와 철수 각각 충전을 여러번 하여도 
동시성이 제어되어 최적의 성능을 낼 수 있습니다.

## 이외의 것들

분산환경을 고려하지 않고 어플리케이션 수준에서 락을 구현했는데 위 방법 이외에도 다양한 기술들이 있습니다.

### DB Lock 제어하기

- 낙관적 락

낙관적 락은 데이터를 읽을 때는 락을 걸지 않고, 데이터를 업데이트할 때만 이전 데이터와 현재 데이터를 비교하여 충돌 여부를 판단합니다.

장점은 성능이 좋고 데드락 발생 가능성이 낮으나, 단점은 실패 시 재시도를 구현해야 하고 공정성을 보장하지 않습니다.

- 비관적 락

비관적 락은 데이터를 읽을 때부터 해당 데이터에 대한 락을 걸어 다른 트랜잭션이 해당 데이터를 변경할 수 없게 합니다.

장점은 데이터의 일관성 유지이며, 단점은 성능 저하와 데드락 발생 가능성이 높습니다.
 
### Redis Lock 제어하기

Redis를 사용한다면, 락을 사용할 수 있을 때까지 계속 접근을 시도하는 `SETNX 스핀락`, pub/sub를 활용하는 `분산락`이 있습니다. 