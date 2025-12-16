package com.SYUcap.SYUcap.Comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByBoardId(Long boardId);

    @Query("select c from Comment c left join fetch c.user u where c.board.id = :boardId")
    List<Comment> findByBoardIdWithUserFetchJoin(@Param("boardId") Long boardId);

}