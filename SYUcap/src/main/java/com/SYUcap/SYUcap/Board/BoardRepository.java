package com.SYUcap.SYUcap.Board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board,Long> {

    List<Board> findAllByOrderByCreatedAtDesc();

    List<Board> findByCategoryOrderByCreatedAtDesc(String category);
}
