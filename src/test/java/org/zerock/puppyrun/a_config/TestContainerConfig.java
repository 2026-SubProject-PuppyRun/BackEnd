package org.zerock.puppyrun.a_config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.zerock.puppyrun.weather.service.WeatherForecastCollector;

/**
 * 데이터베이스 및 메시지 브로커를 사용하는 서비스 통합 테스트의 공통 기반 클래스입니다.
 *
 * <p>테스트 JVM에서 MySQL 및 RabbitMQ 컨테이너를 한 번만 시작하고 모든 하위 테스트가
 * 동일한 컨테이너와 Spring 컨텍스트를 재사용합니다.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
public abstract class TestContainerConfig {

    /**
     * 모든 통합 테스트가 동일한 Spring Context를 공유할 수 있도록 공통 기반 클래스에 선언합니다.
     * 이를 통해 RabbitMQ @RabbitListener 경쟁 문제를 방지하고 단일 컨텍스트를 보장합니다.
     * 각 테스트 메서드에서 필요에 따라 Stubbing을 설정할 수 있습니다.
     */
    @MockBean
    protected WeatherForecastCollector weatherForecastCollector;

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.5")
            .withDatabaseName("puppy_run_test")
            .withUsername("puppy_run")
            .withPassword("puppy_run");

    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management");

    static {
        MYSQL.start();
        RABBITMQ.start();
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

        // RabbitMQ 컨테이너 동적 설정 (포트가 랜덤으로 할당되므로 동적 주입 필수)
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }
}
