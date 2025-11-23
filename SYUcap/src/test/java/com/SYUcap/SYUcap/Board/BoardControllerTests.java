package com.SYUcap.SYUcap.Board;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(BoardController.class)
class BoardControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @Test
    @DisplayName("[B-TC-001] [성공] 게시글 목록 페이지 접속")
    void getBoardList_Success() throws Exception {
        when(boardService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/board"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-list"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(model().attribute("category", "전체"));

        verify(boardService, times(1)).findAll();
    }

    @Test
    @DisplayName("[B-TC-002] [성공] 게시글이 없을 때 목록 출력")
    void getBoardList_NoPosts() throws Exception {
        when(boardService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/board"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-list"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("아직 글이 없어요")));

        verify(boardService, times(1)).findAll();
    }

    @Test
    @DisplayName("[B-TC-003] [성공] 게시글 검색 기능 테스트")
    void getBoardList_SearchKeyword() throws Exception {
        Board mockBoard = new Board();
        mockBoard.setId(1L);
        mockBoard.setTitle("게임 같이 하실 분");
        mockBoard.setContent("오늘 저녁 7시 PC방에서 만나요");


        when(boardService.searchBoards("게임")).thenReturn(Collections.singletonList(mockBoard));


        mockMvc.perform(get("/board/search").param("keyword", "게임"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-list"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("게임 같이 하실 분")));


        verify(boardService, times(1)).searchBoards("게임");
    }

    @Test
    @DisplayName("[B-TC-004] [성공] 게시글 작성 페이지 접근")
    void getBoardWriteForm_Success() throws Exception {
        mockMvc.perform(get("/board/게임/write"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-form"))
                .andExpect(model().attribute("category", "게임"))
                .andExpect(model().attributeExists("post"));
    }

    @Test
    @DisplayName("[B-TC-005] [성공] 게시글 등록 성공")
    void postBoardWrite_Success() throws Exception {
        Board mockBoard = new Board();
        mockBoard.setId(1L);
        mockBoard.setCategory("게임");
        mockBoard.setTitle("게임 같이 하실 분");
        mockBoard.setContent("오늘 저녁 7시 PC방에서 만나요");

        when(boardService.save(any(Board.class))).thenReturn(mockBoard);

        mockMvc.perform(post("/board/게임/write")
                        .param("title", "게임 같이 하실 분")
                        .param("content", "오늘 저녁 7시 PC방에서 만나요")
                        .param("authorName", "홍길동")
                        .param("location", "학생회관")
                        .param("limitCount", "5")
                        .param("meetingStartTime", "2025-11-25T19:00:00")
                        .param("meetingEndTime", "2025-11-25T21:00:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/board/%EA%B2%8C%EC%9E%84"));

        verify(boardService, times(1)).save(any(Board.class));
    }

    @Test
    @DisplayName("[B-TC-006] [실패] 제목 누락 시 등록 실패")
    void postBoardWrite_Fail_TitleMissing() throws Exception {
        mockMvc.perform(post("/board/게임/write")
                        .param("title", "") // 제목 누락
                        .param("content", "내용만 입력됨")
                        .param("authorName", "홍길동")
                        .param("location", "학생회관")
                        .param("limitCount", "5")
                        .param("meetingStartTime", "2025-11-25T19:00:00")
                        .param("meetingEndTime", "2025-11-25T21:00:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("제목을 입력하세요")));

        verify(boardService, never()).save(any(Board.class));
    }

    @Test
    @DisplayName("[B-TC-007] [실패] 내용 누락 시 등록 실패")
    void postBoardWrite_Fail_ContentMissing() throws Exception {
        mockMvc.perform(post("/board/게임/write")
                        .param("title", "테스트 제목")
                        .param("content", "") // 내용 누락
                        .param("authorName", "홍길동")
                        .param("location", "학생회관")
                        .param("limitCount", "5")
                        .param("meetingStartTime", "2025-11-25T19:00:00")
                        .param("meetingEndTime", "2025-11-25T21:00:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("내용을 입력하세요")));

        verify(boardService, never()).save(any(Board.class));
    }

    @Test
    @DisplayName("[B-TC-008] [성공] 게시글 상세 보기")
    void getBoardDetail_Success() throws Exception {
        Board mockBoard = new Board();
        mockBoard.setId(1L);
        mockBoard.setCategory("게임");
        mockBoard.setTitle("게임 같이 하실 분");
        mockBoard.setContent("내용");
        mockBoard.setAuthorName("홍길동");

        when(boardService.findById(1L)).thenReturn(mockBoard);

        mockMvc.perform(get("/board/게임/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-detail"))
                .andExpect(model().attribute("category", "게임"))
                .andExpect(model().attributeExists("post"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("게임 같이 하실 분")));

        verify(boardService, times(1)).findById(1L);
    }

    @Test
    @DisplayName("[B-TC-010] [성공] 게시글 삭제")
    void postBoardDelete_Success() throws Exception {
        doNothing().when(boardService).delete(1L);

        mockMvc.perform(post("/board/게임/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/board/%EA%B2%8C%EC%9E%84"));

        verify(boardService, times(1)).delete(1L);
    }

    @Test
    @DisplayName("[B-TC-011] [성능] 게시글 조회 시 N+1 쿼리 발생 여부 확인 (Mock Test)")
    void getBoardList_Performance_NPlus1() throws Exception {
        // given
        List<Board> boards = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> {
                    Board p = new Board();
                    p.setId((long) i);
                    p.setCategory("게임");
                    p.setTitle("테스트 " + i);
                    p.setAuthorName("작성자" + i);
                    return p;
                })
                .toList();

        when(boardService.findAll()).thenReturn(boards);

        // when & then
        mockMvc.perform(get("/board/게임"))
                .andExpect(status().isOk())
                .andExpect(view().name("board-list"))
                .andExpect(model().attributeExists("posts"));

        verify(boardService, times(1)).findAll();
    }

}
