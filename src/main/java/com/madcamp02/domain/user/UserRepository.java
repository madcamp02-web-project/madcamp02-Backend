//Repository와 JPA는 어떻게 sql 쿼리 문자열을 날릴까? 에 대한 원리 설명

/*
JPA(구현체인 Hibernate)가 쿼리를 만드는 과정은 크게 3단계로 요약됩니다.

메서드 분석 (Parsing)

findByEmail 같은 메서드 이름을 분석하여 키워드(find=조회, By=조건, Email=필드)를 추출합니다.

JPQL 생성 (Abstract Query)

추출한 정보를 바탕으로 DB 테이블이 아닌 자바 엔티티 객체를 대상으로 하는 중간 단계 쿼리(JPQL)를 생성합니다.

예: SELECT u FROM User u WHERE u.email = :email

SQL 변환 (Translation)

application.properties에 설정된 Dialect(방언) 정보를 참조해, JPQL을 실제 사용하는 DB(MySQL, Oracle 등)에 맞는 SQL로 최종 변환합니다.

예: SELECT * FROM users WHERE email = ?
 */


//Repository는 데이터베이스 접근을 담당하는 인터페이스로 Spring Data JPA가 자동으로 구현체를 생성하는 것.
//Service  →  Repository (인터페이스)  →  JPA가 자동 생성한 구현체  →  Database

//Repository (Interface): 개발자는 구현 코드 없이 인터페이스만 정의
//Spring Data JPA (Implementation): 애플리케이션 로딩 시점(Runtime)에 Dynamic Proxy(동적 프록시) 기술을 사용해, 해당 인터페이스를 구현한 클래스(객체)를 메모리에 자동으로 생성
//Dependency Injection: 생성된 이 프록시 객체가 Service에 주입(DI)
//Execution: Service가 메서드를 호출하면, 실제로는 스프링이 만든 프록시 객체가 실행되어 내부적으로 SQL을 생성하고 DB에 요청

/*
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    //                                               ↑      ↑
    //                                           Entity   PK 타입
}
이런 방식으로 적음

JpaRepository가 제공하는 기본 메서드
메서드	            설명
save(entity)	    저장/수정
findById(id)	    ID로 조회 (Optional 반환)
findAll()	        전체 조회
delete(entity)	    삭제
deleteById(id)	    ID로 삭제
existsById(id)	    존재 여부 확인
count()	            전체 개수

메서드 이름 기반 쿼리 (Query Method)
메서드 이름만 작성하면 JPA가 자동으로 쿼리를 생성

// findBy + 필드명
Optional<User> findByEmail(String email);
// → SELECT * FROM users WHERE email = ?

// findBy + 필드명 + And + 필드명
List<User> findBySajuElementAndZodiacSign(String element, String sign);
// → SELECT * FROM users WHERE saju_element = ? AND zodiac_sign = ?

// OrderBy
List<User> findByProviderOrderByCreatedAtDesc(String provider);
// → SELECT * FROM users WHERE provider = ? ORDER BY created_at DESC

이런식으로 Spring은 sql문을 날린다.

키워드 목록
키워드	            예시	                        SQL
And	                findByNameAndAge	        WHERE name = ? AND age = ?
Or	                findByNameOrAge	            WHERE name = ? OR age = ?
Between	            findByAgeBetween	        WHERE age BETWEEN ? AND ?
LessThan	        findByAgeLessThan	        WHERE age < ?
GreaterThan	        findByAgeGreaterThan	    WHERE age > ?
Like	            findByNameLike	            WHERE name LIKE ?
Containing	        findByNameContaining	    WHERE name LIKE %?%
In	                findByAgeIn	                WHERE age IN (?)
OrderBy	            findByNameOrderByAgeDesc	ORDER BY age DESC
Not	                findByNameNot	            WHERE name != ?
IsNull	            findByNameIsNull	        WHERE name IS NULL


@Query 어노테이션 (JPQL)
복잡한 쿼리는 직접 작성
이런식으로 작성한다

@Query("SELECT u FROM User u WHERE u.createdAt >= :date")
List<User> findRecentUsers(@Param("date") LocalDateTime date);

@Query("SELECT u FROM User u WHERE u.email LIKE %:keyword%")
List<User> searchByEmail(@Param("keyword") String keyword);



Native Query (순수 SQL)은 이런식으로 작성

@Query(value = "SELECT * FROM users WHERE created_at >= :date", nativeQuery = true)
List<User> findRecentUsersNative(@Param("date") LocalDateTime date);
*/

package com.madcamp02.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);

    // 닉네임으로 사용자 조회
    Optional<User> findByNickname(String nickname);

    // 닉네임 존재 여부 확인
    boolean existsByNickname(String nickname);

    // 사주 오행으로 사용자 목록 조회
    List<User> findBySajuElement(String sajuElement);

    // 띠로 사용자 목록 조회
    List<User> findByZodiacSign(String zodiacSign);

    // Provider로 사용자 목록 조회
    List<User> findByProvider(String provider);

    // 닉네임 검색 (부분 일치)
    List<User> findByNicknameContaining(String keyword);

    // 이메일과 Provider로 조회 (OAuth 로그인용)
    Optional<User> findByEmailAndProvider(String email, String provider);
}