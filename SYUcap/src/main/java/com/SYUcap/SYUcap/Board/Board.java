package com.SYUcap.SYUcap.Board;

import com.SYUcap.SYUcap.User.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@ToString
@Getter
@Setter
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // 게시글 ID

    private String category;            // 게임/스터디/영화/운동/밥약
    private String title;               // 제목

    @Column(columnDefinition = "TEXT")
    private String content;             // 내용

    private String location;            // 장소
    private LocalDateTime meetingStartTime; // 시작시간
    private LocalDateTime meetingEndTime;   // 종료시간

    private Integer limitCount;         // 제한 인원
    private String authorName;          // 작성자 이름
    private LocalDateTime createdAt;    // 작성일

    // 작성자(유저) 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Users user;

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
