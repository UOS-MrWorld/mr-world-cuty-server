package com.mrworld.yaho.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// React가 소유하는 주소를 직접 열거나 새로고침해도 index.html로 진입시킨다.
// API, Swagger, assets 요청은 이 매핑에 포함하지 않는다.
@Controller
public class SpaController {

    @GetMapping({
            "/login",
            "/signup",
            "/tours",
            "/my-trips",
            "/cart",
            "/saved",
            "/staff/tours",
            "/staff/tours/new",
            "/staff/inventory",
            "/staff/customers"
    })
    public String forwardStaticRoute() {
        return "forward:/index.html";
    }

    @GetMapping({
            "/tours/{tourId}",
            "/tours/{tourId}/configure",
            "/tours/{tourId}/checkout",
            "/tours/{tourId}/preview",
            "/staff/tours/{tourId}/edit"
    })
    public String forwardDynamicRoute() {
        return "forward:/index.html";
    }
}
