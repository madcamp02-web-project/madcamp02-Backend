# ChatHistory 데이터 수집 API 설계

Fine-tuning을 위해 실제 사용자 대화 데이터를 수집할 수 있는 API 설계 문서입니다.

## 1. API 엔드포인트

### 1.1 관리자용 ChatHistory 조회 API

**엔드포인트**: `GET /api/v1/admin/chat-history`

**권한**: 관리자 권한 필요 (`ROLE_ADMIN`)

**목적**: Fine-tuning 데이터 수집을 위해 페르소나별, 날짜별 ChatHistory를 조회합니다.

**Query Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `personaType` | String | 아니오 | 페르소나 타입 (`sage`, `analyst`, `friend`) |
| `startDate` | String (ISO 8601) | 아니오 | 시작 날짜 (예: `2026-01-01T00:00:00`) |
| `endDate` | String (ISO 8601) | 아니오 | 종료 날짜 (예: `2026-01-31T23:59:59`) |
| `limit` | Integer | 아니오 | 최대 조회 개수 (기본값: 1000) |
| `offset` | Integer | 아니오 | 오프셋 (기본값: 0) |
| `minRating` | Integer | 아니오 | 최소 페르소나 평점 (1-5, 필터링용) |

**응답 형식**:

```json
{
  "items": [
    {
      "id": 1,
      "userId": 123,
      "personaType": "sage",
      "question": "불(火) 오행인데 어떤 종목이 좋을까?",
      "response": "허허, 자네의 사주는 불 기운이 왕성하니...",
      "personaRating": 4,
      "useCase": "oracle",
      "createdAt": "2026-01-21T10:30:00"
    }
  ],
  "total": 150,
  "limit": 1000,
  "offset": 0
}
```

**보안 고려사항**:
- 개인정보 보호: `userId`는 해시화하거나 제외할 수 있음
- PII 필터링: 질문/응답에서 개인정보가 포함된 경우 필터링 옵션 제공
- 데이터 익명화: 실제 사용자 정보는 제거하고 대화 내용만 제공

### 1.2 Fine-tuning용 데이터 내보내기 API

**엔드포인트**: `GET /api/v1/admin/chat-history/export`

**권한**: 관리자 권한 필요

**목적**: Fine-tuning용 JSONL 형식으로 직접 내보내기

**Query Parameters**: 위와 동일

**응답**: 
- Content-Type: `application/x-ndjson` (JSONL)
- 파일 다운로드 형식

**응답 예시** (JSONL):

```jsonl
{"question": "불(火) 오행인데 어떤 종목이 좋을까?", "answer": "허허, 자네의 사주는 불 기운이 왕성하니..."}
{"question": "오늘 투자 운세는?", "answer": "오늘은 금(金) 기운이 강하여..."}
```

## 2. 백엔드 구현 가이드

### 2.1 ChatHistoryRepository 확장

```java
@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    
    // 페르소나별 조회
    @Query("SELECT ch FROM ChatHistory ch WHERE ch.personaType = :personaType " +
           "AND ch.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ch.createdAt DESC")
    Page<ChatHistory> findByPersonaTypeAndDateRange(
        @Param("personaType") PersonaType personaType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // 평점 필터링 포함
    @Query("SELECT ch FROM ChatHistory ch WHERE ch.personaType = :personaType " +
           "AND ch.personaRating >= :minRating " +
           "AND ch.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY ch.createdAt DESC")
    Page<ChatHistory> findByPersonaTypeWithRating(
        @Param("personaType") PersonaType personaType,
        @Param("minRating") Integer minRating,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // 통계 조회
    @Query("SELECT ch.personaType, COUNT(ch) FROM ChatHistory ch " +
           "WHERE ch.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY ch.personaType")
    List<Object[]> countByPersonaType(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
```

### 2.2 ChatHistoryController (관리자용)

```java
@RestController
@RequestMapping("/api/v1/admin/chat-history")
@PreAuthorize("hasRole('ADMIN')")
public class ChatHistoryAdminController {
    
    private final ChatHistoryService chatHistoryService;
    
    @GetMapping
    public ResponseEntity<ChatHistoryListResponse> getChatHistory(
        @RequestParam(required = false) PersonaType personaType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestParam(required = false) Integer minRating,
        @RequestParam(defaultValue = "1000") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        // 날짜 기본값 설정 (최근 3개월)
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(3);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<ChatHistory> historyPage = chatHistoryService.findChatHistory(
            personaType, startDate, endDate, minRating, pageable
        );
        
        return ResponseEntity.ok(ChatHistoryListResponse.from(historyPage));
    }
    
    @GetMapping("/export")
    public ResponseEntity<Resource> exportChatHistory(
        @RequestParam(required = false) PersonaType personaType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestParam(required = false) Integer minRating
    ) {
        // JSONL 형식으로 내보내기
        String jsonlContent = chatHistoryService.exportToJsonl(
            personaType, startDate, endDate, minRating
        );
        
        ByteArrayResource resource = new ByteArrayResource(jsonlContent.getBytes(StandardCharsets.UTF_8));
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=chat_history.jsonl")
            .contentType(MediaType.parseMediaType("application/x-ndjson"))
            .body(resource);
    }
}
```

### 2.3 ChatHistoryService 확장

```java
@Service
public class ChatHistoryService {
    
    public Page<ChatHistory> findChatHistory(
        PersonaType personaType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer minRating,
        Pageable pageable
    ) {
        if (minRating != null && minRating > 0) {
            return chatHistoryRepository.findByPersonaTypeWithRating(
                personaType, minRating, startDate, endDate, pageable
            );
        } else {
            return chatHistoryRepository.findByPersonaTypeAndDateRange(
                personaType, startDate, endDate, pageable
            );
        }
    }
    
    public String exportToJsonl(
        PersonaType personaType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer minRating
    ) {
        List<ChatHistory> histories = findChatHistory(
            personaType, startDate, endDate, minRating, Pageable.unpaged()
        ).getContent();
        
        StringBuilder jsonl = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();
        
        for (ChatHistory history : histories) {
            Map<String, String> data = Map.of(
                "question", history.getQuestion(),
                "answer", history.getResponse()
            );
            
            try {
                jsonl.append(mapper.writeValueAsString(data)).append("\n");
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize chat history: {}", history.getId(), e);
            }
        }
        
        return jsonl.toString();
    }
}
```

## 3. 데이터 수집 스크립트 사용법

### 3.1 Python 스크립트 실행

```bash
# 특정 페르소나 데이터 수집
python ai-server/fine-tuning/scripts/collect_chat_history.py \
    --base-url http://localhost:8080 \
    --token YOUR_ADMIN_TOKEN \
    --persona sage \
    --output ./data/sage_raw.jsonl \
    --limit 1000

# 모든 페르소나 데이터 수집
python ai-server/fine-tuning/scripts/collect_chat_history.py \
    --base-url http://localhost:8080 \
    --token YOUR_ADMIN_TOKEN \
    --all \
    --output-dir ./data \
    --start-date 2026-01-01 \
    --end-date 2026-01-31

# 평점 4점 이상만 수집 (API 확장 필요)
python ai-server/fine-tuning/scripts/collect_chat_history.py \
    --base-url http://localhost:8080 \
    --token YOUR_ADMIN_TOKEN \
    --persona analyst \
    --min-rating 4 \
    --output ./data/analyst_high_quality.jsonl
```

### 3.2 직접 API 호출

```bash
# cURL로 데이터 수집
curl -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
     "http://localhost:8080/api/v1/admin/chat-history?personaType=sage&limit=1000" \
     > sage_raw.jsonl

# JSONL 형식으로 직접 다운로드
curl -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
     "http://localhost:8080/api/v1/admin/chat-history/export?personaType=sage" \
     -o sage_raw.jsonl
```

## 4. 보안 및 개인정보 보호

### 4.1 PII 필터링

- 개인정보(이름, 전화번호, 이메일 등)가 포함된 대화는 자동으로 제외
- 사용자 ID는 해시화하거나 제외
- 민감한 금융 정보(계좌번호, 카드번호 등) 필터링

### 4.2 데이터 익명화

- 실제 사용자 정보는 제거
- 대화 내용만 Fine-tuning에 사용
- 통계 목적으로만 사용자 ID 집계

### 4.3 접근 제어

- 관리자 권한만 접근 가능
- API 호출 로그 기록
- 데이터 다운로드 이력 추적

## 5. 다음 단계

1. **백엔드 구현**: `ChatHistoryAdminController` 및 관련 Service 구현
2. **데이터 수집**: 실제 사용자 대화 데이터가 충분히 쌓일 때까지 대기
3. **품질 필터링**: 평점이 높은 대화만 선별하여 Fine-tuning에 사용
4. **데이터 전처리**: `prepare_dataset.py`로 JSONL 형식 변환
5. **Fine-tuning 실행**: `train_lora.py`로 학습 진행

## 6. 관련 문서

- **금융 데이터 통합**: `docs/AI_FINANCIAL_DATA_INTEGRATION.md` - 실제 금융 API 데이터를 활용한 대화 생성
- **Fine-tuning 가이드**: `ai-server/fine-tuning/README.md` - 전체 Fine-tuning 프로세스
- **페르소나 시스템 설계**: `docs/BACKEND_PERSONA_DESIGN.md` - 백엔드 페르소나 시스템 상세 설계
- **AI 서버 명세**: `docs/AI_SERVER_SPEC.md` - AI 서버 전체 아키텍처 및 API 명세
- **백엔드 개발 계획**: `docs/BACKEND_DEVELOPMENT_PLAN.md` - 백엔드 개발 계획 및 AI 연동 상세
