package com.sapo.mock.clothing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // KÍCH HOẠT @Scheduled CHẠY NGẦM
public class ClothingShopPosApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClothingShopPosApplication.class, args);
	}

}
