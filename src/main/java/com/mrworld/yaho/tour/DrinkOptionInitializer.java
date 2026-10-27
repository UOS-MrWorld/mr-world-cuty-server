package com.mrworld.yaho.tour;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// 음료 옵션을 등록하는 API가 명세에 없어서, 음료 옵션이 비어 있을 때만 기본 음료를 채워 둔다.
@Component
@RequiredArgsConstructor
public class DrinkOptionInitializer implements ApplicationRunner {

    private final DrinkOptionRepository drinkOptionRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (drinkOptionRepository.count() > 0) {
            return;
        }

        drinkOptionRepository.saveAll(List.of(
                DrinkOption.create("COFFEE", 5_000),
                DrinkOption.create("CHAMPAGNE", 30_000)
        ));
    }
}
