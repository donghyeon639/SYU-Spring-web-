package com.SYUcap.SYUcap.Board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board,Long> {
    List<Board> findByTitleContainingOrContentContaining(String titleKeyword, String contentKeyword);

    List<Board> findAllByOrderByCreatedAtDesc();
    @Query("select b from Board b left join fetch b.user u order by b.createdAt desc")
    List<Board> findAllWithUserFetchJoinOrderByCreatedAtDesc();

    @Query("select b from Board b left join fetch b.user u where b.category = :cat order by b.createdAt desc")
    List<Board> findByCategoryWithUserFetchJoinOrderByCreatedAtDesc(@Param("cat") String category);

    @Query("select b from Board b left join fetch b.user u where b.title like concat('%', :keyword, '%') or b.content like concat('%', :keyword, '%')")
    List<Board> findByKeywordWithUserFetchJoin(@Param("keyword") String keyword);
}
