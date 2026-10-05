package com.mrworld.yaho.config;

public class SwaggerExamples {
    public static final String BAD_REQUEST = """
            {
              "success": false,
              "message": "요청 값이 올바르지 않습니다.",
              "code": 400
            }
            """;

    public static final String UNAUTHORIZED = """
            {
              "success": false,
              "message": "인증이 필요합니다.",
              "code": 401
            }
            """;

    public static final String INVALID_CREDENTIALS = """
            {
              "success": false,
              "message": "아이디 또는 비밀번호가 올바르지 않습니다.",
              "code": 401
            }
            """;

    public static final String INVALID_REFRESH_TOKEN = """
            {
              "success": false,
              "message": "유효하지 않은 refresh token입니다.",
              "code": 401
            }
            """;

    public static final String FORBIDDEN = """
            {
              "success": false,
              "message": "접근 권한이 없습니다.",
              "code": 403
            }
            """;

    public static final String NOT_FOUND = """
            {
              "success": false,
              "message": "요청한 리소스를 찾을 수 없습니다.",
              "code": 404
            }
            """;

    public static final String LOGIN_ID_DUPLICATED = """
            {
              "success": false,
              "message": "이미 사용 중인 로그인 아이디입니다.",
              "code": 409
            }
            """;

    public static final String CONFLICT = """
            {
              "success": false,
              "message": "현재 상태에서는 요청을 처리할 수 없습니다.",
              "code": 409
            }
            """;

    public static final String INTERNAL_SERVER_ERROR = """
            {
              "success": false,
              "message": "서버 오류가 발생했습니다.",
              "code": 500
            }
            """;

    private SwaggerExamples() {
    }
}
