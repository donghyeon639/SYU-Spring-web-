package com.SYUcap.SYUcap.Board;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;

    public List<Board> findAll() {
        return boardRepository.findAll();
    }

    public Board findById(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + id));
    }

    public Board save(Board board) {
        // 제목
        if (board.getTitle() == null || board.getTitle().isBlank()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if (board.getTitle().length() > 20) {
            throw new IllegalArgumentException("제목은 20자를 넘을 수 없습니다.");
        }

        // 내용
        if (board.getContent() == null || board.getContent().isBlank()) {
            throw new IllegalArgumentException("내용은 필수입니다.");
        }
        if (board.getContent().length() > 200) {
            throw new IllegalArgumentException("내용은 200자를 넘을 수 없습니다.");
        }

        // 제한 인원
        if (board.getLimitCount() == null || board.getLimitCount() < 1) {
            throw new IllegalArgumentException("제한 인원은 1 이상이어야 합니다.");
        }

        // 모임 시간
        if (board.getMeetingStartTime() == null || board.getMeetingEndTime() == null) {
            throw new IllegalArgumentException("모임 시간은 필수입니다.");
        }
        if (board.getMeetingStartTime().isAfter(board.getMeetingEndTime())) {
            throw new IllegalArgumentException("시작 시간이 종료 시간보다 늦을 수 없습니다.");
        }
        return boardRepository.save(board);
    }

    public void delete(Long id) {
        boardRepository.deleteById(id);
    }

    public List<Board> searchBoards(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return boardRepository.findAll();
        }
        return boardRepository.findByTitleContainingOrContentContaining(keyword, keyword);
    }
}
