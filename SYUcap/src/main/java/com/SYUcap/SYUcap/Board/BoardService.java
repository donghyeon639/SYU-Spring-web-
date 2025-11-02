package com.SYUcap.SYUcap.Board;

import com.SYUcap.SYUcap.User.Users;
import com.SYUcap.SYUcap.User.UserRepository;
import com.SYUcap.SYUcap.User.CustomUser;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Board> getAllSorted() {
        return boardRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Board> getByCategorySorted(String category) {
        return boardRepository.findByCategoryOrderByCreatedAtDesc(category);
    }

    @Transactional(readOnly = true)
    public Board getById(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Board not found: " + id));
    }

    public Board createPost(String category,
                            String title,
                            String content,
                            String location,
                            LocalDateTime meetingStartTime,
                            LocalDateTime meetingEndTime,
                            Integer limitCount,
                            Authentication auth) {
        // 로그인 사용자(CustomUser) 활용
        CustomUser principal = (CustomUser) auth.getPrincipal();
        Long authorId = principal.getId();
        String authorName = principal.getUsername();

        // Users 엔티티 레퍼런스 취득(추가 select 방지)
        Users authorRef = userRepository.getReferenceById(authorId);

        Board board = new Board();
        board.setCategory(category);
        board.setTitle(title);
        board.setContent(content);
        board.setLocation(location);
        board.setMeetingStartTime(meetingStartTime);
        board.setMeetingEndTime(meetingEndTime);
        board.setLimitCount(limitCount);
        board.setUser(authorRef);
        board.setAuthorName(authorName);
        return boardRepository.save(board);
    }

    public Board updatePost(Long id,
                            String title,
                            String content,
                            String location,
                            LocalDateTime meetingStartTime,
                            LocalDateTime meetingEndTime,
                            Integer limitCount) {
        Board board = getById(id);
        board.setTitle(title);
        board.setContent(content);
        board.setLocation(location);
        board.setMeetingStartTime(meetingStartTime);
        board.setMeetingEndTime(meetingEndTime);
        board.setLimitCount(limitCount);
        return boardRepository.save(board);
    }

    public void delete(Long id) {
        boardRepository.deleteById(id);
    }
}
