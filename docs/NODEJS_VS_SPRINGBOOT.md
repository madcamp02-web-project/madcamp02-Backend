# Node.js (Express) vs Spring Boot 구조 비교

> 두 프레임워크는 **동일한 역할**을 수행하지만, **코드 작성 방식과 내부 동작 방식**이 다릅니다.

---

## 목차

1. [핵심 차이점 요약](#1-핵심-차이점-요약)
2. [전체 요청 처리 흐름](#2-전체-요청-처리-흐름)
3. [계층별 상세 비교](#3-계층별-상세-비교)
   - [3.1 라우팅 (Routing)](#31-라우팅-routing)
   - [3.2 미들웨어 vs 필터/인터셉터](#32-미들웨어-vs-필터인터셉터)
   - [3.3 컨트롤러 (Controller)](#33-컨트롤러-controller)
   - [3.4 서비스 (Service)](#34-서비스-service)
   - [3.5 데이터 접근 (Repository/Model)](#35-데이터-접근-repositorymodel)
4. [의존성 주입 (Dependency Injection)](#4-의존성-주입-dependency-injection)
5. [트랜잭션 관리](#5-트랜잭션-관리)
6. [전체 코드 예시: 로그인 API](#6-전체-코드-예시-로그인-api)
7. [정리](#7-정리)

---

## 1. 핵심 차이점 요약

| 구분 | Node.js (Express) | Spring Boot |
|------|-------------------|-------------|
| **코드 스타일** | 명시적 (Explicit) | 선언적 (Declarative) |
| **흐름 제어** | 코드로 직접 연결 | 어노테이션(`@`)과 설정으로 연결 |
| **의존성 연결** | `require`/`import`로 수동 연결 | DI(의존성 주입)로 자동 연결 |
| **미들웨어** | 라우터에 직접 체이닝 | SecurityConfig에서 전역 설정 |
| **데이터 접근** | ORM 모델 객체 직접 사용 | Repository 인터페이스 사용 |

---

## 2. 전체 요청 처리 흐름

웹 요청은 다음 4단계를 거칩니다:

```
[클라이언트 요청] → [라우팅] → [인증/검증] → [비즈니스 로직] → [DB 조회] → [응답]
```

### 흐름 비교

#### Node.js (Express)
```
Request → Router → Middleware Chain → Controller → Service → Model → Response
          ↑         ↑                   ↑           ↑        ↑
       (코드에서    (코드에서           (파일 간    (파일 간  (ORM 객체
        직접 연결)   직접 체이닝)        require)   require)  직접 사용)
```

#### Spring Boot
```
Request → DispatcherServlet → Filter Chain → Controller → Service → Repository → Response
          ↑                    ↑              ↑            ↑         ↑
       (프레임워크 자동)     (Config 설정)   (@어노테이션) (DI 주입)  (인터페이스)
```

---

## 3. 계층별 상세 비교

### 3.1 라우팅 (Routing)

**역할**: 요청 URL을 적절한 처리 함수에 연결

#### Node.js (Express)
```javascript
// routes/userRoutes.js
const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');

// URL과 함수를 직접 연결
router.get('/users', userController.getAllUsers);
router.get('/users/:id', userController.getUserById);
router.post('/users', userController.createUser);

module.exports = router;
```

```javascript
// app.js (메인 파일)
const userRoutes = require('./routes/userRoutes');
app.use('/api', userRoutes);  // 라우터를 직접 등록
```

#### Spring Boot
```java
// UserController.java
@RestController                    // 이 클래스가 컨트롤러임을 선언
@RequestMapping("/api/users")      // 기본 URL 경로 설정
public class UserController {
    
    @GetMapping                    // GET /api/users
    public List<User> getAllUsers() { ... }
    
    @GetMapping("/{id}")           // GET /api/users/{id}
    public User getUserById(@PathVariable Long id) { ... }
    
    @PostMapping                   // POST /api/users
    public User createUser(@RequestBody UserDto dto) { ... }
}
```

**차이점**:
- Node.js: 라우터 파일에서 URL과 함수를 `코드로 연결`
- Spring: 메서드 위에 `@GetMapping` 등의 어노테이션으로 `선언`

---

### 3.2 미들웨어 vs 필터/인터셉터

**역할**: 요청이 컨트롤러에 도달하기 전 공통 처리 (인증, 로깅, 에러 핸들링 등)

#### Node.js (Express) - Middleware
```javascript
// middleware/auth.js
const jwt = require('jsonwebtoken');

const authMiddleware = (req, res, next) => {
    const token = req.headers.authorization?.split(' ')[1];
    
    if (!token) {
        return res.status(401).json({ error: 'No token' });
    }
    
    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        req.user = decoded;  // 검증된 유저 정보를 req에 저장
        next();              // 다음 미들웨어/컨트롤러로 이동
    } catch (error) {
        return res.status(401).json({ error: 'Invalid token' });
    }
};

module.exports = authMiddleware;
```

```javascript
// routes/userRoutes.js - 미들웨어 적용
const authMiddleware = require('../middleware/auth');

// 미들웨어가 중간에 위치 (눈에 보임)
router.get('/me', authMiddleware, userController.getMe);

// 여러 미들웨어 체이닝 가능
router.post('/admin', authMiddleware, adminMiddleware, adminController.action);
```

#### Spring Boot - Filter & SecurityConfig
```java
// SecurityConfig.java - 전역 보안 설정
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/signup").permitAll()  // 인증 불필요
                .anyRequest().authenticated()  // 나머지는 인증 필요
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

```java
// JwtAuthenticationFilter.java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) {
        String token = extractToken(request);
        
        if (token != null && jwtProvider.validateToken(token)) {
            Authentication auth = jwtProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        filterChain.doFilter(request, response);  // Node의 next()와 동일
    }
}
```

```java
// Controller에서 인증 정보 사용
@GetMapping("/me")
public UserDto getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
    // SecurityConfig의 필터를 통과해야 여기 도달
    // @AuthenticationPrincipal이 검증된 유저 정보를 자동으로 주입
    return userService.findById(userDetails.getUserId());
}
```

**차이점**:
| 구분 | Node.js | Spring Boot |
|------|---------|-------------|
| 미들웨어 위치 | 라우터에서 직접 체이닝 | SecurityConfig에서 전역 설정 |
| 가시성 | 코드에서 바로 보임 | 설정 파일을 확인해야 함 |
| 유저 정보 접근 | `req.user` | `@AuthenticationPrincipal` |
| 다음 단계 이동 | `next()` 호출 | `filterChain.doFilter()` 호출 |

---

### 3.3 컨트롤러 (Controller)

**역할**: HTTP 요청을 받아 적절한 서비스 호출 후 응답 반환

#### Node.js (Express)
```javascript
// controllers/userController.js
const userService = require('../services/userService');  // 직접 import

exports.getMe = async (req, res, next) => {
    try {
        const userId = req.user.id;  // 미들웨어에서 설정한 값
        const user = await userService.findById(userId);
        
        res.status(200).json({
            success: true,
            data: user
        });
    } catch (error) {
        next(error);  // 에러 핸들링 미들웨어로 전달
    }
};

exports.updateProfile = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const updateData = req.body;
        
        const updated = await userService.updateUser(userId, updateData);
        res.status(200).json({ success: true, data: updated });
    } catch (error) {
        next(error);
    }
};
```

#### Spring Boot
```java
// UserController.java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor  // final 필드 생성자 자동 생성
public class UserController {
    
    private final UserService userService;  // DI로 자동 주입
    
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        UserDto user = userService.findById(userDetails.getUserId());
        return ResponseEntity.ok(UserResponse.from(user));
    }
    
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UpdateProfileRequest request) {
        
        UserDto updated = userService.updateUser(userDetails.getUserId(), request);
        return ResponseEntity.ok(UserResponse.from(updated));
    }
}
```

**차이점**:
| 구분 | Node.js | Spring Boot |
|------|---------|-------------|
| 서비스 가져오기 | `require()` | `private final` + DI |
| 요청 데이터 | `req.body`, `req.params` | `@RequestBody`, `@PathVariable` |
| 응답 방식 | `res.json()` | `ResponseEntity.ok()` |
| 에러 처리 | `try-catch` + `next(error)` | `@ExceptionHandler` 또는 전역 핸들러 |

---

### 3.4 서비스 (Service)

**역할**: 비즈니스 로직 처리

#### Node.js (Express)
```javascript
// services/userService.js
const User = require('../models/User');  // 직접 import
const bcrypt = require('bcrypt');

exports.findById = async (userId) => {
    const user = await User.findByPk(userId);
    if (!user) {
        throw new Error('User not found');
    }
    return user;
};

exports.updateUser = async (userId, updateData) => {
    const user = await User.findByPk(userId);
    if (!user) {
        throw new Error('User not found');
    }
    
    await user.update(updateData);
    return user;
};
```

#### Spring Boot
```java
// UserService.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본적으로 읽기 전용
public class UserService {
    
    private final UserRepository userRepository;  // DI로 주입
    
    public UserDto findById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        return UserDto.from(user);
    }
    
    @Transactional  // 쓰기 작업은 트랜잭션 명시
    public UserDto updateUser(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        user.updateProfile(request.getNickname(), request.getBio());
        return UserDto.from(user);
    }
}
```

**차이점**:
| 구분 | Node.js | Spring Boot |
|------|---------|-------------|
| 클래스 선언 | 없음 (모듈 exports) | `@Service` 어노테이션 |
| 의존성 | `require()`로 직접 연결 | 생성자 주입 (DI) |
| 트랜잭션 | 수동 관리 또는 ORM 의존 | `@Transactional` 선언 |

---

### 3.5 데이터 접근 (Repository/Model)

**역할**: 데이터베이스 CRUD 작업

#### Node.js (Express) - Sequelize ORM
```javascript
// models/User.js
const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const User = sequelize.define('User', {
    id: {
        type: DataTypes.INTEGER,
        primaryKey: true,
        autoIncrement: true
    },
    email: {
        type: DataTypes.STRING,
        unique: true,
        allowNull: false
    },
    password: {
        type: DataTypes.STRING,
        allowNull: false
    },
    nickname: {
        type: DataTypes.STRING
    }
}, {
    tableName: 'users',
    timestamps: true
});

module.exports = User;
```

```javascript
// Service에서 사용
const User = require('../models/User');

// 모델 객체의 메서드 직접 호출
const user = await User.findByPk(id);
const users = await User.findAll({ where: { status: 'active' } });
const newUser = await User.create({ email, password });
await user.update({ nickname: 'new' });
await user.destroy();
```

#### Spring Boot - JPA Repository
```java
// User.java (Entity)
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    private String nickname;
    
    @Builder
    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
    
    public void updateProfile(String nickname, String bio) {
        this.nickname = nickname;
    }
}
```

```java
// UserRepository.java (Interface)
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 메서드 이름만으로 쿼리 자동 생성
    Optional<User> findByEmail(String email);
    
    List<User> findAllByStatus(String status);
    
    boolean existsByEmail(String email);
    
    // 복잡한 쿼리는 @Query로 직접 작성
    @Query("SELECT u FROM User u WHERE u.createdAt > :date")
    List<User> findRecentUsers(@Param("date") LocalDateTime date);
}
```

```java
// Service에서 사용
private final UserRepository userRepository;  // 인터페이스 주입

User user = userRepository.findById(id).orElseThrow(...);
List<User> users = userRepository.findAllByStatus("active");
User newUser = userRepository.save(User.builder().email(email).build());
// update는 @Transactional 내에서 엔티티 수정 시 자동 반영 (Dirty Checking)
userRepository.delete(user);
```

**차이점**:
| 구분 | Node.js (Sequelize) | Spring Boot (JPA) |
|------|---------------------|-------------------|
| 데이터 접근 객체 | Model 클래스 직접 사용 | Repository 인터페이스 |
| 쿼리 작성 | 메서드 파라미터로 조건 전달 | 메서드 이름으로 쿼리 생성 |
| 구현체 | 직접 정의 | Spring Data JPA가 자동 구현 |
| Update 방식 | `instance.update()` 호출 | 엔티티 수정 후 트랜잭션 커밋 시 자동 반영 |

---

## 4. 의존성 주입 (Dependency Injection)

### Node.js - 수동 연결
```javascript
// 파일마다 필요한 모듈을 직접 require
const userService = require('../services/userService');
const authService = require('../services/authService');
const emailService = require('../services/emailService');

// 사용
await userService.findById(id);
```

### Spring Boot - 자동 주입
```java
@Service
@RequiredArgsConstructor  // Lombok: final 필드 생성자 생성
public class AuthService {
    
    // 선언만 하면 Spring이 알아서 객체 주입
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    
    // new 키워드 없이 바로 사용
    public void login() {
        userRepository.findByEmail(...);
        passwordEncoder.matches(...);
    }
}
```

**DI의 장점**:
1. **결합도 감소**: 구현체 변경 시 코드 수정 최소화
2. **테스트 용이**: Mock 객체로 쉽게 교체 가능
3. **생명주기 관리**: Spring이 객체 생성/소멸 관리

---

## 5. 트랜잭션 관리

### Node.js - 수동 관리
```javascript
const sequelize = require('../config/database');

exports.transfer = async (fromId, toId, amount) => {
    const t = await sequelize.transaction();  // 트랜잭션 시작
    
    try {
        await Account.decrement('balance', { 
            by: amount, 
            where: { id: fromId },
            transaction: t 
        });
        
        await Account.increment('balance', { 
            by: amount, 
            where: { id: toId },
            transaction: t 
        });
        
        await t.commit();  // 성공 시 커밋
    } catch (error) {
        await t.rollback();  // 실패 시 롤백
        throw error;
    }
};
```

### Spring Boot - 선언적 관리
```java
@Service
@RequiredArgsConstructor
public class AccountService {
    
    private final AccountRepository accountRepository;
    
    @Transactional  // 이 어노테이션 하나로 트랜잭션 관리
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepository.findById(fromId).orElseThrow(...);
        Account to = accountRepository.findById(toId).orElseThrow(...);
        
        from.withdraw(amount);
        to.deposit(amount);
        
        // 메서드 정상 종료 시 자동 커밋
        // 예외 발생 시 자동 롤백
    }
}
```

---

## 6. 전체 코드 예시: 로그인 API

### Node.js (Express) 전체 구조

```
src/
├── routes/
│   └── authRoutes.js       # 라우팅 정의
├── middleware/
│   └── validate.js         # 입력 검증 미들웨어
├── controllers/
│   └── authController.js   # 요청/응답 처리
├── services/
│   └── authService.js      # 비즈니스 로직
└── models/
    └── User.js             # DB 모델
```

```javascript
// routes/authRoutes.js
const router = require('express').Router();
const validate = require('../middleware/validate');
const authController = require('../controllers/authController');
const { loginSchema } = require('../validators/authValidator');

router.post('/login', validate(loginSchema), authController.login);

module.exports = router;
```

```javascript
// middleware/validate.js
const validate = (schema) => (req, res, next) => {
    const { error } = schema.validate(req.body);
    if (error) {
        return res.status(400).json({ error: error.details[0].message });
    }
    next();
};

module.exports = validate;
```

```javascript
// controllers/authController.js
const authService = require('../services/authService');

exports.login = async (req, res, next) => {
    try {
        const { email, password } = req.body;
        const result = await authService.login(email, password);
        
        res.status(200).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};
```

```javascript
// services/authService.js
const User = require('../models/User');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');

exports.login = async (email, password) => {
    const user = await User.findOne({ where: { email } });
    if (!user) {
        throw new Error('User not found');
    }
    
    const isValid = await bcrypt.compare(password, user.password);
    if (!isValid) {
        throw new Error('Invalid password');
    }
    
    const accessToken = jwt.sign(
        { id: user.id, email: user.email },
        process.env.JWT_SECRET,
        { expiresIn: '1h' }
    );
    
    return { accessToken, user: { id: user.id, email: user.email } };
};
```

---

### Spring Boot 전체 구조

```
src/main/java/com/example/
├── controller/
│   └── AuthController.java      # 요청/응답 처리 + 라우팅
├── service/
│   └── AuthService.java         # 비즈니스 로직
├── repository/
│   └── UserRepository.java      # DB 접근 인터페이스
├── entity/
│   └── User.java                # DB 엔티티
├── dto/
│   ├── LoginRequest.java        # 요청 DTO
│   └── LoginResponse.java       # 응답 DTO
├── security/
│   ├── SecurityConfig.java      # 보안 설정 (미들웨어 역할)
│   └── JwtProvider.java         # JWT 처리
└── exception/
    └── GlobalExceptionHandler.java  # 전역 에러 핸들러
```

```java
// controller/AuthController.java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
```

```java
// dto/LoginRequest.java
@Getter
@NoArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    private String email;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
```

```java
// service/AuthService.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new AuthException("User not found"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("Invalid password");
        }
        
        String accessToken = jwtProvider.createAccessToken(user.getId());
        
        return LoginResponse.builder()
            .accessToken(accessToken)
            .userId(user.getId())
            .email(user.getEmail())
            .build();
    }
}
```

```java
// repository/UserRepository.java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

---

## 7. 정리

### 1:1 매핑 정리

| 기능 | Node.js (Express) | Spring Boot |
|------|-------------------|-------------|
| 라우팅 | `router.get('/path', handler)` | `@GetMapping("/path")` |
| URL 파라미터 | `req.params.id` | `@PathVariable Long id` |
| Query 파라미터 | `req.query.page` | `@RequestParam int page` |
| Request Body | `req.body` | `@RequestBody Dto dto` |
| 미들웨어 | `router.use(middleware)` | `SecurityConfig` + Filter |
| 인증 유저 정보 | `req.user` | `@AuthenticationPrincipal` |
| 응답 | `res.json(data)` | `ResponseEntity.ok(data)` |
| 상태 코드 | `res.status(201)` | `ResponseEntity.status(201)` |
| 에러 처리 | `next(error)` + Error Middleware | `@ExceptionHandler` |
| 의존성 연결 | `require('./service')` | `private final Service` + DI |
| 트랜잭션 | `sequelize.transaction()` | `@Transactional` |

### 개발 철학 차이

#### Node.js (Express)
- **명시적**: 코드를 보면 흐름이 바로 보임
- **유연함**: 구조를 자유롭게 설계 가능
- **낮은 진입장벽**: 빠르게 시작 가능
- **주의 필요**: 일관성 유지를 위해 팀 컨벤션 필요

#### Spring Boot
- **선언적**: 어노테이션으로 의도를 선언
- **규격화**: 정해진 패턴과 구조를 따름
- **자동화**: DI, 트랜잭션 등 프레임워크가 처리
- **학습 곡선**: 내부 동작 원리 이해 필요

---

### 추가 참고 자료

- [Spring Boot 공식 문서](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Express.js 공식 문서](https://expressjs.com/)
- [Spring Security 가이드](https://docs.spring.io/spring-security/reference/)
- [Sequelize ORM 문서](https://sequelize.org/)
