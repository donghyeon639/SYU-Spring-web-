package com.SYUcap.SYUcap.Board;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BoardRepository extends JpaRepository<Board,Long> {
    List<Board> findByTitleContainingOrContentContaining(String titleKeyword, String contentKeyword);
}

