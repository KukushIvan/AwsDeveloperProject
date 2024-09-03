package com.epam.aws;

import com.epam.aws.model.ImageMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private ImageService imageService;

    @Mock
    private SqsProcessor sqsProcessor;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this).close();
    }

    @Test
    void testUploadImage_Success() throws IOException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getSize()).thenReturn(12345L);
        when(multipartFile.getBytes()).thenReturn(new byte[0]);

        ResponseEntity<String> response = imageService.uploadImage(multipartFile);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(Objects.requireNonNull(response.getBody()).contains("File uploaded successfully"));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(jdbcTemplate).update(anyString(), any(Object[].class));
        // Verify the interactions with the S3 client, JDBC template, and SQS processor
        verify(sqsProcessor).sendMessage(anyString());
    }

    @Test
    void testUploadImage_FileIsEmpty() {
        when(multipartFile.isEmpty()).thenReturn(true);

        ResponseEntity<String> response = imageService.uploadImage(multipartFile);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(Objects.requireNonNull(response.getBody()).contains("File is empty"));

        verifyNoInteractions(s3Client, jdbcTemplate, sqsProcessor);
    }

    @Test
    void testUploadImage_FileExtensionNotSupported() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("testfile");

        ResponseEntity<String> response = imageService.uploadImage(multipartFile);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(Objects.requireNonNull(response.getBody()).contains("File extension is not supported"));

        verifyNoInteractions(s3Client, jdbcTemplate, sqsProcessor);
    }

    @Test
    void testDeleteImage_Success() {
        ResponseEntity<String> response = imageService.deleteImage("test.jpg");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("File deleted successfully", response.getBody());
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        verify(jdbcTemplate).update(anyString(), eq("test.jpg"));
    }

    @Test
    void testDeleteImage_Failure() {
        doThrow(new RuntimeException("S3 delete failed")).when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        ResponseEntity<String> response = imageService.deleteImage("test.jpg");

        assertEquals(500, response.getStatusCode().value());
        assertTrue(Objects.requireNonNull(response.getBody()).contains("Could not delete the file"));
    }

    @Test
    void testGetImageMetadata() {
        ImageMetadata mockMetadata = new ImageMetadata("test.jpg", 12345L, "jpg", new java.util.Date());
        when(jdbcTemplate.queryForObject(anyString(), any(ImageMetadataRowMapper.class), any(Object[].class)))
                .thenReturn(mockMetadata);

        ResponseEntity<ImageMetadata> response = imageService.getImageMetadata("test.jpg");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockMetadata, response.getBody());
    }

    @Test
    void testGetRandomImageMetadata() {
        ImageMetadata mockMetadata = new ImageMetadata("random.jpg", 12345L, "jpg", new java.util.Date());
        when(jdbcTemplate.queryForObject(anyString(), any(ImageMetadataRowMapper.class)))
                .thenReturn(mockMetadata);

        ResponseEntity<ImageMetadata> response = imageService.getRandomImageMetadata();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockMetadata, response.getBody());
    }

    @Test
    void testDownloadImage_Success() {
        byte[] mockImageData = "image data".getBytes();
        ResponseInputStream<GetObjectResponse> mockInputStream = new ResponseInputStream<>(mock(GetObjectResponse.class), new ByteArrayInputStream(mockImageData));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockInputStream);

        ResponseEntity<Resource> response = imageService.downloadImage("test.jpg");

        assertEquals(200, response.getStatusCode().value());
        assertArrayEquals(mockImageData, ((ByteArrayResource) Objects.requireNonNull(response.getBody())).getByteArray());
    }

    @Test
    void testDownloadImage_Failure() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(new RuntimeException("S3 download failed"));

        ResponseEntity<Resource> response = imageService.downloadImage("test.jpg");

        assertEquals(500, response.getStatusCode().value());
        assertNull(response.getBody());
    }
}
