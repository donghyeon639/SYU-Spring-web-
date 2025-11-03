package com.SYUcap.SYUcap.GroupMember;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 그룹 멤버 Repository - 그룹 멤버 관련 데이터베이스 조회 메서드
 */
public interface GroupMembersRepository extends JpaRepository<GroupMembers, Long> {

    /**
     * 특정 그룹의 멤버 목록 조회 (가입일순)
     * @param groupId 그룹 ID
     * @return 그룹 멤버 리스트
     */
    List<GroupMembers> findByGroupIdOrderByJoinedAt(Long groupId);

    /**
     * 특정 사용자가 속한 그룹 멤버 정보 조회 (가입일 역순)
     * @param userId 사용자 ID
     * @return 그룹 멤버 리스트
     */
    List<GroupMembers> findByUserIdOrderByJoinedAtDesc(Long userId);

    /**
     * 특정 사용자와 그룹의 멤버 정보 조회
     * @param userId 사용자 ID
     * @param groupId 그룹 ID
     * @return 그룹 멤버 정보
     */
    Optional<GroupMembers> findByUserIdAndGroupId(Long userId, Long groupId);

    /**
     * 특정 그룹의 특정 역할 멤버 조회
     * @param groupId 그룹 ID
     * @param role 역할 (LEADER, MEMBER)
     * @return 그룹 멤버 정보
     */
    Optional<GroupMembers> findByGroupIdAndRole(Long groupId, String role);

    /**
     * 특정 그룹의 멤버 수 조회
     * @param groupId 그룹 ID
     * @return 멤버 수
     */
    long countByGroupId(Long groupId);

    /**
     * 특정 사용자가 속한 그룹 수 조회
     * @param userId 사용자 ID
     * @return 그룹 수
     */
    long countByUserId(Long userId);

    /**
     * 특정 그룹의 모든 멤버 삭제
     * @param groupId 그룹 ID
     */
    @Modifying
    @Query("DELETE FROM GroupMembers gm WHERE gm.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);

    /**
     * 특정 사용자가 특정 카테고리 그룹에 속한 멤버 정보 조회
     * @param userId 사용자 ID
     * @param category 카테고리
     * @return 그룹 멤버 리스트
     */
    @Query("SELECT gm FROM GroupMembers gm WHERE gm.user.id = :userId AND gm.group.board.category = :category ORDER BY gm.joinedAt DESC")
    List<GroupMembers> findByUserIdAndGroupBoardCategoryOrderByJoinedAtDesc(@Param("userId") Long userId,
                                                                            @Param("category") String category);
}