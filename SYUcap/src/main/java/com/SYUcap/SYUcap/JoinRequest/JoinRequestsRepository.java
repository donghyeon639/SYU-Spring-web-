package com.SYUcap.SYUcap.JoinRequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**가입 신청 Repository - 가입 신청 관련 데이터베이스 조회 메서드*/
public interface JoinRequestsRepository extends JpaRepository<JoinRequests, Long> {

    // 특정 그룹의 가입 신청 목록 조회 (신청일 역순)
    List<JoinRequests> findByGroupIdOrderByRequestedAtDesc(Long groupId);

    //특정 사용자의 가입 신청 목록 조회 (신청일 역순)
    List<JoinRequests> findByUserIdOrderByRequestedAtDesc(Long userId);

    // 특정 그룹의 특정 상태 가입 신청 조회
    List<JoinRequests> findByGroupIdAndStatusOrderByRequestedAt(Long groupId, String status);

    // 특정 그룹의 특정 상태 가입 신청 조회 (신청일 역순)
    List<JoinRequests> findByGroupIdAndStatusOrderByRequestedAtDesc(Long groupId, String status);

    // 특정 사용자가 특정 그룹에 대기 중인 신청이 있는지 확인
    @Query("SELECT CASE WHEN COUNT(jr) > 0 THEN true ELSE false END FROM JoinRequests jr " +
            "WHERE jr.user.id = :userId AND jr.group.id = :groupId AND jr.status = 'PENDING'")
    boolean existsPendingRequestByUserIdAndGroupId(@Param("userId") Long userId,
                                                   @Param("groupId") Long groupId);

    //그룹장이 처리해야 할 대기 중인 신청 조회
    @Query("SELECT jr FROM JoinRequests jr JOIN GroupMembers gm ON jr.group.id = gm.group.id " +
            "WHERE gm.user.id = :leaderId AND gm.role = 'LEADER' AND jr.status = 'PENDING' " +
            "ORDER BY jr.requestedAt DESC")
    List<JoinRequests> findPendingRequestsByLeaderId(@Param("leaderId") Long leaderId);

    // 특정 그룹의 모든 가입 신청 삭제
    @Modifying
    @Query("DELETE FROM JoinRequests jr WHERE jr.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}