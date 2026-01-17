package com.madcamp02.dto.request;

//DTO(Data Transfer Object)에 대해서...
//spring에서는 Request라는 파일 내부에 있는 java 파일을 의미한다.

//프론트엔드에서 보낸 JSON 데이터를 자바가 이해할 수 있는 객체로 담아주는 전용 그릇

/*
//DTO의 역할

### 1. JSON 번역기 (JSON  Java Object)

프론트엔드(웹/앱)는 자바를 모릅니다.
그래서 데이터를 보낼 때 **JSON**이라는 문자열 형식으로 보냅니다.

            **프론트엔드가 보내는 데이터 (JSON):**

            ```json
            {
              "refreshToken": "eyJhbGciOiJIUzI1Ni..."
            }
            ```

**스프링 서버 (Java):**
스프링은 이 JSON 문자열을 받으면, `RefreshRequest` 클래스의 설계도를 보고
똑같이 생긴 자바 객체로 변환합니다. 이것을 **역직렬화(Deserialization)**라고 합니다.

//json을 받아서 Request라는 DTO을 이용해서 역직렬화를 해서
//하나의 직렬화된 긴 문자열 1줄로 변환하는 것임(json -> (역직렬화) -> 문자열 1줄)

* JSON의 `"refreshToken"` 키를 찾아서
* Java 클래스의 `private String refreshToken;` 변수에 값을 쏙 넣어줍니다.

> **Node.js와 비교:**
> * **Node.js:** `req.body.refreshToken` (그냥 들어온 대로 씀, 오타 나면 `undefined` 뜸)
> * **Spring:** `request.getRefreshToken()` (미리 정의된 객체로 변환해서 씀, 훨씬 안전함)

---

### 2. 문지기 (Validation Check)

코드에 보면 이런 어노테이션이 붙어있죠?
`@NotBlank(message = "refreshToken은 필수입니다.")`

이 `Request` 객체는 컨트롤러에 도착하기 전에 검문소 역할을 합니다.
만약 프론트엔드가 실수를 해서 토큰을 안 보냈거나, 빈 문자열 `""`을 보냈다면?

* **이 클래스가 없으면: 컨트롤러 내부까지 들어가서 에러가 터지거나 엉뚱한 로직이 돔.
* **이 클래스가 있으면: 아예 입구 컷(400 Bad Request 에러)을 시켜버립니다.
                    "야, 필수 데이터 빠졌어!" 하고요.

---

### 3. 안전한 데이터 필터 (Safety)

해커가 악의적으로 이상한 데이터를 섞어서 보낸다고 가정해 봅시다.
(JSON은 중간에 가로채서 변조하는 것은 일도 아니니까...)(그냥 텍스트 파일이잖아)

        **해커의 요청:**

        ```json
        {
          "refreshToken": "...",
          "isAdmin": true  // ⬅️ 관리자 권한을 몰래 섞어서 보냄
        }

        ```

만약 `RefreshRequest` 클래스에 `isAdmin`이라는 필드(변수)가 없다면?
스프링은 "어? 우리 규격서(Class)에는 isAdmin 같은 건 없는데?" 하고
저 데이터를 무시하고 버립니다.

즉, 우리가 딱 정의한 데이터(`refreshToken`)만 받겠다고 선언하는 보안 장치 역할

---

### 요약: Request 객체의 일생 📦

1. **주문서:** 개발자가 "이 요청을 받을 땐 `refreshToken` 딱 하나만 들어있는 상자를 써라"고 `RefreshRequest` 클래스를 만듦.
2. **배송:** 클라이언트가 JSON 데이터를 보냄.
3. **포장:** 스프링이 JSON을 뜯어서 `RefreshRequest` 상자에 담음.
4. **검수:** `@NotBlank` 체크 (비었으면 반송).
5. **전달:** 내용물이 꽉 찬 `RefreshRequest` 객체가 `AuthController`로 전달됨.

결론적으로 "너네가 데이터 보낼 때 이 양식에 맞춰서 써내!"라고 정해둔 신청서 양식입니다.
*/

/*
내 질문: node는 역직렬화의 경우 메서드 1개로 처리하나? passport의 역직렬화인 deserializeUser()는 뭐야?
네, 아주 정확하게 보셨습니다! 👍
Node.js(Express)에서는 스프링처럼 **"DTO 클래스를 만들고 어노테이션을 붙이는"** 복잡한 과정이 없습니다.

질문하신 내용인 **"메서드 1개로 처리하나?"**와 **"화살표 함수로 처리하나?"**에 대해 명확히 정리해 드릴게요.

---

### 1. Node.js의 역직렬화: "딱 한 줄이면 끝" ⚡️

스프링은 `Request DTO` 클래스를 만들어야 했지만, Node.js(Express)는 서버 설정 파일(app.js)에 있는 **미들웨어 한 줄**이 그 모든 일을 다 처리해 버립니다.

            ```javascript
            // app.js (설정 파일)
            const express = require('express');
            const app = express();

            // ★ 이 한 줄이 스프링의 DTO 변환 과정을 대신합니다.
            // 들어오는 모든 요청의 Body가 JSON이면, 자동으로 자바스크립트 객체로 바꿔서 req.body에 넣어줍니다.
            app.use(express.json());

            ```

이렇게 해두면 컨트롤러에서는 그냥 **화살표 함수** 안에서 바로 갖다 쓰면 됩니다.

            ```javascript
            // authController.js
            // DTO 클래스? 그런 거 안 만듭니다.
            exports.refresh = (req, res) => {
                // 이미 변환되어 있음. 그냥 꺼내 쓰면 됨.
                const refreshToken = req.body.refreshToken;

                // ... 로직 ...
            };

            ```

* **스프링:** JSON  Jackson 라이브러리  `RefreshRequest` 클래스 매핑  컨트롤러 전달
* **Node.js:** JSON  `express.json()` 미들웨어  자바스크립트 객체(`req.body`)  컨트롤러 전달

---

### 2. Passport의 `deserializeUser`는 다른 얘기입니다 🛂

"Passport밖에 안 써봤다"고 하셨는데, Passport에 있는 `deserializeUser`는 위에서 말한 **데이터 전송(DTO)**과는 조금 다른 개념입니다.

* **Express의 `express.json()` (Body Parsing):**
* 목적: 클라이언트가 보낸 **데이터(JSON)**를 읽기 위해.
* 대상: 로그인할 때 아이디/비번, 게시글 작성할 때 제목/내용 등.


* **Passport의 `deserializeUser` (Session):**
* 목적: **쿠키(세션 ID)**를 가지고 **유저 정보(User)**를 복원하기 위해.
* 대상: 로그인 **후**에 "이 사람 누구지?" 하고 DB에서 찾아낼 때.



즉, **Passport**는 "세션 복원용"이고,
우리가 방금 스프링 코드에서 본 **DTO**는 "요청 데이터 파싱용"입니다.

---

### 3. Node.js 방식의 장단점 (스프링과 비교)

Node.js가 훨씬 간단해 보이지만, **"자유에는 책임이 따른다"**는 점이 중요합니다.

| 비교 항목 | Spring Boot (DTO) | Node.js (req.body) |
| --- | --- | --- |
| **코드 양** | 많음 (클래스 생성 필요) | **매우 적음** (설정 1줄) |
| **타입 안정성** | **높음** (String, Int 등 딱 정해짐) | 낮음 (그냥 아무거나 다 들어옴) |
| **유효성 검사** | `@NotBlank` 등으로 자동 처리 | **직접 해야 함** (`if (!req.body.token)...`) |
| **오타 방지** | 컴파일 에러로 잡아줌 | 실행해 봐야 에러 남 (`undefined`) |

### 4. 결론

말씀대로 Node.js는 **`express.json()` 메서드 1개**가 입구에서 모든 JSON을 싹 다 객체로 바꿔버립니다.
그리고 컨트롤러(화살표 함수)에서는 그냥 `req.body.변수명`으로 꺼내 쓰기만 하면 됩니다.
그래서 Node.js가 **"초기 개발 속도가 엄청 빠르다"**는 소리를 듣는 것이고, 스프링은 **"귀찮지만 대규모 협업할 때 안전하다"**는 소리를 듣는 겁니다.
 */




//======================================
// LoginRequest - 로그인 요청 DTO
//======================================
// 클라이언트 → 서버로 전송되는 로그인 요청 데이터 구조
//
// 사용 시점: POST /api/v1/auth/login 요청 시 Body에 포함
//
// 필드 설명:
//   - provider: 인증 제공자 (현재는 "google"만 지원)
//   - idToken: Google OAuth2에서 발급받은 ID Token
//
// Google ID Token이란?
//   - Google 로그인 성공 시 발급되는 JWT 형태의 토큰
//   - 사용자 이메일, 이름, 프로필 사진 등 포함
//   - 서버가 Google 공개키로 검증하여 위변조 확인
//
// 인증 흐름:
//   1. 클라이언트가 Google 로그인 버튼 클릭
//   2. Google이 ID Token 발급
//   3. 클라이언트가 이 DTO로 서버에 전송
//   4. 서버가 검증 후 자체 JWT 발급
//======================================

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    //------------------------------------------
    // 인증 제공자 (필수)
    //------------------------------------------
    // 현재 지원: "google"
    // 향후 확장: "kakao", "naver", "apple" 등
    //------------------------------------------
    @NotBlank(message = "provider는 필수입니다.")
    private String provider;

    //------------------------------------------
    // Google ID Token (필수)
    //------------------------------------------
    // Google OAuth2 인증 후 발급받은 JWT 토큰
    // 
    // 포함 정보 (Payload):
    //   - sub: Google 사용자 고유 ID
    //   - email: 이메일 주소
    //   - name: 사용자 이름
    //   - picture: 프로필 사진 URL
    //   - iat: 발급 시간
    //   - exp: 만료 시간
    //------------------------------------------
    @NotBlank(message = "idToken은 필수입니다.")
    private String idToken;
}
