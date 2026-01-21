# 백엔드 페르소나 설계 문서

## 1. Entity 설계

### 1.1 Persona Entity

```java
package com.madcamp02.domain.persona;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "personas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "persona_id")
    private Long personaId;
    
    @Column(name = "persona_type", nullable = false, unique = true, length = 20)
    @Enumerated(EnumType.STRING)
    private PersonaType personaType;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;
    
    @Column(name = "adapter_path", length = 255)
    private String adapterPath;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

public enum PersonaType {
    SAGE("sage", "투자 도사"),
    ANALYST("analyst", "데이터 분석가"),
    FRIEND("friend", "친구 조언자");
    
    private final String code;
    private final String displayName;
    
    PersonaType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
    
    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
}
```

### 1.2 User Entity 확장

```java
// User.java에 추가할 필드

@Column(name = "default_persona", length = 20)
@Enumerated(EnumType.STRING)
@Builder.Default
private PersonaType defaultPersona = PersonaType.SAGE;

@Column(name = "persona_preferences", columnDefinition = "JSONB")
@Type(JsonType.class)
private Map<String, Object> personaPreferences;
```

### 1.3 ChatHistory Entity 확장

```java
// ChatHistory.java에 추가할 필드

@Column(name = "persona_type", length = 20)
@Enumerated(EnumType.STRING)
private PersonaType personaType;

@Column(name = "persona_rating")
@Min(1)
@Max(5)
private Integer personaRating;
```

## 2. Repository 설계

### 2.1 PersonaRepository

```java
package com.madcamp02.domain.persona;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {
    Optional<Persona> findByPersonaType(PersonaType personaType);
    List<Persona> findByIsActiveTrueOrderByPersonaIdAsc();
    boolean existsByPersonaType(PersonaType personaType);
}
```

## 3. Service 설계

### 3.1 PersonaService

```java
package com.madcamp02.service;

import com.madcamp02.domain.persona.Persona;
import com.madcamp02.domain.persona.PersonaType;
import com.madcamp02.domain.persona.PersonaRepository;
import com.madcamp02.domain.user.User;
import com.madcamp02.domain.user.UserRepository;
import com.madcamp02.dto.persona.PersonaResponse;
import com.madcamp02.dto.persona.PersonasResponse;
import com.madcamp02.dto.persona.PersonaStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonaService {
    private final PersonaRepository personaRepository;
    private final UserRepository userRepository;
    
    /**
     * 활성화된 페르소나 목록 조회
     */
    @Transactional(readOnly = true)
    public PersonasResponse getActivePersonas() {
        List<Persona> personas = personaRepository.findByIsActiveTrueOrderByPersonaIdAsc();
        List<PersonaResponse> items = personas.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return PersonasResponse.builder()
            .items(items)
            .build();
    }
    
    /**
     * 사용자 기본 페르소나 조회
     */
    @Transactional(readOnly = true)
    public PersonaType getUserDefaultPersona(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        
        return user.getDefaultPersona() != null 
            ? user.getDefaultPersona() 
            : PersonaType.SAGE; // 기본값
    }
    
    /**
     * 사용자 기본 페르소나 설정
     */
    @Transactional
    public void setUserDefaultPersona(Long userId, PersonaType personaType) {
        // 페르소나 존재 확인
        if (!personaRepository.existsByPersonaType(personaType)) {
            throw new IllegalArgumentException("Invalid persona type: " + personaType);
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        
        user.setDefaultPersona(personaType);
        userRepository.save(user);
        
        log.info("User {} default persona set to {}", userId, personaType);
    }
    
    /**
     * 페르소나별 사용 통계
     */
    @Transactional(readOnly = true)
    public PersonaStatsResponse getPersonaStats(Long userId) {
        // ChatHistory에서 페르소나별 사용 횟수 집계
        // (구현은 ChatHistoryRepository에 통계 쿼리 추가 필요)
        return PersonaStatsResponse.builder()
            .totalChats(0) // TODO: 실제 통계 구현
            .personaUsage(Map.of()) // TODO: 페르소나별 사용 횟수
            .build();
    }
    
    private PersonaResponse toResponse(Persona persona) {
        return PersonaResponse.builder()
            .personaId(persona.getPersonaId())
            .personaType(persona.getPersonaType())
            .name(persona.getName())
            .description(persona.getDescription())
            .isActive(persona.getIsActive())
            .build();
    }
}
```

## 4. DTO 설계

### 4.1 PersonaResponse

```java
package com.madcamp02.dto.persona;

import com.madcamp02.domain.persona.PersonaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaResponse {
    private Long personaId;
    private PersonaType personaType;
    private String name;
    private String description;
    private Boolean isActive;
}
```

### 4.2 PersonasResponse

```java
package com.madcamp02.dto.persona;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonasResponse {
    private List<PersonaResponse> items;
}
```

### 4.3 SetPersonaRequest

```java
package com.madcamp02.dto.persona;

import com.madcamp02.domain.persona.PersonaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetPersonaRequest {
    private PersonaType personaType;
}
```

### 4.4 ChatRequest 확장

```java
// ChatRequest.java에 추가
private PersonaType persona; // 선택적, 없으면 사용자 기본 페르소나 사용
```

## 5. Controller 설계

### 5.1 PersonaController

```java
package com.madcamp02.controller;

import com.madcamp02.domain.persona.PersonaType;
import com.madcamp02.dto.persona.*;
import com.madcamp02.service.PersonaService;
import com.madcamp02.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/personas")
@RequiredArgsConstructor
public class PersonaController {
    private final PersonaService personaService;
    
    /**
     * 활성화된 페르소나 목록 조회
     * GET /api/v1/personas
     */
    @GetMapping
    public ResponseEntity<PersonasResponse> getPersonas() {
        PersonasResponse response = personaService.getActivePersonas();
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자 기본 페르소나 조회
     * GET /api/v1/personas/me
     */
    @GetMapping("/me")
    public ResponseEntity<PersonaResponse> getMyPersona(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        PersonaType personaType = personaService.getUserDefaultPersona(principal.getUserId());
        // Persona 엔티티 조회 후 Response 변환
        PersonaResponse response = personaService.getPersonaByType(personaType);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자 기본 페르소나 설정
     * PUT /api/v1/personas/me
     */
    @PutMapping("/me")
    public ResponseEntity<PersonaResponse> setMyPersona(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestBody SetPersonaRequest request
    ) {
        personaService.setUserDefaultPersona(principal.getUserId(), request.getPersonaType());
        PersonaResponse response = personaService.getPersonaByType(request.getPersonaType());
        return ResponseEntity.ok(response);
    }
}
```

### 5.2 ChatController 확장

```java
// ChatController.java 수정

@PostMapping("/ask")
public ResponseEntity<SseEmitter> askChat(
    @AuthenticationPrincipal UserPrincipal principal,
    @RequestBody ChatRequest request
) {
    Long userId = principal.getUserId();
    
    // 페르소나 선택: 요청에 있으면 사용, 없으면 사용자 기본 페르소나
    PersonaType selectedPersona = request.getPersona() != null
        ? request.getPersona()
        : personaService.getUserDefaultPersona(userId);
    
    // 컨텍스트 구성
    AiChatContext context = buildContext(userId);
    
    // AI Gateway 호출
    AiChatRequest aiRequest = AiChatRequest.builder()
        .useCase(request.getUseCase())
        .persona(selectedPersona)
        .message(request.getMessage())
        .context(context)
        .options(request.getOptions())
        .build();
    
    // SSE 스트리밍 응답
    SseEmitter emitter = new SseEmitter(60000L);
    
    // 비동기 처리
    aiClient.chatStream(aiRequest, emitter, (response) -> {
        // 응답 저장 시 페르소나 정보 포함
        chatService.saveHistory(
            userId,
            request.getUseCase(),
            request.getMessage(),
            response.getContent(),
            selectedPersona,
            response.getModel()
        );
    });
    
    return ResponseEntity.ok(emitter);
}
```

## 6. AiClient 확장

### 6.1 AiChatRequest 확장

```java
package com.madcamp02.dto.ai;

import com.madcamp02.domain.persona.PersonaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {
    private String useCase;
    private Integer userId;
    private String message;
    private PersonaType persona; // 페르소나 추가
    private Map<String, Object> context;
    private Map<String, Object> options;
}
```

### 6.2 AiClient 메서드 확장

```java
package com.madcamp02.external.ai;

// 기존 메서드에 persona 파라미터 추가
public AiChatResponse chat(AiChatRequest request) {
    // AI Gateway 호출 시 persona 포함
    // ...
}
```

## 7. 금융 데이터 통합 (신규)

### 7.1 컨텍스트 구성 확장

`ChatService`에서 AI Gateway로 전달하는 컨텍스트에 금융 데이터를 동적으로 추가합니다.

```java
@Service
public class ChatService {
    
    private final MarketService marketService;
    private final StockService stockService;
    
    /**
     * 금융 데이터를 포함한 확장 컨텍스트 구성
     */
    private Map<String, Object> buildEnhancedContext(Long userId, String message) {
        Map<String, Object> context = new HashMap<>();
        
        // 기존: 포트폴리오 + 사주
        context.put("portfolio", getPortfolioContext(userId));
        context.put("saju", getSajuContext(userId));
        
        // 신규: 금융 데이터 (질문 내용에 따라 동적으로 추가)
        if (containsStockMention(message)) {
            List<String> mentionedTickers = extractTickers(message);
            List<Map<String, Object>> stockData = mentionedTickers.stream()
                .map(ticker -> {
                    StockQuoteResponse quote = stockService.getQuote(ticker);
                    return Map.of(
                        "ticker", ticker,
                        "currentPrice", quote.getCurrentPrice(),
                        "changePercent", quote.getChangePercent()
                    );
                })
                .collect(Collectors.toList());
            context.put("stocks", stockData);
        }
        
        if (needsMarketData(message)) {
            MarketIndicesResponse indices = marketService.getIndices();
            context.put("market", Map.of("indices", indices.getItems()));
        }
        
        if (needsNews(message)) {
            MarketNewsResponse news = marketService.getNews();
            context.put("news", Map.of("items", news.getItems().subList(0, Math.min(3, news.getItems().size()))));
        }
        
        return context;
    }
    
    private boolean containsStockMention(String message) {
        // 종목 티커 패턴 감지
        Pattern tickerPattern = Pattern.compile("\\b([A-Z]{1,5})\\b");
        return tickerPattern.matcher(message.toUpperCase()).find();
    }
    
    private List<String> extractTickers(String message) {
        // 질문에서 언급된 종목 티커 추출
        List<String> tickers = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b([A-Z]{1,5})\\b");
        Matcher matcher = pattern.matcher(message.toUpperCase());
        while (matcher.find()) {
            tickers.add(matcher.group(1));
        }
        return tickers;
    }
    
    private boolean needsMarketData(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("시장") || lowerMessage.contains("지수") || 
               lowerMessage.contains("다우") || lowerMessage.contains("나스닥");
    }
    
    private boolean needsNews(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("뉴스") || lowerMessage.contains("소식") || 
               lowerMessage.contains("이슈");
    }
}
```

**상세 설계**: `docs/AI_FINANCIAL_DATA_INTEGRATION.md` 참조

## 8. ChatService 확장

### 7.1 saveHistory 메서드 확장

```java
public void saveHistory(
    Long userId,
    String useCase,
    String question,
    String response,
    PersonaType personaType,  // 추가
    String modelId
) {
    ChatHistory history = ChatHistory.builder()
        .userId(userId)
        .useCase(useCase)
        .question(question)
        .response(response)
        .personaType(personaType)  // 추가
        .model(modelId)
        .createdAt(LocalDateTime.now())
        .build();
    
    chatHistoryRepository.save(history);
}
```

## 8. 에러 코드

```java
// ErrorCode.java에 추가
AI_005("AI_005", "Invalid persona type", 400),
AI_006("AI_006", "Persona not found", 404),
```

## 9. 구현 순서

1. Flyway V9 마이그레이션 실행
2. Persona Entity 및 Repository 구현
3. User, ChatHistory Entity 확장
4. PersonaService 구현
5. PersonaController 구현
6. ChatController 확장
7. AiClient 확장
8. ChatService 확장 (금융 데이터 통합 포함)
9. 통합 테스트

## 10. 관련 문서

- **금융 데이터 통합**: `docs/AI_FINANCIAL_DATA_INTEGRATION.md` - 실제 금융 API 데이터를 활용한 대화 및 Fine-tuning
- **ChatHistory 데이터 수집**: `docs/BACKEND_CHAT_HISTORY_API.md` - Fine-tuning용 실제 대화 데이터 수집 API
- **AI 서버 명세**: `docs/AI_SERVER_SPEC.md` - AI 서버 전체 아키텍처 및 API 명세
- **백엔드 개발 계획**: `docs/BACKEND_DEVELOPMENT_PLAN.md` - 백엔드 개발 계획 및 AI 연동 상세
- **프론트엔드 개발 계획**: `docs/FRONTEND_DEVELOPMENT_PLAN.md` - 프론트엔드 개발 계획 및 `/oracle` 페이지 연동
- **프론트엔드 API 연결**: `docs/FRONTEND_API_WIRING.md` - 프론트엔드 API 연결 명세
- **Fine-tuning 가이드**: `ai-server/fine-tuning/README.md` - LoRA Fine-tuning 전체 프로세스 가이드
