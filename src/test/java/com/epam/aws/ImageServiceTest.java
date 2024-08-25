package com.epam.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.util.EC2MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AmazonS3 s3Client;

    @Mock
    private AWSSimpleSystemsManagement ssmClient;

    @InjectMocks
    private ImageService imageService;

    private MockedStatic<AWSSimpleSystemsManagementClientBuilder> ssmClientBuilderMock;

    private final String testFileName = "test.jpg";
    private ImageMetadata testMetadata;

    @BeforeEach
    void setUp() {
        // Mock the static method to return the mocked SSM client
        ssmClientBuilderMock = mockStatic(AWSSimpleSystemsManagementClientBuilder.class);
        AWSSimpleSystemsManagementClientBuilder ssmClientBuilder = mock(AWSSimpleSystemsManagementClientBuilder.class);
        when(ssmClientBuilder.build()).thenReturn(ssmClient);
        ssmClientBuilderMock.when(AWSSimpleSystemsManagementClientBuilder::defaultClient).thenReturn(ssmClientBuilder);

        // Mock the response from SSM client
        GetParameterResult parameterResult = mock(GetParameterResult.class);
        when(parameterResult.getParameter()).thenReturn(new com.amazonaws.services.simplesystemsmanagement.model.Parameter().withValue("test-bucket"));
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenReturn(parameterResult);

        // Реинициализация ImageService для использования замокированных зависимостей
        imageService = new ImageService(jdbcTemplate, s3Client);

        testMetadata = new ImageMetadata(testFileName, 12345L, "image/jpeg", "2024-08-22T12:34:56Z");
    }

//    @Test
    void testGetImageMetadataSuccess() {
        GetParameterResult parameterResult = Mockito.mock(GetParameterResult.class);
        when(parameterResult.getParameter()).thenReturn(new com.amazonaws.services.simplesystemsmanagement.model.Parameter().withValue("test-bucket"));
        // Mock the jdbcTemplate query to return a list containing testMetadata
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(BeanPropertyRowMapper.class)))
                .thenReturn(List.of(testMetadata));
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenReturn(parameterResult);
        // Call the method
        ResponseEntity<ImageMetadata> response = imageService.getImageMetadata(testFileName);

        // Validate the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testMetadata, response.getBody());
    }

//    @Test
    void testGetImageMetadataNotFound() {
        // Mock the jdbcTemplate query to return an empty list
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(BeanPropertyRowMapper.class)))
                .thenReturn(List.of());

        // Call the method
        ResponseEntity<ImageMetadata> response = imageService.getImageMetadata(testFileName);

        // Validate the response
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

//    @Test
    void testGetImageMetadataException() {
        // Mock the jdbcTemplate query to throw an exception
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(BeanPropertyRowMapper.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Call the method
        ResponseEntity<ImageMetadata> response = imageService.getImageMetadata(testFileName);

        // Validate the response
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

//    @Test
    void testUploadImage() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getSize()).thenReturn(12345L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenReturn(Mockito.mock(java.io.InputStream.class));

        ResponseEntity<String> response = imageService.uploadImage(file);


        verify(s3Client).putObject(any(PutObjectRequest.class));
        verify(jdbcTemplate).update(anyString(), anyString(), anyLong(), anyString());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("File uploaded successfully: test.jpg", response.getBody());
    }

//    @Test
    void testGetImageMetadata() {
        ImageMetadata metadata = new ImageMetadata("test.jpg", 12345L, "image/jpeg", "2023-08-21T12:34:56Z");
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(BeanPropertyRowMapper.class)))
                .thenReturn(List.of(metadata));

        ResponseEntity<ImageMetadata> response = imageService.getImageMetadata("test.jpg");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(metadata, response.getBody());
    }

//    @Test
    void testGetRandomImageMetadata() {
        ImageMetadata metadata = new ImageMetadata("random.jpg", 54321L, "image/png", "2023-08-21T12:34:56Z");
        when(jdbcTemplate.query(anyString(), any(BeanPropertyRowMapper.class)))
                .thenReturn(List.of(metadata));

        ResponseEntity<ImageMetadata> response = imageService.getRandomImageMetadata();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(metadata, response.getBody());
    }

//    @Test
    void testDownloadImage() throws Exception {
        URL url = new URL("https://example.com/test.jpg");
        when(s3Client.getUrl(anyString(), anyString())).thenReturn(url);
        Resource resource = new UrlResource(url);
        when(resource.exists()).thenReturn(true);

        ResponseEntity<Resource> response = imageService.downloadImage("test.jpg");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(resource, response.getBody());
    }

//    @Test
    void testDeleteImage() {
        ResponseEntity<String> response = imageService.deleteImage("test.jpg");

        verify(s3Client).deleteObject(anyString(), anyString());
        verify(jdbcTemplate).update(anyString(), anyString());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("File deleted successfully", response.getBody());
    }
}