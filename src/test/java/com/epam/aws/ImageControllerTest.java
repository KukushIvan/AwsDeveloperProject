package com.epam.aws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ImageControllerTest {

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ImageController imageController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUploadImage() {
        MultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());
        when(imageService.uploadImage(file)).thenReturn(new ResponseEntity<>("File uploaded successfully: test.jpg", HttpStatus.OK));

        ResponseEntity<String> response = imageController.uploadImage(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("File uploaded successfully: test.jpg", response.getBody());
    }

    @Test
    void testDownloadImage() {
        when(imageService.downloadImage(anyString())).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        ResponseEntity<?> response = imageController.downloadImage("test.jpg");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGetImageMetadata() {
        ImageMetadata metadata = new ImageMetadata("test.jpg", 12345L, "image/jpeg", "2023-08-21T12:34:56Z");
        when(imageService.getImageMetadata(anyString())).thenReturn(new ResponseEntity<>(metadata, HttpStatus.OK));

        ResponseEntity<ImageMetadata> response = imageController.getImageMetadata("test.jpg");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(metadata, response.getBody());
    }

    @Test
    void testGetRandomImageMetadata() {
        ImageMetadata metadata = new ImageMetadata("random.jpg", 54321L, "image/png", "2023-08-21T12:34:56Z");
        when(imageService.getRandomImageMetadata()).thenReturn(new ResponseEntity<>(metadata, HttpStatus.OK));

        ResponseEntity<ImageMetadata> response = imageController.getRandomImageMetadata();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(metadata, response.getBody());
    }

    @Test
    void testDeleteImage() {
        when(imageService.deleteImage(anyString())).thenReturn(new ResponseEntity<>("File deleted successfully", HttpStatus.OK));

        ResponseEntity<String> response = imageController.deleteImage("test.jpg");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("File deleted successfully", response.getBody());
    }
}