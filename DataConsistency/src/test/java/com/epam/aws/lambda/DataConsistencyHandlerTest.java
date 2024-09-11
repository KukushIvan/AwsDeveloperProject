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
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.sql.*;
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

        when(mockResultSet.next()).thenReturn(true, false);  // Return one record, then stop
        when(mockResultSet.getString("file_name")).thenReturn("image1.jpg");
        when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenReturn(null);

        String result = dataConsistencyHandler.handleRequest(input, mockContext);
        assertEquals("Data is consistent", result);

        verify(mockLogger).log(contains("Triggered by " + expectedLog + "\n"));
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

        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("file_name")).thenReturn("image1.jpg");

        // Simulate object not existing in S3
        doThrow(NoSuchKeyException.builder().build()).when(mockS3Client).headObject(any(HeadObjectRequest.class));

        String result = dataConsistencyHandler.handleRequest(input, mockContext);
        assertEquals("Data inconsistency detected", result);

        verify(mockLogger).log(contains("Triggered by API Gateway"));
        verify(mockLogger).log(contains("Image image1.jpg not found in S3"));
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
    void testDoesObjectExist_objectExists() {
        when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenReturn(null);
        boolean exists = DataConsistencyHandler.doesObjectExist("bucket", "objectKey");
        assertTrue(exists);
    }

    @Test
    void testDoesObjectExist_objectDoesNotExist() {
        doThrow(NoSuchKeyException.builder().build()).when(mockS3Client).headObject(any(HeadObjectRequest.class));
        boolean exists = DataConsistencyHandler.doesObjectExist("bucket", "objectKey");
        assertFalse(exists);
    }

    @Test
    void testDoesObjectExist_s3Exception() {
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
                .statusCode(500)
                .message("Internal Server Error")
                .build();
        doThrow(s3Exception).when(mockS3Client).headObject(any(HeadObjectRequest.class));

        S3Exception thrown = assertThrows(S3Exception.class, () -> {
            DataConsistencyHandler.doesObjectExist("bucket", "objectKey");
        });

        assertEquals(500, thrown.statusCode());
    }
}
