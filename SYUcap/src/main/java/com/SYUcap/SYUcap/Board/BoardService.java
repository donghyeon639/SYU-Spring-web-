package com.SYUcap.SYUcap.Board;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;

    // 제목은 20자 이하
    public static boolean isTitleValid(String title) {
        return title != null && title.length() <= 20;
    }

    // 내용은 200자 이하
    public static boolean isContentValid(String content) {
        return content != null && content.length() <= 200;
    }

    // 시작 시간이 종료 시간 이후면 경고 메시지 반환, 아니면 null
    public static String validateMeetingTime(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            return "시작 시간이 종료 시간보다 빠를 수 없습니다.";
        }
        return null;
    }

    // 제한 인원이 1 미만이면 경고 메시지 반환, 아니면 null
    public static String validateLimitCount(Integer limitCount) {
        if (limitCount == null || limitCount < 1) {
            return "최소 1명 이상이어야 합니다";
        }
        return null;
    }

    public List<Board> findAll() {
        return boardRepository.findAllWithUserFetchJoinOrderByCreatedAtDesc();
    }

    public Board findById(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + id));
    }

    public Board save(Board board) {
        Objects.requireNonNull(board, "게시글 정보가 null 입니다");
        if (!isTitleValid(board.getTitle())) {
            throw new IllegalArgumentException("제목 길이 20자를 초과할 수 없습니다");
        }
        if (!isContentValid(board.getContent())) {
            throw new IllegalArgumentException("내용 길이 200자를 초과할 수 없습니다");
        }
        String timeMsg = validateMeetingTime(board.getMeetingStartTime(), board.getMeetingEndTime());
        if (timeMsg != null) {
            throw new IllegalArgumentException(timeMsg);
        }
        String countMsg = validateLimitCount(board.getLimitCount());
        if (countMsg != null) {
            throw new IllegalArgumentException(countMsg);
        }
        return boardRepository.save(board);
    }

    public void delete(Long id) {
        boardRepository.deleteById(id);
    }

    public List<Board> searchBoards(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return boardRepository.findAllWithUserFetchJoinOrderByCreatedAtDesc();
        }
        return boardRepository.findByKeywordWithUserFetchJoin(keyword);
    }

    //전체 게시글을 생성일 기준 내림차순으로 정렬해서 반환
    @Transactional(readOnly = true)
    public List<Board> getAllSorted() {
        return boardRepository.findAllWithUserFetchJoinOrderByCreatedAtDesc();
    }

    // 카테고리별 목록 페치 조인
    @Transactional(readOnly = true)
    public List<Board> getByCategorySorted(String category) {
        return boardRepository.findByCategoryWithUserFetchJoinOrderByCreatedAtDesc(category);
    }
}
