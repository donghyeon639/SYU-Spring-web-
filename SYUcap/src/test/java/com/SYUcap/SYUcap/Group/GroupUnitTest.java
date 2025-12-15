package com.SYUcap.SYUcap.Group;

import com.SYUcap.SYUcap.Board.Board;
import com.SYUcap.SYUcap.Board.BoardRepository;
import com.SYUcap.SYUcap.GroupMember.GroupMembers;
import com.SYUcap.SYUcap.GroupMember.GroupMembersRepository;
import com.SYUcap.SYUcap.JoinRequest.JoinRequestsRepository;
import com.SYUcap.SYUcap.User.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class GroupUnitTest {

    @Mock
    private GroupsRepository groupsRepository;

    @Mock
    private GroupMembersRepository groupMembersRepository;

    @Mock
    private JoinRequestsRepository joinRequestsRepository;

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private GroupService groupService;

    private Users testUser;
    private Board testBoard;
    private Groups testGroup;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        testUser = new Users();
        testUser.setUserId("testuser");
        testUser.setPassword("password123!");
        testUser.setUserName("테스트유저");

        // Mock에서 사용할 수 있도록 id 설정
        try {
            java.lang.reflect.Field idField = Users.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
        } catch (Exception e) {
            // id가 public이면 직접 설정
            testUser.id = 1L;
        }

        testBoard = new Board();
        testBoard.setTitle("스터디 모집");
        testBoard.setContent("같이 공부하실 분");
        testBoard.setLimitCount(5);
        testBoard.setUser(testUser);

        testGroup = new Groups(testBoard);
    }

    @Test
    @DisplayName("단위 테스트: createGroupFromBoard - 그룹 생성 로직")
    void testCreateGroupFromBoard() {
        // Given: Mock 설정
        when(groupsRepository.save(any(Groups.class))).thenAnswer(invocation -> {
            Groups group = invocation.getArgument(0);
            // ID 설정 (실제 DB처럼)
            try {
                java.lang.reflect.Field idField = Groups.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(group, 1L);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return group;
        });

        when(groupMembersRepository.save(any(GroupMembers.class))).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        // When: 그룹 생성
        Groups createdGroup = groupService.createGroupFromBoard(testBoard);

        // Then: 검증
        // 1. groupsRepository.save()가 호출되었는가?
        verify(groupsRepository, times(1)).save(any(Groups.class));

        // 2. groupMembersRepository.save()가 호출되었는가? (그룹장 추가)
        verify(groupMembersRepository, times(1)).save(any(GroupMembers.class));

        // 3. 생성된 그룹이 null이 아닌가?
        assertThat(createdGroup).isNotNull();
        assertThat(createdGroup.getCurrentCount()).isEqualTo(1);
        assertThat(createdGroup.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("단위 테스트: isLeader - 그룹장 권한 확인")
    void testIsLeader() {
        // Given: Mock 설정
        Long userId = 1L;
        Long groupId = 1L;

        GroupMembers leaderMember = new GroupMembers(testUser, testGroup, "LEADER");
        when(groupMembersRepository.findByUserIdAndGroupId(userId, groupId))
                .thenReturn(Optional.of(leaderMember));

        // When: 그룹장 확인
        boolean isLeader = groupService.isLeader(userId, groupId);

        // Then: 검증
        assertThat(isLeader).isTrue();
        verify(groupMembersRepository, times(1)).findByUserIdAndGroupId(userId, groupId);
    }

    @Test
    @DisplayName("단위 테스트: isLeader - 일반 멤버는 그룹장 아님")
    void testIsNotLeader() {
        // Given: Mock 설정
        Long userId = 2L;
        Long groupId = 1L;

        Users normalUser = new Users();
        normalUser.id = 2L;
        normalUser.setUserId("normaluser");

        GroupMembers normalMember = new GroupMembers(normalUser, testGroup, "MEMBER");
        when(groupMembersRepository.findByUserIdAndGroupId(userId, groupId))
                .thenReturn(Optional.of(normalMember));

        // When: 그룹장 확인
        boolean isLeader = groupService.isLeader(userId, groupId);

        // Then: 검증
        assertThat(isLeader).isFalse();
        verify(groupMembersRepository, times(1)).findByUserIdAndGroupId(userId, groupId);
    }

    @Test
    @DisplayName("단위 테스트: leaveGroup - 일반 멤버 탈퇴")
    void testLeaveGroup() {
        // Given: Mock 설정
        Long userId = 2L;
        Long groupId = 1L;

        Users normalUser = new Users();
        normalUser.id = 2L;

        GroupMembers normalMember = new GroupMembers(normalUser, testGroup, "MEMBER");

        when(groupMembersRepository.findByUserIdAndGroupId(userId, groupId))
                .thenReturn(Optional.of(normalMember));

        doNothing().when(groupMembersRepository).delete(any(GroupMembers.class));
        when(groupsRepository.save(any(Groups.class))).thenReturn(testGroup);

        // When: 탈퇴 실행
        groupService.leaveGroup(userId, groupId);

        // Then: 검증
        // 1. groupMembersRepository.delete()가 호출되었는가?
        verify(groupMembersRepository, times(1)).delete(normalMember);

        // 2. groupsRepository.save()가 호출되었는가? (currentCount 감소)
        verify(groupsRepository, times(1)).save(testGroup);
    }

    @Test
    @DisplayName("단위 테스트: leaveGroup - 그룹장은 탈퇴 불가")
    void testLeaveGroupAsLeader() {
        // Given: Mock 설정
        Long userId = 1L;
        Long groupId = 1L;

        GroupMembers leaderMember = new GroupMembers(testUser, testGroup, "LEADER");

        when(groupMembersRepository.findByUserIdAndGroupId(userId, groupId))
                .thenReturn(Optional.of(leaderMember));

        // When & Then: 예외 발생 확인
        assertThatThrownBy(() -> groupService.leaveGroup(userId, groupId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("그룹장은 탈퇴할 수 없습니다");

        // 삭제 메서드가 호출되지 않았는지 확인
        verify(groupMembersRepository, never()).delete(any(GroupMembers.class));
    }

    @Test
    @DisplayName("단위 테스트: kickMember - 그룹장이 멤버 강제 퇴장")
    void testKickMember() {
        // Given: Mock 설정
        Long targetUserId = 2L;
        Long groupId = 1L;
        Long leaderId = 1L;

        Users targetUser = new Users();
        targetUser.id = 2L;

        // 그룹장 권한 확인
        GroupMembers leaderMember = new GroupMembers(testUser, testGroup, "LEADER");
        when(groupMembersRepository.findByUserIdAndGroupId(leaderId, groupId))
                .thenReturn(Optional.of(leaderMember));

        // 퇴장 대상 멤버
        GroupMembers targetMember = new GroupMembers(targetUser, testGroup, "MEMBER");
        when(groupMembersRepository.findByUserIdAndGroupId(targetUserId, groupId))
                .thenReturn(Optional.of(targetMember));

        doNothing().when(groupMembersRepository).delete(any(GroupMembers.class));
        when(groupsRepository.save(any(Groups.class))).thenReturn(testGroup);

        // When: 강제 퇴장
        groupService.kickMember(targetUserId, groupId, leaderId);

        // Then: 검증
        verify(groupMembersRepository, times(1)).delete(targetMember);
        verify(groupsRepository, times(1)).save(testGroup);
    }

    @Test
    @DisplayName("단위 테스트: kickMember - 일반 멤버는 강제 퇴장 불가")
    void testKickMemberWithoutPermission() {
        // Given: Mock 설정
        Long targetUserId = 2L;
        Long groupId = 1L;
        Long normalUserId = 3L;

        Users normalUser = new Users();
        normalUser.id = 3L;

        // 일반 멤버 (그룹장 아님)
        GroupMembers normalMember = new GroupMembers(normalUser, testGroup, "MEMBER");
        when(groupMembersRepository.findByUserIdAndGroupId(normalUserId, groupId))
                .thenReturn(Optional.of(normalMember));

        // When & Then: 예외 발생 확인
        assertThatThrownBy(() -> groupService.kickMember(targetUserId, groupId, normalUserId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("그룹장만 멤버를 내보낼 수 있습니다");

        verify(groupMembersRepository, never()).delete(any(GroupMembers.class));
    }

    @Test
    @DisplayName("단위 테스트: transferLeadership - 그룹장 권한 위임")
    void testTransferLeadership() {
        // Given: Mock 설정
        Long currentLeaderId = 1L;
        Long newLeaderId = 2L;
        Long groupId = 1L;

        Users newLeader = new Users();
        newLeader.id = 2L;

        GroupMembers currentLeaderMember = new GroupMembers(testUser, testGroup, "LEADER");
        GroupMembers newLeaderMember = new GroupMembers(newLeader, testGroup, "MEMBER");

        when(groupMembersRepository.findByUserIdAndGroupId(currentLeaderId, groupId))
                .thenReturn(Optional.of(currentLeaderMember));
        when(groupMembersRepository.findByUserIdAndGroupId(newLeaderId, groupId))
                .thenReturn(Optional.of(newLeaderMember));
        when(groupMembersRepository.save(any(GroupMembers.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When: 권한 위임
        groupService.transferLeadership(currentLeaderId, newLeaderId, groupId);

        // Then: 검증
        assertThat(currentLeaderMember.getRole()).isEqualTo("MEMBER");
        assertThat(newLeaderMember.getRole()).isEqualTo("LEADER");
        verify(groupMembersRepository, times(2)).save(any(GroupMembers.class));
    }
}