package com.epam.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.sql.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataConsistencyHandlerTest {

    @Mock
    private Connection mockDbConnection;

    @Mock
    private Statement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private S3Client mockS3Client;

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    private DataConsistencyHandler dataConsistencyHandler;

    @BeforeEach
    public void setUp() throws Exception {

        MockitoAnnotations.openMocks(this).close();

        when(mockDbConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("file_name")).thenReturn("image1.jpg");
        when(mockContext.getLogger()).thenReturn(mockLogger);

        dataConsistencyHandler = new DataConsistencyHandler(mockDbConnection, mockS3Client);
    }

    @ParameterizedTest
    @MethodSource("triggeredBy")
    void testHandleRequest(String detailType, String expectedLog) throws SQLException {
        Map<String, Object> input = new HashMap<>();
        input.put("detail-type", detailType);

        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("file_name")).thenReturn("image1.jpg");

        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .contents(S3Object.builder().key("image1.jpg").build())
                .build();
        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(mockResponse);

        String result = dataConsistencyHandler.handleRequest(input, mockContext);
        assertEquals("Data is consistent", result);

        verify(mockLogger).log(contains("Triggered by " + expectedLog + "\n"));
        verify(mockLogger).log(contains("Fetched 1 file names from DB"));
        verify(mockLogger).log(contains("Fetched 1 file names from S3"));
    }

    private static Stream<Arguments> triggeredBy() {
        return Stream.of(
                Arguments.of("Scheduled Event", "EventBridge schedule"),
                Arguments.of("APIGateway", "API Gateway"),
                Arguments.of("WebApp", "WebApp")
        );
    }

    @Test
    void testHandleRequest_dataInconsistent() throws SQLException {
        Map<String, Object> input = new HashMap<>();
        input.put("detail-type", "APIGateway");

        // Симулировать данные из базы данных
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("file_name")).thenReturn("image1.jpg");

        // Симулировать отсутствие файла в S3
        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .contents(Collections.emptyList())
                .build();
        when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(mockResponse);

        String result = dataConsistencyHandler.handleRequest(input, mockContext);
        assertEquals("Data inconsistency detected", result);

        verify(mockLogger).log(contains("File image1.jpg from DB not found in S3"));
    }

    @Test
    void testHandleRequest_sqlException() throws SQLException {
        Map<String, Object> input = new HashMap<>();
        input.put("detail-type", "APIGateway");

        when(mockDbConnection.createStatement()).thenThrow(new SQLException("DB error"));

        String result = dataConsistencyHandler.handleRequest(input, mockContext);
        assertEquals("Data inconsistency detected", result);

        verify(mockLogger).log(contains("Error querying database: DB error"));
    }

    @Test
    void testHandleRequest_noFilesInDb() throws SQLException {
        Map<String, Object> input = new HashMap<>();
        input.put("detail-type", "APIGateway");

        when(mockResultSet.next()).thenReturn(false);

        String result = dataConsistencyHandler.handleRequest(input, mockContext);
        assertEquals("Data inconsistency detected", result);

        verify(mockLogger).log(contains("No files found in DB or an error occurred"));
    }

}
