# 금융 데이터 통합 가이드

AI 대화 및 Fine-tuning에 실제 금융 데이터를 활용하는 방법을 설명합니다.

## 1. 개요

현재 시스템은 Spring Backend의 여러 금융 API를 제공하고 있으며, 이를 AI 대화 컨텍스트에 통합하여 더 정확하고 실용적인 조언을 제공할 수 있습니다.

### 1.1 사용 가능한 금융 API

| API | 엔드포인트 | 설명 | 데이터 |
|-----|-----------|------|--------|
| **Market Indices** | `GET /api/v1/market/indices` | 주요 지수 (SPY, QQQ, DIA) | 현재가, 변동률 |
| **Market News** | `GET /api/v1/market/news` | 최신 시장 뉴스 | 헤드라인, 요약, 링크 |
| **Market Movers** | `GET /api/v1/market/movers` | 급등/급락/거래량 상위 | 종목, 가격, 변동률 |
| **Stock Quote** | `GET /api/v1/stock/quote/{ticker}` | 종목 현재가/호가 | 현재가, 전일종가, 변동률 |
| **Stock Candles** | `GET /api/v1/stock/candles/{ticker}` | 캔들 차트 데이터 | OHLCV, 날짜별 데이터 |
| **Portfolio** | `GET /api/v1/trade/portfolio` | 사용자 포트폴리오 | 보유 종목, 손익, 비중 |
| **Watchlist** | `GET /api/v1/user/watchlist` | 관심 종목 | 종목 리스트 |

## 2. 실제 대화에 금융 데이터 통합

### 2.1 Spring Backend 컨텍스트 구성 확장

`ChatController`에서 AI Gateway로 전달하는 컨텍스트에 금융 데이터를 추가합니다.

**현재 구조** (`ChatController`):
```java
// 기존: 포트폴리오 + 사주만 전달
AiChatRequest aiRequest = AiChatRequest.builder()
    .useCase(request.getUseCase())
    .persona(selectedPersona)
    .message(request.getMessage())
    .context(buildContext(userId))  // 포트폴리오 + 사주
    .build();
```

**개선된 구조**:
```java
// 확장: 금융 데이터도 포함
AiChatRequest aiRequest = AiChatRequest.builder()
    .useCase(request.getUseCase())
    .persona(selectedPersona)
    .message(request.getMessage())
    .context(buildEnhancedContext(userId, request.getMessage()))  // 금융 데이터 포함
    .build();

private Map<String, Object> buildEnhancedContext(Long userId, String message) {
    Map<String, Object> context = new HashMap<>();
    
    // 기존: 포트폴리오 + 사주
    context.put("portfolio", getPortfolioContext(userId));
    context.put("saju", getSajuContext(userId));
    
    // 신규: 금융 데이터 (질문 내용에 따라 동적으로 추가)
    if (containsStockMention(message)) {
        // 종목이 언급된 경우 해당 종목 데이터 추가
        List<String> mentionedTickers = extractTickers(message);
        context.put("stocks", getStockData(mentionedTickers));
    }
    
    if (needsMarketData(message)) {
        // 시장 데이터가 필요한 경우 추가
        context.put("market", getMarketData());
    }
    
    if (needsNews(message)) {
        // 뉴스가 필요한 경우 추가
        context.put("news", getMarketNews());
    }
    
    return context;
}
```

### 2.2 AI Gateway 컨텍스트 포맷팅 확장

`ai-gateway/main.py`의 `format_context()` 함수를 확장하여 금융 데이터를 포함합니다.

**현재 구현**:
```python
def format_context(context: Dict[str, Any]) -> str:
    """컨텍스트를 프롬프트 형식으로 변환"""
    parts = []
    if context.get("portfolio"):
        # 포트폴리오 정보만 처리
    if context.get("saju"):
        # 사주 정보만 처리
    return "\n".join(parts)
```

**개선된 구현**:
```python
def format_context(context: Dict[str, Any]) -> str:
    """컨텍스트를 프롬프트 형식으로 변환 (금융 데이터 포함)"""
    parts = []
    
    # 기존: 포트폴리오 + 사주
    if context.get("portfolio"):
        portfolio = context["portfolio"]
        if portfolio.get("summary"):
            summary = portfolio["summary"]
            parts.append(f"총 자산: {summary.get('totalEquity', 'N/A')} {summary.get('currency', 'USD')}")
            parts.append(f"현금 비중: {summary.get('cashBalance', 0) / summary.get('totalEquity', 1) * 100:.1f}%")
        if portfolio.get("positions"):
            top_positions = ", ".join([pos.get("ticker", "") for pos in portfolio["positions"][:5]])
            parts.append(f"주요 보유 종목: {top_positions}")
    
    if context.get("saju"):
        saju = context["saju"]
        parts.append(f"사주 오행: {saju.get('element', 'N/A')}")
        parts.append(f"띠: {saju.get('zodiacSign', 'N/A')}")
    
    # 신규: 금융 데이터
    if context.get("market"):
        market = context["market"]
        if market.get("indices"):
            indices_info = []
            for idx in market["indices"][:3]:  # 상위 3개 지수
                indices_info.append(f"{idx.get('symbol', 'N/A')}: {idx.get('price', 'N/A')} ({idx.get('changePercent', 0):.2f}%)")
            parts.append(f"주요 지수: {', '.join(indices_info)}")
    
    if context.get("stocks"):
        stocks = context["stocks"]
        for stock in stocks:
            ticker = stock.get("ticker", "N/A")
            price = stock.get("currentPrice", "N/A")
            change = stock.get("changePercent", 0)
            parts.append(f"{ticker} 현재가: ${price} ({change:+.2f}%)")
    
    if context.get("news"):
        news = context["news"]
        if news.get("items"):
            top_news = news["items"][:3]  # 최신 뉴스 3개
            news_headlines = [item.get("headline", "") for item in top_news]
            parts.append(f"최신 시장 뉴스: {', '.join(news_headlines)}")
    
    return "\n".join(parts)
```

### 2.3 질문 분석 및 동적 데이터 로딩

사용자 질문을 분석하여 필요한 금융 데이터만 동적으로 로드합니다.

**구현 예시** (`ChatService`):
```java
private boolean containsStockMention(String message) {
    // 종목 티커 패턴 감지 (예: "AAPL", "애플", "테슬라" 등)
    Pattern tickerPattern = Pattern.compile("\\b([A-Z]{1,5})\\b|애플|테슬라|구글|마이크로소프트");
    return tickerPattern.matcher(message.toUpperCase()).find();
}

private List<String> extractTickers(String message) {
    // 질문에서 언급된 종목 티커 추출
    List<String> tickers = new ArrayList<>();
    // 간단한 패턴 매칭 또는 NLP 모델 사용
    return tickers;
}

private boolean needsMarketData(String message) {
    // "시장", "지수", "다우", "나스닥" 등의 키워드 감지
    String lowerMessage = message.toLowerCase();
    return lowerMessage.contains("시장") || lowerMessage.contains("지수") || 
           lowerMessage.contains("다우") || lowerMessage.contains("나스닥");
}

private boolean needsNews(String message) {
    // "뉴스", "소식", "이슈" 등의 키워드 감지
    String lowerMessage = message.toLowerCase();
    return lowerMessage.contains("뉴스") || lowerMessage.contains("소식") || 
           lowerMessage.contains("이슈");
}
```

## 3. Fine-tuning 데이터에 금융 데이터 포함

### 3.1 데이터 생성 스크립트 확장

`collect_chat_history.py`를 확장하여 실제 대화와 함께 사용된 금융 데이터도 수집합니다.

**확장된 데이터 형식**:
```json
{
  "question": "AAPL 주가가 어때?",
  "answer": "애플(AAPL)의 현재가는 $150.25이며, 전일 대비 2.3% 상승했습니다...",
  "context": {
    "stocks": [
      {
        "ticker": "AAPL",
        "currentPrice": 150.25,
        "changePercent": 2.3,
        "timestamp": "2026-01-21T10:30:00"
      }
    ],
    "market": {
      "indices": [
        {"symbol": "SPY", "price": 450.20, "changePercent": 0.5}
      ]
    }
  }
}
```

### 3.2 금융 데이터 기반 대화 생성

실제 금융 데이터를 사용하여 Fine-tuning용 대화를 자동 생성합니다.

**스크립트**: `ai-server/fine-tuning/scripts/generate_financial_conversations.py`

```python
#!/usr/bin/env python3
"""
실제 금융 데이터를 사용하여 Fine-tuning용 대화를 생성하는 스크립트
"""

import requests
import json
import jsonlines
from datetime import datetime, timedelta
from typing import List, Dict, Any

class FinancialDataGenerator:
    """금융 데이터 기반 대화 생성기"""
    
    def __init__(self, backend_url: str, token: str):
        self.backend_url = backend_url.rstrip('/')
        self.token = token
        self.session = requests.Session()
        self.session.headers.update({
            'Authorization': f'Bearer {token}',
            'Content-Type': 'application/json'
        })
    
    def get_market_data(self) -> Dict[str, Any]:
        """시장 지수 데이터 조회"""
        response = self.session.get(f"{self.backend_url}/api/v1/market/indices")
        return response.json() if response.status_code == 200 else {}
    
    def get_stock_quote(self, ticker: str) -> Dict[str, Any]:
        """종목 현재가 조회"""
        response = self.session.get(f"{self.backend_url}/api/v1/stock/quote/{ticker}")
        return response.json() if response.status_code == 200 else {}
    
    def get_market_news(self) -> Dict[str, Any]:
        """시장 뉴스 조회"""
        response = self.session.get(f"{self.backend_url}/api/v1/market/news")
        return response.json() if response.status_code == 200 else {}
    
    def generate_sage_conversation(self, stock_data: Dict[str, Any]) -> Dict[str, str]:
        """투자 도사 스타일 대화 생성"""
        ticker = stock_data.get("ticker", "N/A")
        price = stock_data.get("currentPrice", 0)
        change = stock_data.get("changePercent", 0)
        
        question = f"{ticker} 주가가 어때?"
        
        if change > 0:
            answer = f"허허, {ticker}는 현재 ${price:.2f}로 전일 대비 {change:.2f}% 상승했네. " \
                    f"상승 기운이 강하니 단기적으로는 긍정적이지만, 변동성을 고려하여 신중하게 접근하도록 하게. " \
                    f"투자의 최종 책임은 자네에게 있다네."
        else:
            answer = f"{ticker}는 현재 ${price:.2f}로 전일 대비 {abs(change):.2f}% 하락했네. " \
                    f"하락 기운이 있으니 비중 조절을 명심하게나. 다만 장기 관점에서 보면 기회일 수도 있으니 " \
                    f"차분하게 분석하는 것이 중요하네. 투자의 최종 책임은 자네에게 있다네."
        
        return {"question": question, "answer": answer}
    
    def generate_analyst_conversation(self, stock_data: Dict[str, Any]) -> Dict[str, str]:
        """데이터 분석가 스타일 대화 생성"""
        ticker = stock_data.get("ticker", "N/A")
        price = stock_data.get("currentPrice", 0)
        change = stock_data.get("changePercent", 0)
        volume = stock_data.get("volume", 0)
        
        question = f"{ticker}의 현재 상황을 분석해줘"
        
        answer = f"{ticker}의 현재가는 ${price:.2f}이며, 전일 대비 {change:+.2f}% 변동했습니다. " \
                f"거래량은 {volume:,}주로 기록되었습니다. " \
                f"기술적 분석 결과, 현재 추세는 {'상승' if change > 0 else '하락'} 구간에 있습니다. " \
                f"포트폴리오에 포함할 경우 리스크 관리를 위해 적절한 비중 조절을 권장합니다. " \
                f"투자의 최종 책임은 투자자에게 있습니다."
        
        return {"question": question, "answer": answer}
    
    def generate_friend_conversation(self, stock_data: Dict[str, Any]) -> Dict[str, str]:
        """친구 조언자 스타일 대화 생성"""
        ticker = stock_data.get("ticker", "N/A")
        price = stock_data.get("currentPrice", 0)
        change = stock_data.get("changePercent", 0)
        
        question = f"{ticker} 지금 사도 될까?"
        
        if abs(change) > 3:
            answer = f"야, {ticker}는 지금 ${price:.2f}인데 전일 대비 {change:+.2f}%나 변동했어. " \
                    f"변동성이 크니까 급하게 결정하지 말고 좀 더 지켜보는 게 나을 것 같아. " \
                    f"차트도 보고, 뉴스도 확인해보고 나서 결정하는 게 좋겠어. " \
                    f"결국 결정은 네가 해야 해."
        else:
            answer = f"{ticker}는 현재 ${price:.2f}로 비교적 안정적인 수준이야. " \
                    f"전일 대비 {change:+.2f}% 변동했는데, 이 정도면 큰 변동은 아니야. " \
                    f"다만 지금 당장 사는 것보다는 조정이 올 때 기다렸다가 사는 것도 방법이야. " \
                    f"인내심이 투자에서 가장 중요한 거야."
        
        return {"question": question, "answer": answer}
    
    def generate_conversations(self, tickers: List[str], persona: str, count: int = 100):
        """금융 데이터 기반 대화 생성"""
        conversations = []
        
        for ticker in tickers:
            stock_data = self.get_stock_quote(ticker)
            if not stock_data:
                continue
            
            stock_data["ticker"] = ticker
            
            for _ in range(count // len(tickers)):
                if persona == "sage":
                    conv = self.generate_sage_conversation(stock_data)
                elif persona == "analyst":
                    conv = self.generate_analyst_conversation(stock_data)
                else:  # friend
                    conv = self.generate_friend_conversation(stock_data)
                
                conversations.append(conv)
        
        return conversations


def main():
    import argparse
    
    parser = argparse.ArgumentParser(description="금융 데이터 기반 대화 생성")
    parser.add_argument("--backend-url", type=str, default="http://localhost:8080")
    parser.add_argument("--token", type=str, required=True)
    parser.add_argument("--persona", type=str, choices=["sage", "analyst", "friend"], required=True)
    parser.add_argument("--tickers", type=str, nargs="+", default=["AAPL", "TSLA", "MSFT", "GOOGL"])
    parser.add_argument("--count", type=int, default=100)
    parser.add_argument("--output", type=str, required=True)
    
    args = parser.parse_args()
    
    generator = FinancialDataGenerator(args.backend_url, args.token)
    conversations = generator.generate_conversations(args.tickers, args.persona, args.count)
    
    # JSONL로 저장
    with jsonlines.open(args.output, mode='w') as writer:
        for conv in conversations:
            writer.write(conv)
    
    print(f"✅ {len(conversations)}개 대화를 {args.output}에 저장했습니다.")


if __name__ == '__main__':
    main()
```

## 4. 구현 단계

### Phase 1: 컨텍스트 확장 (1주)

1. **Spring Backend `ChatService` 확장**
   - `buildEnhancedContext()` 메서드 구현
   - 질문 분석 로직 추가 (`containsStockMention()`, `extractTickers()` 등)
   - 동적 금융 데이터 로딩

2. **AI Gateway `format_context()` 확장**
   - 금융 데이터 포맷팅 추가
   - 시장 지수, 종목 가격, 뉴스 정보 포함

### Phase 2: Fine-tuning 데이터 생성 (1주)

1. **금융 데이터 기반 대화 생성 스크립트**
   - `generate_financial_conversations.py` 구현
   - 실제 API 데이터를 사용한 대화 생성

2. **기존 데이터 수집 스크립트 확장**
   - `collect_chat_history.py`에 컨텍스트 정보 포함
   - 금융 데이터가 사용된 대화만 선별

### Phase 3: 통합 테스트 (1주)

1. **실제 대화 테스트**
   - 종목 언급 시 자동으로 데이터 로드 확인
   - 컨텍스트가 프롬프트에 올바르게 포함되는지 확인

2. **Fine-tuning 품질 평가**
   - 금융 데이터 포함 대화로 학습한 모델 평가
   - 실제 데이터 정확도 향상 확인

## 5. 사용 예시

### 5.1 실제 대화 예시

**사용자 질문**: "AAPL 주가가 어때?"

**Spring Backend 처리**:
1. 질문에서 "AAPL" 감지
2. `GET /api/v1/stock/quote/AAPL` 호출
3. 컨텍스트에 종목 데이터 추가:
   ```json
   {
     "stocks": [{
       "ticker": "AAPL",
       "currentPrice": 150.25,
       "changePercent": 2.3
     }]
   }
   ```

**AI Gateway 프롬프트 구성**:
```
[사용자 컨텍스트]
AAPL 현재가: $150.25 (+2.30%)
```

**AI 응답** (Analyst 페르소나):
"애플(AAPL)의 현재가는 $150.25이며, 전일 대비 2.3% 상승했습니다. 기술적 분석 결과, 상승 추세가 지속되고 있습니다..."

### 5.2 Fine-tuning 데이터 생성 예시

```bash
# 실제 금융 데이터를 사용하여 대화 생성
python scripts/generate_financial_conversations.py \
    --backend-url http://localhost:8080 \
    --token YOUR_TOKEN \
    --persona analyst \
    --tickers AAPL TSLA MSFT GOOGL NVDA \
    --count 200 \
    --output ./data/analyst_financial_raw.jsonl
```

## 6. 주의사항

### 6.1 API 호출 제한

- Finnhub API: 일일 호출 제한 있음
- EODHD API: 일일 20회 제한 (무료 플랜)
- Redis 캐싱을 활용하여 중복 호출 방지

### 6.2 데이터 정확성

- 실시간 데이터는 캐시된 데이터와 다를 수 있음
- 대화 생성 시점의 데이터를 명시적으로 기록
- Fine-tuning 시 데이터 타임스탬프 포함

### 6.3 성능 최적화

- 필요한 데이터만 동적으로 로드
- 질문 분석을 통한 불필요한 API 호출 방지
- 비동기 처리로 응답 시간 최소화

## 7. 참고 자료

- **Market API 명세**: `docs/BACKEND_DEVELOPMENT_PLAN.md` - Market API 상세 명세
- **Stock API 명세**: `docs/BACKEND_DEVELOPMENT_PLAN.md` - Stock API 상세 명세
- **AI Gateway 컨텍스트 처리**: `ai-server/ai-gateway/main.py` - AI Gateway 구현 코드
- **ChatHistory 데이터 수집 API**: `docs/BACKEND_CHAT_HISTORY_API.md` - 실제 대화 데이터 수집
- **Fine-tuning 가이드**: `ai-server/fine-tuning/README.md` - LoRA Fine-tuning 전체 프로세스
- **페르소나 시스템 설계**: `docs/BACKEND_PERSONA_DESIGN.md` - 백엔드 페르소나 시스템 상세 설계
- **AI 서버 명세**: `docs/AI_SERVER_SPEC.md` - AI 서버 전체 아키텍처 및 API 명세
