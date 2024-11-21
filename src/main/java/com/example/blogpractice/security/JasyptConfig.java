package com.example.blogpractice.security;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.salt.RandomSaltGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JasyptConfig {

    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();

        // 환경 변수에서 암호화 키를 읽어옴
        String password = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
        if (password == null) {
            throw new IllegalStateException("JASYPT_ENCRYPTOR_PASSWORD 환경 변수가 설정되지 않았습니다.");
        }

        encryptor.setPassword(password);
        encryptor.setAlgorithm("PBEWithHmacSHA512AndAES_256");
        encryptor.setSaltGenerator(new RandomSaltGenerator());
        encryptor.setIvGenerator(new RandomIvGenerator());

        return encryptor;
    }
}
