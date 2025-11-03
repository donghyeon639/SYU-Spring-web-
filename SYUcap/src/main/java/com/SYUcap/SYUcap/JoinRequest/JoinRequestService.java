package com.SYUcap.SYUcap.JoinRequest;

import com.SYUcap.SYUcap.GroupMember.GroupMembers;
import com.SYUcap.SYUcap.GroupMember.GroupMembersRepository;
import com.SYUcap.SYUcap.Group.Groups;
import com.SYUcap.SYUcap.Group.GroupsRepository;
import com.SYUcap.SYUcap.User.UserRepository;
import com.SYUcap.SYUcap.User.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 가입 신청 서비스 - 그룹 가입 신청/승인/거절 관리
 */
@Service
@RequiredArgsConstructor
@Transactional
public class JoinRequestService {

    private final JoinRequestsRepository joinRequestsRepository;
    private final GroupMembersRepository groupMembersRepository;
    private final GroupsRepository groupsRepository;
    private final UserRepository userRepository;

    /**
     * 그룹 가입 신청
     * @param userId 신청자 ID
     * @param groupId 그룹 ID
     * @param message 신청 메시지
     * @return 생성된 가입 신청
     * @throws IllegalStateException 이미 멤버이거나 신청한 경우, 그룹이 가득 찬 경우
     */
    public JoinRequests requestJoin(Long userId, Long groupId, String message) {
        // 1. 사용자와 그룹 존재 확인
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));

        // 2. 이미 그룹 멤버인지 확인
        if (groupMembersRepository.findByUserIdAndGroupId(userId, groupId).isPresent()) {
            throw new IllegalStateException("이미 해당 그룹의 멤버입니다.");
        }

        // 3. 이미 가입 신청했는지 확인
        if (joinRequestsRepository.existsPendingRequestByUserIdAndGroupId(userId, groupId)) {
            throw new IllegalStateException("이미 가입 신청을 하였습니다.");
        }

        // 4. 그룹이 가득 찼는지 확인
        if (group.isFull()) {
            throw new IllegalStateException("그룹 인원이 가득 찼습니다.");
        }

        // 5. 그룹이 마감 상태인지 확인
        if ("CLOSED".equals(group.getStatus())) {
            throw new IllegalStateException("마감된 그룹입니다.");
        }

        // 6. 가입 신청 생성
        JoinRequests joinRequest = new JoinRequests(user, group, message);
        return joinRequestsRepository.save(joinRequest);
    }

    /**
     * 가입 신청 승인
     * @param requestId 신청 ID
     * @param leaderId 처리자(그룹장) ID
     * @throws IllegalStateException 권한이 없거나 이미 처리된 경우, 그룹이 가득 찬 경우
     */
    public void approveJoinRequest(Long requestId, Long leaderId) {
        // 1. 가입 신청 조회
        JoinRequests joinRequest = joinRequestsRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("가입 신청을 찾을 수 없습니다."));

        // 2. 대기 중인 신청인지 확인
        if (!joinRequest.isPending()) {
            throw new IllegalStateException("이미 처리된 신청입니다.");
        }

        // 3. 처리자가 그룹장인지 확인
        Groups group = joinRequest.getGroup();
        Optional<GroupMembers> leaderOpt = groupMembersRepository.findByGroupIdAndRole(group.getId(), "LEADER");
        if (leaderOpt.isEmpty() || !leaderOpt.get().getUser().getId().equals(leaderId)) {
            throw new IllegalStateException("그룹장만 가입 신청을 처리할 수 있습니다.");
        }

        // 4. 그룹이 가득 찼는지 다시 확인
        if (group.isFull()) {
            throw new IllegalStateException("그룹 인원이 가득 차서 승인할 수 없습니다.");
        }

        // 5. 신청 승인 처리
        Users leader = userRepository.findById(leaderId)
                .orElseThrow(() -> new IllegalArgumentException("처리자를 찾을 수 없습니다."));
        joinRequest.approve(leader);
        joinRequestsRepository.save(joinRequest);

        // 6. 그룹 멤버 추가
        GroupMembers newMember = new GroupMembers(joinRequest.getUser(), group, "MEMBER");
        groupMembersRepository.save(newMember);

        // 7. 그룹 인원수 증가
        group.incrementCurrentCount();
        groupsRepository.save(group);
    }

    /**
     * 가입 신청 거절
     * @param requestId 신청 ID
     * @param leaderId 처리자(그룹장) ID
     * @throws IllegalStateException 권한이 없거나 이미 처리된 경우
     */
    public void rejectJoinRequest(Long requestId, Long leaderId) {
        // 1. 가입 신청 조회
        JoinRequests joinRequest = joinRequestsRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("가입 신청을 찾을 수 없습니다."));

        // 2. 대기 중인 신청인지 확인
        if (!joinRequest.isPending()) {
            throw new IllegalStateException("이미 처리된 신청입니다.");
        }

        // 3. 처리자가 그룹장인지 확인
        Groups group = joinRequest.getGroup();
        Optional<GroupMembers> leaderOpt = groupMembersRepository.findByGroupIdAndRole(group.getId(), "LEADER");
        if (leaderOpt.isEmpty() || !leaderOpt.get().getUser().getId().equals(leaderId)) {
            throw new IllegalStateException("그룹장만 가입 신청을 처리할 수 있습니다.");
        }

        // 4. 신청 거절 처리
        Users leader = userRepository.findById(leaderId)
                .orElseThrow(() -> new IllegalArgumentException("처리자를 찾을 수 없습니다."));
        joinRequest.reject(leader);
        joinRequestsRepository.save(joinRequest);
    }

    /**
     * 가입 신청 취소 (신청자가 직접)
     * @param requestId 신청 ID
     * @param userId 신청자 ID
     * @throws IllegalStateException 권한이 없거나 이미 처리된 경우
     */
    public void cancelJoinRequest(Long requestId, Long userId) {
        // 1. 가입 신청 조회
        JoinRequests joinRequest = joinRequestsRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("가입 신청을 찾을 수 없습니다."));

        // 2. 대기 중인 신청인지 확인
        if (!joinRequest.isPending()) {
            throw new IllegalStateException("이미 처리된 신청입니다.");
        }

        // 3. 신청자 본인인지 확인
        if (!joinRequest.getUser().getId().equals(userId)) {
            throw new IllegalStateException("본인의 신청만 취소할 수 있습니다.");
        }

        // 4. 가입 신청 삭제
        joinRequestsRepository.delete(joinRequest);
    }

    /**
     * 사용자의 가입 신청 내역 조회
     */
    @Transactional(readOnly = true)
    public List<JoinRequests> getUserJoinRequests(Long userId) {
        return joinRequestsRepository.findByUserIdOrderByRequestedAtDesc(userId);
    }

    /**
     * 그룹장이 처리해야 할 대기 중인 신청 조회
     */
    @Transactional(readOnly = true)
    public List<JoinRequests> getPendingRequestsForLeader(Long leaderId) {
        return joinRequestsRepository.findPendingRequestsByLeaderId(leaderId);
    }

    /**
     * 특정 그룹의 가입 신청 내역 조회 (상태별)
     */
    @Transactional(readOnly = true)
    public List<JoinRequests> getJoinRequestsByGroup(Long groupId, String status) {
        if (status != null && !status.isEmpty()) {
            return joinRequestsRepository.findByGroupIdAndStatusOrderByRequestedAtDesc(groupId, status);
        }
        return joinRequestsRepository.findByGroupIdOrderByRequestedAtDesc(groupId);
    }

    /**
     * 사용자가 특정 그룹에 대기 중인 신청이 있는지 확인
     */
    @Transactional(readOnly = true)
    public boolean hasPendingRequest(Long userId, Long groupId) {
        return joinRequestsRepository.existsPendingRequestByUserIdAndGroupId(userId, groupId);
    }

    /**
     * 특정 그룹의 대기 중인 가입 신청 조회
     */
    @Transactional(readOnly = true)
    public List<JoinRequests> getPendingJoinRequests(Long groupId) {
        return joinRequestsRepository.findByGroupIdAndStatusOrderByRequestedAt(groupId, "PENDING");
    }
}