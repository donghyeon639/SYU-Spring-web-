package com.SYUcap.SYUcap.Group;

import com.SYUcap.SYUcap.Board.Board;
import com.SYUcap.SYUcap.Board.BoardRepository;
import com.SYUcap.SYUcap.GroupMember.GroupMembers;
import com.SYUcap.SYUcap.GroupMember.GroupMembersRepository;
import com.SYUcap.SYUcap.JoinRequest.JoinRequestService;
import com.SYUcap.SYUcap.JoinRequest.JoinRequests;
import com.SYUcap.SYUcap.JoinRequest.JoinRequestsRepository;
import com.SYUcap.SYUcap.User.Users;
import com.SYUcap.SYUcap.User.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.Assertions.*;


@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class GroupIntegrationTest {

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
    private JoinRequestService joinRequestService;

    @Autowired
    private JoinRequestsRepository joinRequestsRepository;

    @Autowired
    private EntityManager entityManager;

    private Users testUser;
    private Board testBoard;

    @BeforeEach
    void setUp() {
        // 이전 테스트 데이터 정리 (유저/게시글/그룹 관련 엔티티 순으로 삭제)
        // 그룹 연관 관계를 고려해 순서 주의: 그룹 멤버/가입요청 -> 그룹 -> 게시글 -> 유저
        joinRequestsRepository.deleteAll();
        groupMembersRepository.deleteAll();
        groupsRepository.deleteAll();
        boardRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트용 사용자 생성 - userId를 매 테스트마다 유일하게 설정
        long userCount = userRepository.count();
        testUser = new Users();
        testUser.setUserId("testuser_" + userCount);
        testUser.setPassword("password123!");
        testUser.setUserName("테스트유저");
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("TC-G-001: 게시글 작성 시 그룹 자동 생성")
    void testCreateGroupFromBoard() {
        // Given: 게시글 데이터 준비
        testBoard = new Board();
        testBoard.setTitle("스터디 모집");
        testBoard.setContent("같이 공부할 사람 구합니다");
        testBoard.setLimitCount(5);
        testBoard.setUser(testUser);
        testBoard = boardRepository.save(testBoard);

        // When: 그룹 자동 생성
        Groups createdGroup = groupService.createGroupFromBoard(testBoard);

        // Then: 검증
        // 1. Groups가 생성되었는가?
        assertThat(createdGroup).isNotNull();
        assertThat(createdGroup.getId()).isNotNull();

        // 2. 그룹 정보가 올바른가?
        assertThat(createdGroup.getCurrentCount()).isEqualTo(1);
        assertThat(createdGroup.getStatus()).isEqualTo("ACTIVE");
        assertThat(createdGroup.getBoard().getId()).isEqualTo(testBoard.getId());

        // 3. 그룹장(LEADER)이 자동으로 등록되었는가?
        Optional<GroupMembers> leaderOpt = groupMembersRepository
                .findAll()
                .stream()
                .filter(gm -> gm.getGroup().getId().equals(createdGroup.getId())
                        && gm.getUser().getId().equals(testUser.getId()))
                .findFirst();

        assertThat(leaderOpt).isPresent();
        GroupMembers leader = leaderOpt.get();
        assertThat(leader.getRole()).isEqualTo("LEADER");
        assertThat(leader.getUser().getId()).isEqualTo(testUser.getId());

        // 4. DB에 실제로 저장되었는가?
        Optional<Groups> savedGroup = groupsRepository.findById(createdGroup.getId());
        assertThat(savedGroup).isPresent();
    }

    @Test
    @DisplayName("TC-G-003: 제한 인원 도달 시 자동 마감")
    void testGroupAutoClose() {
        // Given: 제한 인원 5명인 그룹 생성
        testBoard = new Board();
        testBoard.setTitle("스터디 모집");
        testBoard.setContent("같이 공부할 사람 구합니다");
        testBoard.setLimitCount(5);
        testBoard.setUser(testUser);
        testBoard = boardRepository.save(testBoard);

        Groups group = groupService.createGroupFromBoard(testBoard);

        // 초기 상태 확인
        assertThat(group.getCurrentCount()).isEqualTo(1);
        assertThat(group.getStatus()).isEqualTo("ACTIVE");

        // When: 4명의 멤버를 추가 (총 5명이 되도록)
        for (int i = 1; i <= 4; i++) {
            Users newUser = new Users();
            newUser.setUserId("user" + i);
            newUser.setPassword("password123!");
            newUser.setUserName("사용자" + i);
            newUser = userRepository.save(newUser);

            GroupMembers member = new GroupMembers(newUser, group, "MEMBER");
            groupMembersRepository.save(member);

            group.incrementCurrentCount();
        }
        groupsRepository.save(group);

        // Then: 검증
        // 1. 현재 인원이 5명인가?
        Groups updatedGroup = groupsRepository.findById(group.getId()).orElse(null);
        assertThat(updatedGroup).isNotNull();
        assertThat(updatedGroup.getCurrentCount()).isEqualTo(5);

        // 2. 그룹 상태가 자동으로 CLOSED로 변경되었는가?
        assertThat(updatedGroup.getStatus()).isEqualTo("CLOSED");

        // 3. isFull() 메서드가 true를 반환하는가?
        assertThat(updatedGroup.isFull()).isTrue();
    }

    @Test
    @DisplayName("TC-G-005: 그룹장의 멤버 강제 퇴장")
    void testKickMember() {
        // Given: 그룹과 일반 멤버 준비
        testBoard = new Board();
        testBoard.setTitle("스터디 모집");
        testBoard.setContent("같이 공부할 사람 구합니다");
        testBoard.setLimitCount(5);
        testBoard.setUser(testUser);
        testBoard = boardRepository.save(testBoard);

        Groups group = groupService.createGroupFromBoard(testBoard);

        // 일반 멤버 추가
        Users member = new Users();
        member.setUserId("member1");
        member.setPassword("password123!");
        member.setUserName("일반멤버");
        member = userRepository.save(member);

        GroupMembers groupMember = new GroupMembers(member, group, "MEMBER");
        groupMembersRepository.save(groupMember);

        group.incrementCurrentCount();
        groupsRepository.save(group);

        // 초기 상태 확인
        assertThat(group.getCurrentCount()).isEqualTo(2); // 그룹장 + 멤버 1명

        // When: 그룹장이 멤버 강제 퇴장
        groupService.kickMember(member.id, group.getId(), testUser.id);

        // Then: 검증
        // 1. GroupMembers에서 삭제되었는가?
        Optional<GroupMembers> kickedMember = groupMembersRepository
                .findByUserIdAndGroupId(member.id, group.getId());
        assertThat(kickedMember).isEmpty();

        // 2. currentCount가 감소했는가?
        Groups updatedGroup = groupsRepository.findById(group.getId()).orElse(null);
        assertThat(updatedGroup).isNotNull();
        assertThat(updatedGroup.getCurrentCount()).isEqualTo(1); // 그룹장만 남음
    }

    @Test
    @DisplayName("TC-G-006: 그룹장 권한 위임")
    void testTransferLeadership() {
        // Given: 그룹과 일반 멤버 준비
        testBoard = new Board();
        testBoard.setTitle("스터디 모집");
        testBoard.setContent("같이 공부할 사람 구합니다");
        testBoard.setLimitCount(5);
        testBoard.setUser(testUser);
        testBoard = boardRepository.save(testBoard);

        Groups group = groupService.createGroupFromBoard(testBoard);

        // 일반 멤버 추가
        Users newLeader = new Users();
        newLeader.setUserId("newleader");
        newLeader.setPassword("password123!");
        newLeader.setUserName("신규리더");
        newLeader = userRepository.save(newLeader);

        GroupMembers newLeaderMember = new GroupMembers(newLeader, group, "MEMBER");
        groupMembersRepository.save(newLeaderMember);

        // When: 그룹장 권한 위임
        groupService.transferLeadership(testUser.id, newLeader.id, group.getId());

        // Then: 검증
        // 1. 기존 그룹장이 MEMBER로 변경되었는가?
        Optional<GroupMembers> oldLeaderOpt = groupMembersRepository
                .findByUserIdAndGroupId(testUser.id, group.getId());
        assertThat(oldLeaderOpt).isPresent();
        assertThat(oldLeaderOpt.get().getRole()).isEqualTo("MEMBER");

        // 2. 새 멤버가 LEADER로 변경되었는가?
        Optional<GroupMembers> newLeaderOpt = groupMembersRepository
                .findByUserIdAndGroupId(newLeader.id, group.getId());
        assertThat(newLeaderOpt).isPresent();
        assertThat(newLeaderOpt.get().getRole()).isEqualTo("LEADER");
    }

    @Test
    @DisplayName("TC-G-007: 그룹장이 수동으로 그룹 마감")
    void testCloseGroup() {
        // Given: 그룹과 대기 중인 가입 신청 준비
        testBoard = new Board();
        testBoard.setTitle("스터디 모집");
        testBoard.setContent("같이 공부할 사람 구합니다");
        testBoard.setLimitCount(5);
        testBoard.setUser(testUser);
        testBoard = boardRepository.save(testBoard);

        Groups group = groupService.createGroupFromBoard(testBoard);
        group = groupsRepository.findById(group.getId()).orElseThrow();

        // 대기 중인 가입 신청 3건 추가
        List<Users> applicants = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Users applicant = new Users();
            applicant.setUserId("applicant" + i);
            applicant.setPassword("password123!");
            applicant.setUserName("신청자" + i);
            applicant = userRepository.save(applicant);
            applicants.add(applicant);

            JoinRequests request = new JoinRequests(applicant, group, "가입 신청!");
            joinRequestsRepository.save(request);
        }

        // 초기 상태 확인
        assertThat(group.getStatus()).isEqualTo("ACTIVE");

        // When: 그룹장이 그룹 마감
        groupService.closeGroup(group.getId(), testUser.id);

        // Then: 검증
        // 1. 그룹 상태가 CLOSED로 변경되었는가?
        Groups closedGroup = groupsRepository.findById(group.getId()).orElse(null);
        assertThat(closedGroup).isNotNull();
        assertThat(closedGroup.getStatus()).isEqualTo("CLOSED");

        // 2. 모든 대기 신청이 REJECTED로 변경되었는가?
        List<JoinRequests> rejectedRequests = joinRequestsRepository
                .findByGroupIdAndStatusOrderByRequestedAtDesc(group.getId(), "REJECTED");
        assertThat(rejectedRequests).hasSize(3);

        for (JoinRequests request : rejectedRequests) {
            assertThat(request.getStatus()).isEqualTo("REJECTED");
            assertThat(request.getProcessedAt()).isNotNull();
        }
    }

    @Test
    @DisplayName("TC-G-013: 가입 신청 승인 후 멤버 추가")
    void testApproveJoinRequest() {
        // Given: 그룹과 가입 신청 준비
        testBoard = new Board();
        testBoard.setTitle("스터디 모집");
        testBoard.setContent("같이 공부할 사람 구합니다");
        testBoard.setLimitCount(5);
        testBoard.setUser(testUser);
        testBoard = boardRepository.save(testBoard);

        Groups group = groupService.createGroupFromBoard(testBoard);
        group = groupsRepository.findById(group.getId()).orElseThrow();

        // 가입 신청자 생성
        Users applicant = new Users();
        applicant.setUserId("applicant");
        applicant.setPassword("password123!");
        applicant.setUserName("신청자");
        applicant = userRepository.save(applicant);

        // 가입 신청 생성
        JoinRequests joinRequest = new JoinRequests(applicant, group, "가입하고 싶습니다!");
        joinRequest = joinRequestsRepository.save(joinRequest);

        // 초기 상태 확인
        assertThat(joinRequest.getStatus()).isEqualTo("PENDING");
        assertThat(group.getCurrentCount()).isEqualTo(1);

        // When: 그룹장이 승인
        joinRequestService.approveJoinRequest(joinRequest.getId(), testUser.id);

        // Then: 검증
        // 1. 신청 상태가 APPROVED로 변경되었는가?
        JoinRequests approvedRequest = joinRequestsRepository.findById(joinRequest.getId()).orElse(null);
        assertThat(approvedRequest).isNotNull();
        assertThat(approvedRequest.getStatus()).isEqualTo("APPROVED");
        assertThat(approvedRequest.getProcessedBy().id).isEqualTo(testUser.id);

        // 2. GroupMembers에 추가되었는가?
        Optional<GroupMembers> newMemberOpt = groupMembersRepository
                .findByUserIdAndGroupId(applicant.id, group.getId());
        assertThat(newMemberOpt).isPresent();
        assertThat(newMemberOpt.get().getRole()).isEqualTo("MEMBER");

        // 3. currentCount가 증가했는가?
        Groups updatedGroup = groupsRepository.findById(group.getId()).orElse(null);
        assertThat(updatedGroup).isNotNull();
        assertThat(updatedGroup.getCurrentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-G-014: 가입 신청 거절")
    void testRejectJoinRequest() {
        // Given: 그룹과 가입 신청 준비
        testBoard = new Board();
        testBoard.setTitle("스터디 모집");
        testBoard.setContent("같이 공부할 사람 구합니다");
        testBoard.setLimitCount(5);
        testBoard.setUser(testUser);
        testBoard = boardRepository.save(testBoard);

        Groups group = groupService.createGroupFromBoard(testBoard);
        group = groupsRepository.findById(group.getId()).orElseThrow();

        // 가입 신청자 생성
        Users applicant = new Users();
        applicant.setUserId("applicant");
        applicant.setPassword("password123!");
        applicant.setUserName("신청자");
        applicant = userRepository.save(applicant);

        // 가입 신청 생성
        JoinRequests joinRequest = new JoinRequests(applicant, group, "가입하고 싶습니다!");
        joinRequest = joinRequestsRepository.save(joinRequest);

        // 초기 상태 확인
        assertThat(joinRequest.getStatus()).isEqualTo("PENDING");
        assertThat(group.getCurrentCount()).isEqualTo(1);

        // When: 그룹장이 거절
        joinRequestService.rejectJoinRequest(joinRequest.getId(), testUser.id);

        // Then: 검증
        // 1. 신청 상태가 REJECTED로 변경되었는가?
        JoinRequests rejectedRequest = joinRequestsRepository.findById(joinRequest.getId()).orElse(null);
        assertThat(rejectedRequest).isNotNull();
        assertThat(rejectedRequest.getStatus()).isEqualTo("REJECTED");
        assertThat(rejectedRequest.getProcessedBy().id).isEqualTo(testUser.id);

        // 2. GroupMembers에 추가되지 않았는가?
        Optional<GroupMembers> memberOpt = groupMembersRepository
                .findByUserIdAndGroupId(applicant.id, group.getId());
        assertThat(memberOpt).isEmpty();

        // 3. currentCount가 유지되는가?
        Groups updatedGroup = groupsRepository.findById(group.getId()).orElse(null);
        assertThat(updatedGroup).isNotNull();
        assertThat(updatedGroup.getCurrentCount()).isEqualTo(1); // 그룹장만
    }

    @Test
    @DisplayName("TC-G-022: 그룹장이 마감 중 다른 관리자가 신청 승인")
    void testConcurrentCloseAndApprove() throws InterruptedException {
        // Given: 그룹과 가입 신청 준비
        testBoard = new Board();
        testBoard.setTitle("스터디 모집");
        testBoard.setContent("같이 공부할 사람 구합니다");
        testBoard.setLimitCount(5);
        testBoard.setUser(testUser);
        testBoard = boardRepository.save(testBoard);

        Groups group = groupService.createGroupFromBoard(testBoard);
        group = groupsRepository.findById(group.getId()).orElseThrow();
        Long groupId = group.getId();

        // 가입 신청자 생성
        Users applicant = new Users();
        applicant.setUserId("applicant");
        applicant.setPassword("password123!");
        applicant.setUserName("신청자");
        applicant = userRepository.save(applicant);

        // 가입 신청 생성
        JoinRequests joinRequest = new JoinRequests(applicant, group, "가입 신청!");
        joinRequest = joinRequestsRepository.save(joinRequest);
        Long requestId = joinRequest.getId();

        System.out.println("=== 초기 상태 ===");
        System.out.println("그룹 상태: " + group.getStatus());
        System.out.println("신청 상태: " + joinRequest.getStatus());

        // When: 순차적으로 실행 (동시성은 실제 서버 환경에서 테스트)
        // 1. 먼저 마감 시도
        try {
            groupService.closeGroup(groupId, testUser.id);
            System.out.println("그룹 마감 성공");
        } catch (Exception e) {
            System.out.println("그룹 마감 실패: " + e.getMessage());
        }

        // 2. 그 다음 승인 시도 (마감된 그룹에 승인 시도)
        try {
            joinRequestService.approveJoinRequest(requestId, testUser.id);
            System.out.println("신청 승인 성공");
        } catch (Exception e) {
            System.out.println("신청 승인 실패: " + e.getMessage());
        }

        // Then: 검증
        Groups finalGroup = groupsRepository.findById(groupId).orElse(null);
        JoinRequests finalRequest = joinRequestsRepository.findById(requestId).orElse(null);

        System.out.println("\n=== TC-G-022 결과 ===");
        System.out.println("최종 그룹 상태: " + (finalGroup != null ? finalGroup.getStatus() : "null"));
        System.out.println("최종 신청 상태: " + (finalRequest != null ? finalRequest.getStatus() : "null"));

        // 1. 그룹이 CLOSED 상태인가?
        assertThat(finalGroup).isNotNull();
        assertThat(finalGroup.getStatus()).isEqualTo("CLOSED");

        // 2. 대기 신청이 REJECTED로 변경되었는가?
        assertThat(finalRequest).isNotNull();
        assertThat(finalRequest.getStatus()).isEqualTo("REJECTED");

        // 3. 승인 시도는 실패해야 함 (이미 처리된 신청)
        System.out.println("마감 후 대기 신청이 자동으로 거절됨 - 정상 동작");
    }
}