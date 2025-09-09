package com.example.cms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 데이터베이스 연결 테스트
 * MySQL 데이터베이스 연결이 정상적으로 작동하는지 확인
 */
@SpringBootTest
@ActiveProfiles("test") // H2 데이터베이스를 사용하여 테스트
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testDatabaseConnection() throws SQLException {
        // Given: DataSource가 주입되었을 때
        assertNotNull(dataSource, "DataSource should be injected");

        // When: 데이터베이스 연결을 시도할 때
        try (Connection connection = dataSource.getConnection()) {
            // Then: 연결이 성공적으로 이루어져야 함
            assertNotNull(connection, "Connection should not be null");
            assertTrue(connection.isValid(1), "Connection should be valid");
            assertFalse(connection.isClosed(), "Connection should not be closed");
        }
    }

    @Test
    void testConnectionMetadata() throws SQLException {
        // Given: 데이터베이스 연결이 있을 때
        try (Connection connection = dataSource.getConnection()) {
            // When: 메타데이터를 조회할 때
            var metadata = connection.getMetaData();
            
            // Then: 메타데이터가 올바르게 반환되어야 함
            assertNotNull(metadata, "Database metadata should not be null");
            assertNotNull(metadata.getDatabaseProductName(), "Database product name should not be null");
            assertNotNull(metadata.getDriverName(), "Driver name should not be null");
            
            System.out.println("Database Product: " + metadata.getDatabaseProductName());
            System.out.println("Driver Name: " + metadata.getDriverName());
            System.out.println("Driver Version: " + metadata.getDriverVersion());
        }
    }
}