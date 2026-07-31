package org.zerock.puppyrun.a_config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;

/**
 * 데이터베이스를 사용하는 서비스 통합 테스트의 공통 기반 클래스입니다.
 *
 * <p>테스트 JVM에서 MySQL 컨테이너를 한 번만 시작하고 모든 하위 테스트가
 * 동일한 컨테이너와 Spring 컨텍스트를 재사용합니다.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
public abstract class TestContainerConfig {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.5")
            .withDatabaseName("puppy_run_test")
            .withUsername("puppy_run")
            .withPassword("puppy_run");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.docker.compose.enabled", () -> "false");
    }
}
