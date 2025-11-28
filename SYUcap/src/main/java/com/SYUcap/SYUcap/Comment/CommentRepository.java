package com.SYUcap.SYUcap.Comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

//n+1 문제 해결
    @Query("select c from Comment c join fetch c.user where c.board.id = :boardId")
    List<Comment> findByBoardId(@Param("boardId") Long boardId);
}