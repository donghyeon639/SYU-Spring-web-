package com.SYUcap.SYUcap.Board;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 댓글 ID

    @Column(columnDefinition = "TEXT")
    private String content; // 댓글 내용

    private String authorName; // 댓글 작성자 이름 (Users 엔티티와 직접 연결 X)

    private LocalDateTime createdAt; // 댓글 작성 시간

    // '하나의' 게시글(Board)에 '많은' 댓글(Comment)이 달릴 수 있음
    @ManyToOne
    private Board board; // 이 댓글이 속한 게시글

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}