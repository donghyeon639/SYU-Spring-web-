package com.SYUcap.SYUcap.GroupMember;

import com.SYUcap.SYUcap.Group.Groups;
import com.SYUcap.SYUcap.User.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**그룹 멤버 엔티티 - Users와 Groups 간의 다대다 관계를 매핑 */
@Entity
@ToString
@Getter
@Setter
@NoArgsConstructor
@Table(name = "group_members",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "group_id"}) // 같은 사용자가 같은 그룹에 중복 가입 방지
        })
public class GroupMembers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 정보
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Users user;

    // 그룹 정보
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Groups group;

    // 멤버 역할 (LEADER: 그룹장, MEMBER: 일반 멤버)
    @Column(nullable = false)
    private String role = "MEMBER";

    // 가입일
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    //생성자 - 일반 멤버로 그룹 가입
    public GroupMembers(Users user, Groups group) {
        this.user = user;
        this.group = group;
        this.joinedAt = LocalDateTime.now();
    }

    //생성자 - 역할을 지정하여 그룹 가입 (그룹장 생성 시 사용)
    public GroupMembers(Users user, Groups group, String role) {
        this.user = user;
        this.group = group;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

    //그룹장인지 확인@return 그룹장이면 true, 아니면 false
    public boolean isLeader() {
        return "LEADER".equals(this.role);
    }

    @PrePersist
    public void onCreate() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
    }
}