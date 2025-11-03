package com.SYUcap.SYUcap.Group;

import com.SYUcap.SYUcap.Board.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 그룹 Repository - 그룹 관련 데이터베이스 조회 메서드
 */
public interface GroupsRepository extends JpaRepository<Groups, Long> {

    /**
     * Board ID로 그룹 조회
     * @param boardId Board ID
     * @return 해당 Board의 그룹
     */
    Optional<Groups> findByBoardId(Long boardId);

    /**
     * Board 엔티티로 그룹 조회
     * @param board Board 엔티티
     * @return 해당 Board의 그룹
     */
    Optional<Groups> findByBoard(Board board);

    /**
     * 활성 상태인 그룹들 조회 (최신순)
     * @return 활성 그룹 리스트
     */
    List<Groups> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * 특정 카테고리의 활성 그룹들 조회
     * @param category 카테고리
     * @return 해당 카테고리의 활성 그룹 리스트
     */
    @Query("SELECT g FROM Groups g WHERE g.status = 'ACTIVE' AND g.board.category = :category ORDER BY g.createdAt DESC")
    List<Groups> findActiveByCategoryOrderByCreatedAtDesc(@Param("category") String category);

    /**
     * 가득 차지 않은 그룹들 조회 (참여 가능한 그룹들)
     * @return 참여 가능한 그룹 리스트
     */
    @Query("SELECT g FROM Groups g WHERE g.currentCount < g.board.limitCount AND g.status = 'ACTIVE' ORDER BY g.createdAt DESC")
    List<Groups> findAvailableGroupsOrderByCreatedAtDesc();

    /**
     * 특정 사용자가 리더인 그룹들 조회
     * @param userId 사용자 ID
     * @return 해당 사용자가 리더인 그룹 리스트
     */
    @Query("SELECT g FROM Groups g JOIN GroupMembers gm ON g.id = gm.group.id WHERE gm.user.id = :userId AND gm.role = 'LEADER' ORDER BY g.createdAt DESC")
    List<Groups> findByLeaderIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * 그룹 개수 통계 - 상태별
     * @param status 그룹 상태
     * @return 해당 상태의 그룹 개수
     */
    long countByStatus(String status);

    /**
     * 특정 사용자가 리더이고 특정 카테고리인 그룹들 조회
     * @param leaderId 리더 ID
     * @param category 카테고리
     * @return 해당 카테고리의 리더 그룹 리스트
     */
    @Query("SELECT g FROM Groups g JOIN GroupMembers gm ON g.id = gm.group.id " +
            "WHERE gm.user.id = :leaderId AND gm.role = 'LEADER' AND g.board.category = :category " +
            "ORDER BY g.createdAt DESC")
    List<Groups> findByLeaderIdAndCategoryOrderByCreatedAtDesc(@Param("leaderId") Long leaderId,
                                                               @Param("category") String category);
}
