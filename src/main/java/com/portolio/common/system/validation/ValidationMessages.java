package com.portolio.common.system.validation;

/**
 * [Validation Constants]
 * Jakarta Bean Validation 관련 메시지를 상수로 관리하는 클래스입니다.
 *
 * 이 클래스를 사용하는 주된 목적은 다음과 같습니다.
 * 1. **일관성 유지**: 애플리케이션 전체에서 동일한 유효성 검증 규칙에 대해 항상 동일한 에러 메시지를 사용하도록 보장합니다.
 * 2. **오타 방지**: 메시지를 문자열 리터럴("필수 입력 항목입니다.")로 직접 사용하는 대신, 상수(`ValidationMessages.NOT_BLANK`)를 사용함으로써
 *    컴파일 시점에 오타를 체크할 수 있고, 코드의 안정성을 높입니다.
 * 3. **중앙 관리**: 유효성 검증 메시지에 대한 정책이 변경될 경우(예: 문구 수정), 이 파일 하나만 수정하면
 *    애플리케이션 전체에 일관되게 적용할 수 있어 유지보수가 매우 용이해집니다.
 *
 * 이 클래스는 `final class`와 `private constructor`를 사용하여 상속 및 인스턴스화를 방지하는
 * 전형적인 유틸리티 클래스 패턴을 따릅니다.
 *
 * (참고: 만약 다국어(i18n) 지원이 필요하다면, 이 클래스 대신 `src/main/resources` 폴더에
 * `messages.properties`, `messages_ko.properties` 와 같은 메시지 소스 파일을 사용하는 것이 더 적합합니다.)
 */
public final class ValidationMessages {

    /**
     * 유틸리티 클래스는 인스턴스화할 필요가 없으므로 private 생성자로 막음.
     */
    private ValidationMessages() {}

    // --- 공통 유효성 검증 메시지 ---

    /**
     * @NotBlank, @NotEmpty, @NotNull 어노테이션에서 사용할 수 있는 메시지.
     * 값이 비어있거나 null일 수 없음을 나타냄.
     * 사용 예시: @NotBlank(message = ValidationMessages.NOT_BLANK)
     */
    public static final String NOT_BLANK = "필수 입력 항목입니다.";

    // --- 사용자(User) 관련 유효성 검증 메시지 ---

    /**
     * @Email 어노테이션에서 사용할 수 있는 메시지.
     * 입력된 값이 유효한 이메일 형식이 아님을 나타냄.
     * 사용 예시: @Email(message = ValidationMessages.INVALID_EMAIL_FORMAT)
     */
    public static final String INVALID_EMAIL_FORMAT = "올바른 이메일 형식이 아닙니다.";

    /**
     * @Pattern 어노테이션과 함께 사용하여 비밀번호 정책을 검증할 때 사용할 수 있는 메시지.
     * 사용 예시: @Pattern(regexp = "(?=.*[0-9])(?=.*[a-zA-Z])...", message = ValidationMessages.PASSWORD_POLICY)
     */
    public static final String PASSWORD_POLICY = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상 20자 이하이어야 합니다.";

    /**
     * @Pattern 어노테이션과 함께 사용하여 사용자 이름(닉네임)의 형식을 검증할 때 사용할 수 있는 메시지.
     * 사용 예시: @Pattern(regexp = "^[가-힣a-zA-Z0-9]{2,10}$", message = ValidationMessages.INVALID_USERNAME_FORMAT)
     */
    public static final String INVALID_USERNAME_FORMAT = "사용자 이름은 한글, 영문, 숫자로만 2자 이상 10자 이하로 입력해주세요.";


    // --- 게시글(Post) 관련 유효성 검증 메시지 ---

    /**
     * @Size 어노테이션과 함께 사용하여 게시글 제목의 최대 길이를 제한할 때 사용할 수 있는 메시지.
     * 사용 예시: @Size(max = 100, message = ValidationMessages.TITLE_TOO_LONG)
     */
    public static final String TITLE_TOO_LONG = "제목은 100자를 초과할 수 없습니다.";

    /**
     * @Size 어노테이션과 함께 사용하여 게시글 내용의 최대 길이를 제한할 때 사용할 수 있는 메시지.
     * 사용 예시: @Size(max = 5000, message = ValidationMessages.CONTENT_TOO_LONG)
     */
    public static final String CONTENT_TOO_LONG = "내용은 5000자를 초과할 수 없습니다.";


    // --- 숫자 관련 유효성 검증 메시지 ---

    /**
     * @Min, @Positive 어노테이션에서 사용할 수 있는 메시지.
     * 값이 양수여야 함을 나타냄.
     * 사용 예시: @Min(value = 1, message = ValidationMessages.MUST_BE_POSITIVE)
     */
    public static final String MUST_BE_POSITIVE = "값은 0보다 커야 합니다.";

    /**
     * @Max 어노테이션에서 사용할 수 있는 메시지.
     * 값이 특정 최대값을 초과할 수 없음을 나타냄.
     * 이 메시지는 %d 와 같은 포맷 지정자를 사용하여 동적으로 값을 주입할 수 없음.
     * 동적 메시지가 필요하다면 메시지 소스 파일(.properties)을 사용.
     * 사용 예시: @Max(value = 1000, message = ValidationMessages.MAX_VALUE_EXCEEDED)
     */
    public static final String MAX_VALUE_EXCEEDED = "최대값을 초과할 수 없습니다.";
}