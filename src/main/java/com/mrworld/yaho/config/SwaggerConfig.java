package com.mrworld.yaho.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class SwaggerConfig {

    // 인증 없이 호출하는 API. 이 경로에는 401/403 공통 응답을 붙이지 않는다.
    private static final Set<String> PUBLIC_PATHS =
            Set.of("/api/v1/auth/signup", "/api/v1/auth/login", "/api/v1/auth/refresh");

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "JWT";

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                                .name(jwtSchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(jwtSchemeName))
                .info(apiInfo());
    }

    // 모든 API에 공통으로 나는 오류 응답(401 인증, 403 권한, 500 서버 오류)을 한 번에 붙인다.
    // 컨트롤러에 같은 코드의 응답이 따로 적혀 있으면 그 설명을 그대로 둔다.
    @Bean
    public OpenApiCustomizer commonErrorResponses() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperations().forEach(operation -> {
                    ApiResponses responses = operation.getResponses();
                    if (!PUBLIC_PATHS.contains(path)) {
                        responses.putIfAbsent("401", errorResponse("인증 토큰 없음 또는 만료", SwaggerExamples.UNAUTHORIZED));
                    }
                    if (path.startsWith("/api/v1/customer/")) {
                        responses.putIfAbsent("403", errorResponse("고객 권한이 아님", SwaggerExamples.FORBIDDEN));
                    } else if (path.startsWith("/api/v1/staff/")) {
                        responses.putIfAbsent("403", errorResponse("직원 권한이 아님", SwaggerExamples.FORBIDDEN));
                    }
                    responses.putIfAbsent("500", errorResponse("서버 오류", SwaggerExamples.INTERNAL_SERVER_ERROR));
                }));
    }

    private ApiResponse errorResponse(String description, String example) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ResultDto"))
                        .addExamples("example", new Example().value(example))));
    }

    private Info apiInfo() {
        return new Info()
                .title("Mr.CUTY")
                .description("소프트웨어공학 프로젝트 난쏘공: Mr.CUTY API 명세서")
                .version("1.0.0");
    }
}
