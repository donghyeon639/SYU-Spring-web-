package com.SYUcap.SYUcap.Group;

import com.SYUcap.SYUcap.Board.Board;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// 그룹 엔티티 - Board와 1:1 관계를 가지며, 그룹의 상세 정보를 관리
@Entity
@ToString
@Getter
@Setter
@NoArgsConstructor
public class Groups {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Board와 1:1 관계 - 하나의 게시글은 하나의 그룹을 가짐
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Board board;

    // 현재 참여 인원 수
    @Column(name = "current_count", nullable = false)
    private Integer currentCount = 1; // 그룹장 포함하여 시작

    // 그룹 생성일
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 그룹 상태 (ACTIVE: 활성, CLOSED: 마감, COMPLETED: 완료)
    @Column(nullable = false)
    private String status = "ACTIVE";

    /**생성자 - Board 정보로 그룹 생성*/

    public Groups(Board board) {
        this.board = board;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 참여 인원 증가
     */
    public void incrementCurrentCount() {
        this.currentCount++;
        // 제한 인원에 도달하면 그룹 상태를 CLOSED로 변경
        if (this.currentCount >= this.board.getLimitCount()) {
            this.status = "CLOSED";
        }
    }

    /*** 참여 인원 감소*/
    public void decrementCurrentCount() {
        if (this.currentCount > 1) { // 그룹장은 최소 1명 유지
            this.currentCount--;
            // 제한 인원 미만이면 다시 ACTIVE 상태로 변경
            if (this.currentCount < this.board.getLimitCount() && "CLOSED".equals(this.status)) {
                this.status = "ACTIVE";
            }
        }
    }

    /**
     * 그룹이 가득 찼는지 확인
     * @return 가득 찼으면 true, 아니면 false
     */
    public boolean isFull() {
        return this.currentCount >= this.board.getLimitCount();
    }

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}