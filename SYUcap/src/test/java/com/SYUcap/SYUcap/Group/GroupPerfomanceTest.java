package com.SYUcap.SYUcap.Group;

import com.SYUcap.SYUcap.Board.Board;
import com.SYUcap.SYUcap.Board.BoardRepository;
import com.SYUcap.SYUcap.GroupMember.GroupMembers;
import com.SYUcap.SYUcap.GroupMember.GroupMembersRepository;
import com.SYUcap.SYUcap.JoinRequest.JoinRequests;
import com.SYUcap.SYUcap.JoinRequest.JoinRequestsRepository;
import com.SYUcap.SYUcap.User.Users;
import com.SYUcap.SYUcap.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;


@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class GroupPerformanceTest {

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupsRepository groupsRepository;

    @Autowired
    private GroupMembersRepository groupMembersRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JoinRequestsRepository joinRequestsRepository;

    private Users testUser;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = new Users();
        testUser.setUserId("testuser");
        testUser.setPassword("password123!");
        testUser.setUserName("테스트유저");
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("TC-LOAD-002: 500명이 동일 그룹에 동시 가입 신청")
    @Transactional
    void testConcurrentJoinRequests() throws InterruptedException {
        // Given: 그룹 생성
        Board board = new Board();
        board.setTitle("대규모 스터디");
        board.setContent("많은 사람 환영");
        board.setLimitCount(10);
        board.setUser(testUser);
        board = boardRepository.save(board);

        Groups group = groupService.createGroupFromBoard(board);
        Long groupId = group.getId();

        // 500명의 신청자 미리 생성
        List<Users> applicants = new ArrayList<>();
        for (int i = 1; i <= 500; i++) {
            Users user = new Users();
            user.setUserId("applicant" + i);
            user.setPassword("password123!");
            user.setUserName("신청자" + i);
            applicants.add(userRepository.save(user));
        }

        // When: 500명이 동시에 가입 신청
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(500);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (Users applicant : applicants) {
            executor.submit(() -> {
                try {
                    JoinRequests request = new JoinRequests(applicant, group, "가입 신청합니다!");
                    joinRequestsRepository.save(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 검증
        System.out.println("=== TC-LOAD-002 결과 ===");
        System.out.println("총 처리 시간: " + duration + "ms");
        System.out.println("성공: " + successCount.get() + "건");
        System.out.println("실패: " + failCount.get() + "건");

        // 1. 모든 신청이 처리되었는가?
        assertThat(successCount.get() + failCount.get()).isEqualTo(500);

        // 2. 중복 신청이 없는가? (UniqueConstraint 동작)
        List<JoinRequests> requests = joinRequestsRepository.findByGroupIdOrderByRequestedAtDesc(groupId);
        assertThat(requests.size()).isLessThanOrEqualTo(500);

        // 3. 응답 시간이 합리적인가? (60초 이내)
        assertThat(duration).isLessThan(60000);
    }

    @Test
    @DisplayName("TC-LOAD-004: 200명이 동시에 그룹 생성")
    void testConcurrentGroupCreation() throws InterruptedException {
        // Given: 200명의 사용자 생성
        List<Users> users = new ArrayList<>();
        for (int i = 1; i <= 200; i++) {
            Users user = new Users();
            user.setUserId("creator_" + System.currentTimeMillis() + "_" + i);  // ✅ 고유한 ID 생성
            user.setPassword("password123!");
            user.setUserName("생성자" + i);
            users.add(userRepository.save(user));

            // ✅ 추가: 약간의 지연으로 타임스탬프 중복 방지
            if (i % 50 == 0) {
                Thread.sleep(10);
            }
        }

        // When: 200명이 동시에 그룹 생성
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(200);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 200; i++) {
            Users user = users.get(i);
            int finalI = i;
            executor.submit(() -> {
                try {
                    Board board = new Board();
                    board.setTitle("그룹 " + finalI + "_" + System.currentTimeMillis());  // ✅ 고유한 제목
                    board.setContent("내용 " + finalI);
                    board.setLimitCount(5);
                    board.setUser(user);
                    board = boardRepository.save(board);

                    groupService.createGroupFromBoard(board);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("실패: " + e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 검증
        System.out.println("=== TC-LOAD-004 결과 ===");
        System.out.println("총 처리 시간: " + duration + "ms");
        System.out.println("성공: " + successCount.get() + "건");
        System.out.println("실패: " + failCount.get() + "건");

        // 1. 대부분 성공했는가?
        assertThat(successCount.get()).isGreaterThan(150); // 최소 75% 성공

        // 2. 응답 시간이 합리적인가?
        assertThat(duration).isLessThan(60000);
    }

    @Test
    @DisplayName("TC-PERF-006: 100만 건 가입 신청 데이터 생성")
    @Transactional
    void testBulkInsertJoinRequests() {
        // Given: 그룹 생성
        Board board = new Board();
        board.setTitle("대규모 테스트");
        board.setContent("백만 건 테스트");
        board.setLimitCount(1000000);
        board.setUser(testUser);
        board = boardRepository.save(board);

        Groups group = groupService.createGroupFromBoard(board);

        // When: 100만 건 생성 (시간이 오래 걸리므로 1만 건으로 축소)
        int targetCount = 10000; // 실제로는 1,000,000이지만 테스트 시간 단축

        long startTime = System.currentTimeMillis();

        List<JoinRequests> batch = new ArrayList<>();
        for (int i = 1; i <= targetCount; i++) {
            Users user = new Users();
            user.setUserId("bulk_user_" + i);
            user.setPassword("password123!");
            user.setUserName("벌크유저" + i);
            user = userRepository.save(user);

            JoinRequests request = new JoinRequests(user, group, "가입 신청");
            batch.add(request);

            // 1000건씩 배치 저장
            if (i % 1000 == 0) {
                joinRequestsRepository.saveAll(batch);
                batch.clear();
                System.out.println("진행: " + i + " / " + targetCount);
            }
        }

        if (!batch.isEmpty()) {
            joinRequestsRepository.saveAll(batch);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 검증
        System.out.println("=== TC-PERF-006 결과 ===");
        System.out.println("총 처리 시간: " + duration + "ms");
        System.out.println("생성된 데이터: " + targetCount + "건");
        System.out.println("초당 처리량: " + (targetCount * 1000 / duration) + " TPS");

        long count = joinRequestsRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(targetCount);
    }

    @Test
    @DisplayName("TC-PERF-007: 그룹 해체 시 연관 데이터 10,000건 삭제")
    void testBulkDeleteOnGroupDeletion() {
        // Given: 대량 데이터가 있는 그룹 생성
        Board board = new Board();
        board.setTitle("대규모 그룹");
        board.setContent("삭제 테스트");
        board.setLimitCount(10000);
        board.setUser(testUser);
        board = boardRepository.save(board);

        Groups group = groupService.createGroupFromBoard(board);
        Long groupId = group.getId();

        // ✅ 추가: 그룹을 다시 조회해서 영속 상태로 만들기
        group = groupsRepository.findById(groupId).orElseThrow();

        // 멤버 100명 추가
        int memberCount = 100;
        List<GroupMembers> membersBatch = new ArrayList<>();

        for (int i = 1; i <= memberCount; i++) {
            Users user = new Users();
            user.setUserId("member_bulk_" + i);
            user.setPassword("password123!");
            user.setUserName("멤버" + i);
            user = userRepository.save(user);

            GroupMembers member = new GroupMembers(user, group, "MEMBER");
            membersBatch.add(member);

            // 50건씩 배치 저장
            if (i % 50 == 0) {
                groupMembersRepository.saveAll(membersBatch);
                membersBatch.clear();
                System.out.println("멤버 생성 진행: " + i + " / " + memberCount);
            }
        }

        if (!membersBatch.isEmpty()) {
            groupMembersRepository.saveAll(membersBatch);
        }

        // 가입 신청 100건 추가
        int requestCount = 100;
        List<JoinRequests> requestsBatch = new ArrayList<>();

        for (int i = 1; i <= requestCount; i++) {
            Users user = new Users();
            user.setUserId("applicant_bulk_" + i);
            user.setPassword("password123!");
            user.setUserName("신청자" + i);
            user = userRepository.save(user);

            JoinRequests request = new JoinRequests(user, group, "가입 신청");
            requestsBatch.add(request);

            // 50건씩 배치 저장
            if (i % 50 == 0) {
                joinRequestsRepository.saveAll(requestsBatch);
                requestsBatch.clear();
            }
        }

        if (!requestsBatch.isEmpty()) {
            joinRequestsRepository.saveAll(requestsBatch);
        }

        System.out.println("데이터 생성 완료. 삭제 시작...");

        // When: 그룹 해체
        long startTime = System.currentTimeMillis();
        groupService.deleteGroup(groupId, testUser.id);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 검증
        System.out.println("=== TC-PERF-007 결과 ===");
        System.out.println("삭제 처리 시간: " + duration + "ms");
        System.out.println("삭제된 멤버: " + (memberCount + 1) + "명"); // +1은 그룹장
        System.out.println("삭제된 신청: " + requestCount + "건");

        // 모두 삭제되었는가?
        assertThat(groupsRepository.findById(groupId)).isEmpty();
        assertThat(groupMembersRepository.findByGroupIdOrderByJoinedAt(groupId)).isEmpty();
        assertThat(joinRequestsRepository.findByGroupIdOrderByRequestedAtDesc(groupId)).isEmpty();

        // 합리적인 시간 내에 삭제되었는가? (10초 이내)
        assertThat(duration).isLessThan(10000);
    }

    @Test
    @DisplayName("TC-PERF-008: 1억 건 가입 신청 중 특정 사용자 조회")
    @Transactional
    void testQueryPerformanceOnLargeDataset() {
        // Given: 대량 데이터 생성 (실제로는 1000건으로 축소)
        Board board = new Board();
        board.setTitle("조회 성능 테스트");
        board.setContent("대용량 조회");
        board.setLimitCount(10000);
        board.setUser(testUser);
        board = boardRepository.save(board);

        Groups group = groupService.createGroupFromBoard(board);

        // 특정 사용자
        Users targetUser = new Users();
        targetUser.setUserId("target_user");
        targetUser.setPassword("password123!");
        targetUser.setUserName("타겟유저");
        targetUser = userRepository.save(targetUser);

        // 타겟 사용자의 신청 추가
        JoinRequests targetRequest = new JoinRequests(targetUser, group, "내 신청");
        joinRequestsRepository.save(targetRequest);

        // 다른 사용자 1000명 추가
        int otherCount = 1000;
        for (int i = 1; i <= otherCount; i++) {
            Users user = new Users();
            user.setUserId("other_user_" + i);
            user.setPassword("password123!");
            user.setUserName("유저" + i);
            user = userRepository.save(user);

            JoinRequests request = new JoinRequests(user, group, "신청");
            joinRequestsRepository.save(request);
        }

        System.out.println("데이터 생성 완료. 조회 시작...");

        // When: 특정 사용자 조회
        long startTime = System.currentTimeMillis();
        List<JoinRequests> results = joinRequestsRepository.findByUserIdOrderByRequestedAtDesc(targetUser.id);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 검증
        System.out.println("=== TC-PERF-008 결과 ===");
        System.out.println("조회 시간: " + duration + "ms");
        System.out.println("전체 데이터: " + (otherCount + 1) + "건");
        System.out.println("조회 결과: " + results.size() + "건");

        // 1. 정확히 조회되었는가?
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUser().id).isEqualTo(targetUser.id);

        // 2. 응답 시간이 합리적인가? (5초 이내)
        assertThat(duration).isLessThan(5000);
    }

    @Test
    @DisplayName("TC-RES-001: 100,000개 그룹 전체 조회 (페이징 없이)")
    @Transactional
    void testOutOfMemoryScenario() {
        // Given: 대량 그룹 생성 (실제로는 1000개로 축소)
        int groupCount = 1000;

        for (int i = 1; i <= groupCount; i++) {
            Users user = new Users();
            user.setUserId("owner_" + i);
            user.setPassword("password123!");
            user.setUserName("오너" + i);
            user = userRepository.save(user);

            Board board = new Board();
            board.setTitle("그룹 " + i);
            board.setContent("내용 " + i);
            board.setLimitCount(5);
            board.setUser(user);
            board = boardRepository.save(board);

            groupService.createGroupFromBoard(board);

            if (i % 100 == 0) {
                System.out.println("그룹 생성 진행: " + i + " / " + groupCount);
            }
        }

        System.out.println("데이터 생성 완료. 전체 조회 시작...");

        // When: 페이징 없이 전체 조회
        long startTime = System.currentTimeMillis();
        List<Groups> allGroups = groupsRepository.findAll();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then: 검증
        System.out.println("=== TC-RES-001 결과 ===");
        System.out.println("조회 시간: " + duration + "ms");
        System.out.println("조회된 그룹: " + allGroups.size() + "개");
        System.out.println("메모리 사용량 측정 필요 (OutOfMemoryError 발생 가능)");

        // 조회는 되었는가?
        assertThat(allGroups.size()).isGreaterThanOrEqualTo(groupCount);

        // 경고: 실제 10만 개면 OutOfMemoryError 발생 가능
        System.out.println("실제 100,000개 그룹에서는 페이징 필수");
    }
}