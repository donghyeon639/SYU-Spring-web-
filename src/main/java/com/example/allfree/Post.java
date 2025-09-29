package com.example.allfree;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "post")
public class Post {

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

    public Post() {}

    public Post(Long id, String category, String title, String content,
                String location, LocalDateTime meetingStartTime,
                LocalDateTime meetingEndTime, Integer limitCount,
                String authorName, LocalDateTime createdAt) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.content = content;
        this.location = location;
        this.meetingStartTime = meetingStartTime;
        this.meetingEndTime = meetingEndTime;
        this.limitCount = limitCount;
        this.authorName = authorName;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // === Getter / Setter ===
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getMeetingStartTime() { return meetingStartTime; }
    public void setMeetingStartTime(LocalDateTime meetingStartTime) { this.meetingStartTime = meetingStartTime; }

    public LocalDateTime getMeetingEndTime() { return meetingEndTime; }
    public void setMeetingEndTime(LocalDateTime meetingEndTime) { this.meetingEndTime = meetingEndTime; }

    public Integer getLimitCount() { return limitCount; }
    public void setLimitCount(Integer limitCount) { this.limitCount = limitCount; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
