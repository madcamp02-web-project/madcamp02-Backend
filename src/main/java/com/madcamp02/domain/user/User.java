package com.madcamp02.domain.user;

import com.madcamp02.domain.chat.ChatHistory;
import com.madcamp02.domain.item.Inventory;
import com.madcamp02.domain.notification.Notification;
import com.madcamp02.domain.portfolio.Portfolio;
import com.madcamp02.domain.trade.TradeLog;
import com.madcamp02.domain.wallet.Wallet;
import com.madcamp02.domain.watchlist.Watchlist;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//이런 방식으로 위에 사용할
//package를 지정하는 이유는 일종의 namespace를 만드는 것을 뜻함

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    //PK 선언
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //id 값을 내가 직접 넣지 않고 DB(MySQL 등)가 알아서 1씩 증가시키게 하겠다 라는 뜻 
    @Column(name = "user_id")
    private long userId; //java 내에서 userId라는 변수로 사용하겠다는 것것

    // 일반 컬럼들
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 20)
    private String provider;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "saju_element", length = 10)
    private String sajuElement;

    @Column(name = "zodiac_sign", length = 20)
    private String zodiacSign;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    //다른 Table간의 관계 추가
    // 1:1 관계 (User ↔ Wallet)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Wallet wallet;

//@OneToOne 이 어노테이션 자체가 일대일 관계를 의미
//mappedBy = "user"라는 것이 B 테이블상에서 외래 키(Foreign Key)를 들고 있는 건 Wallet이라는 의미
//나머지는 cascade로 fk 연결한다는 것
//orphanRemoval = true (고아 객체 제거) 라는 것 = User와 연결이 끊어지면, Wallet 삭제

//나머지도 같은 방식으로 작성한다

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Portfolio> portfolios = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeLog> tradeLogs = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inventory> inventories = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatHistory> chatHistories = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Watchlist> watchlists = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    //ArrayList 자체가 리스트 형이라서 java에서 배열 쓴다고 하면 이 리스트 쓴다고 생각하면 됨

// @Builder라는 어노테이션 자체가 복잡한 User 객체를 아주 쉽고 안전하게 만들기 위한 '조립 설명서(Builder)'를 정의하는 것이라고 할 수 있음


    //Builder가 없으면 생성자 파라미터 순서를 계속 외워서 객체 만들때 매번 User user = new User("a@a.com", "길동", "google", LocalDate.now(), "목", "용"); 이런식으로 인스턴스를 생성해줘야 함
//하지만 Builder를 설정해 둔다면
/*
User user = User.builder()
    .email("a@a.com")
    .nickname("길동")
    .provider("google")
    .birthDate(LocalDate.now())
    .sajuElement("목")
    .zodiacSign("용")
    .build();

    이런식으로 객체의 메서드로 불러버리면 메서드 순서 상관없이 해당 메서드만 불러버리면 되니까 상관이 없어진다

        //이렇게 쉽게 파라미터 대신에 메서드로 객체를 생성하려고 만든 것것
    */
    @Builder
    public User(String email, String nickname, String provider,
                LocalDate birthDate, String sajuElement, String zodiacSign) {
        this.email = email;
        this.nickname = nickname;
        this.provider = provider != null ? provider : "GOOGLE";
        this.birthDate = birthDate;
        this.sajuElement = sajuElement;
        this.zodiacSign = zodiacSign;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 비즈니스 메서드 ==========

    public void updateProfile(String nickname, String avatarUrl) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateSaju(String sajuElement, String zodiacSign) {
        this.sajuElement = sajuElement;
        this.zodiacSign = zodiacSign;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}