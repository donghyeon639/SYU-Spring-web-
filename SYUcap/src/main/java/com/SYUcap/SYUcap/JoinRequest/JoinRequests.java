package com.SYUcap.SYUcap.JoinRequest;

import com.SYUcap.SYUcap.Group.Groups;
import com.SYUcap.SYUcap.User.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**그룹 가입 신청 엔티티 - 사용자의 그룹 가입 신청을 관리*/
@Entity
@ToString
@Getter
@Setter
@NoArgsConstructor
@Table(name = "join_requests",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "group_id"}) // 중복 신청 방지
        })
public class JoinRequests {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신청자 정보
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Users user;

    // 신청한 그룹 정보
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Groups group;

    // 신청 메시지
    @Column(columnDefinition = "TEXT")
    private String message;

    // 신청 상태 (PENDING: 대기, APPROVED: 승인, REJECTED: 거절)
    @Column(nullable = false)
    private String status = "PENDING";

    // 신청일
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    // 처리일 (승인/거절된 날짜)
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // 처리자 (그룹장)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Users processedBy;

    //생성자 - 가입 신청 생성
    public JoinRequests(Users user, Groups group, String message) {
        this.user = user;
        this.group = group;
        this.message = message;
        this.requestedAt = LocalDateTime.now();
    }

    //신청 승인 처리
    public void approve(Users processedBy) {
        this.status = "APPROVED";
        this.processedAt = LocalDateTime.now();
        this.processedBy = processedBy;
    }

    // 신청 거절 처리
    public void reject(Users processedBy) {
        this.status = "REJECTED";
        this.processedAt = LocalDateTime.now();
        this.processedBy = processedBy;
    }

    // 대기 중인 신청인지 확인
    public boolean isPending() {
        return "PENDING".equals(this.status);
    }

    // 승인된 신청인지 확인
    public boolean isApproved() {
        return "APPROVED".equals(this.status);
    }

    //거절된 신청인지 확인
    public boolean isRejected() {
        return "REJECTED".equals(this.status);
    }

    @PrePersist
    public void onCreate() {
        if (this.requestedAt == null) {
            this.requestedAt = LocalDateTime.now();
        }
    }
}