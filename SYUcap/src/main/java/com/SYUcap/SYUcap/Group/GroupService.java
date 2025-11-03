package com.SYUcap.SYUcap.Group;

import com.SYUcap.SYUcap.Board.Board;
import com.SYUcap.SYUcap.Board.BoardRepository;
import com.SYUcap.SYUcap.GroupMember.GroupMembers;
import com.SYUcap.SYUcap.GroupMember.GroupMembersRepository;
import com.SYUcap.SYUcap.JoinRequest.JoinRequests;
import com.SYUcap.SYUcap.JoinRequest.JoinRequestsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 그룹 서비스 - 그룹 생성, 조회, 관리 기능
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {

    private final GroupsRepository groupsRepository;
    private final GroupMembersRepository groupMembersRepository;
    private final JoinRequestsRepository joinRequestsRepository;
    private final BoardRepository boardRepository;

    /**
     * 게시글 작성 시 자동으로 그룹 생성
     * @param board 생성된 게시글
     * @return 생성된 그룹
     */
    public Groups createGroupFromBoard(Board board) {
        // 1. 그룹 생성
        Groups group = new Groups(board);
        Groups savedGroup = groupsRepository.save(group);

        // 2. 게시글 작성자를 그룹장으로 추가
        GroupMembers leader = new GroupMembers(board.getUser(), savedGroup, "LEADER");
        groupMembersRepository.save(leader);

        return savedGroup;
    }

    /**
     * 그룹 상세 조회 (Board ID로)
     * @param boardId 게시글 ID
     * @return 그룹 정보
     */
    @Transactional(readOnly = true)
    public Optional<Groups> getGroupByBoardId(Long boardId) {
        return groupsRepository.findByBoardId(boardId);
    }

    /**
     * 그룹 ID로 조회
     * @param groupId 그룹 ID
     * @return 그룹 정보
     */
    @Transactional(readOnly = true)
    public Optional<Groups> getGroupById(Long groupId) {
        return groupsRepository.findById(groupId);
    }

    /**
     * 사용자가 리더인 그룹들 조회
     * @param userId 사용자 ID
     * @return 리더인 그룹 리스트
     */
    @Transactional(readOnly = true)
    public List<Groups> getGroupsByLeader(Long userId) {
        return groupsRepository.findByLeaderIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 그룹 멤버 목록 조회
     * @param groupId 그룹 ID
     * @return 그룹 멤버 리스트
     */
    @Transactional(readOnly = true)
    public List<GroupMembers> getGroupMembers(Long groupId) {
        return groupMembersRepository.findByGroupIdOrderByJoinedAt(groupId);
    }

    /**
     * 그룹 리더 조회
     * @param groupId 그룹 ID
     * @return 그룹 리더 정보
     */
    @Transactional(readOnly = true)
    public Optional<GroupMembers> getGroupLeader(Long groupId) {
        return groupMembersRepository.findByGroupIdAndRole(groupId, "LEADER");
    }

    /**
     * 사용자가 그룹 멤버인지 확인
     * @param userId 사용자 ID
     * @param groupId 그룹 ID
     * @return 멤버이면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    public boolean isMember(Long userId, Long groupId) {
        return groupMembersRepository.findByUserIdAndGroupId(userId, groupId).isPresent();
    }

    /**
     * 사용자가 그룹 리더인지 확인
     * @param userId 사용자 ID
     * @param groupId 그룹 ID
     * @return 리더이면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    public boolean isLeader(Long userId, Long groupId) {
        Optional<GroupMembers> member = groupMembersRepository.findByUserIdAndGroupId(userId, groupId);
        return member.isPresent() && member.get().isLeader();
    }

    /**
     * 사용자가 참여 중인 그룹들 조회
     * @param userId 사용자 ID
     * @return 참여 중인 그룹 멤버 리스트
     */
    @Transactional(readOnly = true)
    public List<GroupMembers> getUserGroups(Long userId) {
        return groupMembersRepository.findByUserIdOrderByJoinedAtDesc(userId);
    }

    /**
     * 그룹 멤버 탈퇴 처리
     * @param userId 사용자 ID
     * @param groupId 그룹 ID
     * @throws IllegalArgumentException 그룹장은 탈퇴할 수 없음
     */
    public void leaveGroup(Long userId, Long groupId) {
        Optional<GroupMembers> memberOpt = groupMembersRepository.findByUserIdAndGroupId(userId, groupId);

        if (memberOpt.isPresent()) {
            GroupMembers member = memberOpt.get();

            // 그룹장은 탈퇴할 수 없음 (그룹 해체 기능 별도 필요)
            if (member.isLeader()) {
                throw new IllegalArgumentException("그룹장은 탈퇴할 수 없습니다. 그룹을 해체하거나 리더를 위임해주세요.");
            }

            // 멤버 삭제
            groupMembersRepository.delete(member);

            // 그룹 현재 인원 수 감소
            Groups group = member.getGroup();
            group.decrementCurrentCount();
            groupsRepository.save(group);
        }
    }

    /**
     * 카테고리별 내 그룹 조회 (참여 중인 그룹) - 그룹장인 그룹은 제외
     * @param userId 사용자 ID
     * @param category 카테고리 (null이면 전체)
     * @return 카테고리별 참여 그룹 리스트 (그룹장 제외)
     */
    @Transactional(readOnly = true)
    public List<GroupMembers> getUserGroupsByCategory(Long userId, String category) {
        List<GroupMembers> allGroups;
        if (category == null || category.trim().isEmpty()) {
            allGroups = groupMembersRepository.findByUserIdOrderByJoinedAtDesc(userId);
        } else {
            allGroups = groupMembersRepository.findByUserIdAndGroupBoardCategoryOrderByJoinedAtDesc(userId, category);
        }

        // 그룹장인 그룹들은 제외하고 일반 멤버인 그룹만 반환
        return allGroups.stream()
                .filter(member -> !"LEADER".equals(member.getRole()))
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 내가 만든 그룹 조회
     * @param userId 사용자 ID
     * @param category 카테고리 (null이면 전체)
     * @return 카테고리별 리더 그룹 리스트
     */
    @Transactional(readOnly = true)
    public List<Groups> getLeaderGroupsByCategory(Long userId, String category) {
        if (category == null || category.trim().isEmpty()) {
            return groupsRepository.findByLeaderIdOrderByCreatedAtDesc(userId);
        }
        return groupsRepository.findByLeaderIdAndCategoryOrderByCreatedAtDesc(userId, category);
    }

    /**
     * 멤버 강제 퇴장 (그룹장 권한)
     * @param userId 퇴장시킬 사용자 ID
     * @param groupId 그룹 ID
     * @param leaderId 그룹장 ID (권한 확인용)
     * @throws IllegalStateException 권한이 없거나 그룹장을 퇴장시키려는 경우
     */
    public void kickMember(Long userId, Long groupId, Long leaderId) {
        // 1. 그룹장 권한 확인
        if (!isLeader(leaderId, groupId)) {
            throw new IllegalStateException("그룹장만 멤버를 내보낼 수 있습니다.");
        }

        // 2. 퇴장시킬 멤버 조회
        Optional<GroupMembers> memberOpt = groupMembersRepository.findByUserIdAndGroupId(userId, groupId);
        if (memberOpt.isEmpty()) {
            throw new IllegalArgumentException("해당 사용자는 이 그룹의 멤버가 아닙니다.");
        }

        GroupMembers member = memberOpt.get();

        // 3. 그룹장은 퇴장시킬 수 없음
        if (member.isLeader()) {
            throw new IllegalStateException("그룹장은 내보낼 수 없습니다.");
        }

        // 4. 멤버 삭제
        groupMembersRepository.delete(member);

        // 5. 그룹 현재 인원 수 감소
        Groups group = member.getGroup();
        group.decrementCurrentCount();
        groupsRepository.save(group);
    }

    /**
     * 그룹장 위임
     * @param currentLeaderId 현재 그룹장 ID
     * @param newLeaderId 새 그룹장 ID
     * @param groupId 그룹 ID
     * @throws IllegalStateException 권한이 없거나 새 리더가 멤버가 아닌 경우
     */
    public void transferLeadership(Long currentLeaderId, Long newLeaderId, Long groupId) {
        // 1. 현재 그룹장 권한 확인
        if (!isLeader(currentLeaderId, groupId)) {
            throw new IllegalStateException("그룹장만 권한을 위임할 수 있습니다.");
        }

        // 2. 현재 그룹장과 새 그룹장 조회
        Optional<GroupMembers> currentLeaderOpt = groupMembersRepository.findByUserIdAndGroupId(currentLeaderId, groupId);
        Optional<GroupMembers> newLeaderOpt = groupMembersRepository.findByUserIdAndGroupId(newLeaderId, groupId);

        if (currentLeaderOpt.isEmpty() || newLeaderOpt.isEmpty()) {
            throw new IllegalArgumentException("해당 사용자들이 이 그룹의 멤버가 아닙니다.");
        }

        GroupMembers currentLeader = currentLeaderOpt.get();
        GroupMembers newLeader = newLeaderOpt.get();

        // 3. 역할 변경
        currentLeader.setRole("MEMBER");
        newLeader.setRole("LEADER");

        groupMembersRepository.save(currentLeader);
        groupMembersRepository.save(newLeader);
    }

    /**
     * 그룹 해체 (그룹장만 가능)
     * @param groupId 그룹 ID
     * @param leaderId 그룹장 ID (권한 확인용)
     * @throws IllegalStateException 권한이 없는 경우
     */
    public void deleteGroup(Long groupId, Long leaderId) {
        // 1. 그룹장 권한 확인
        if (!isLeader(leaderId, groupId)) {
            throw new IllegalStateException("그룹장만 그룹을 해체할 수 있습니다.");
        }

        // 2. 그룹 조회
        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));

        Board board = group.getBoard();

        // 3. 가입 신청 삭제
        joinRequestsRepository.deleteByGroupId(groupId);

        // 4. 그룹 멤버 삭제
        groupMembersRepository.deleteByGroupId(groupId);

        // 5. 그룹 삭제
        groupsRepository.delete(group);

        // 6. 게시글 완전 삭제
        boardRepository.delete(board);
    }

    /**
     * 그룹 마감 (그룹장만 가능)
     * @param groupId 그룹 ID
     * @param leaderId 그룹장 ID (권한 확인용)
     * @throws IllegalStateException 권한이 없는 경우
     */
    public void closeGroup(Long groupId, Long leaderId) {
        // 1. 그룹장 권한 확인
        if (!isLeader(leaderId, groupId)) {
            throw new IllegalStateException("그룹장만 그룹을 마감할 수 있습니다.");
        }

        // 2. 그룹 조회
        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));

        // 3. 이미 마감된 그룹인지 확인
        if ("CLOSED".equals(group.getStatus())) {
            throw new IllegalStateException("이미 마감된 그룹입니다.");
        }

        // 4. 그룹 상태를 CLOSED로 변경
        group.setStatus("CLOSED");
        groupsRepository.save(group);

        // 5. 대기 중인 가입 신청 모두 거절 처리
        List<JoinRequests> pendingRequests = joinRequestsRepository.findByGroupIdAndStatusOrderByRequestedAt(groupId, "PENDING");
        for (JoinRequests request : pendingRequests) {
            request.setStatus("REJECTED");
            request.setProcessedAt(LocalDateTime.now());
            request.setProcessedBy(group.getBoard().getUser()); // 그룹장이 처리
        }
        joinRequestsRepository.saveAll(pendingRequests);
    }

    /**
     * 그룹 마감 해제 (그룹장만 가능)
     */
    public void reopenGroup(Long groupId, Long leaderId) {
        // 1. 그룹장 권한 확인
        if (!isLeader(leaderId, groupId)) {
            throw new IllegalStateException("그룹장만 그룹을 다시 열 수 있습니다.");
        }

        // 2. 그룹 조회
        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));

        // 3. 마감된 그룹인지 확인
        if (!"CLOSED".equals(group.getStatus())) {
            throw new IllegalStateException("마감되지 않은 그룹입니다.");
        }

        // 4. 그룹이 가득 찼는지 확인 (가득 차면 다시 열 수 없음)
        if (group.isFull()) {
            throw new IllegalStateException("인원이 가득 차서 다시 열 수 없습니다.");
        }

        // 5. 그룹 상태를 ACTIVE로 변경
        group.setStatus("ACTIVE");
        groupsRepository.save(group);
    }
}
