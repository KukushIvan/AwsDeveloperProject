
package com.epam.aws;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import com.epam.aws.model.ImageMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class ImageService {

    private final S3Client s3Client;
    final String bucketName;

    private final JdbcTemplate jdbcTemplate;

    private final SqsProcessor sqsProcessor;

    @Autowired
    public ImageService(S3Client s3Client, JdbcTemplate jdbcTemplate, SqsProcessor sqsProcessor) {
        this.s3Client = s3Client;
        this.jdbcTemplate = jdbcTemplate;
        this.bucketName = System.getenv("S3_BUCKET_NAME");
        this.sqsProcessor = sqsProcessor;
    }

    @PostConstruct
    public void init() {
        if (bucketName == null) {
            throw new IllegalStateException("S3_BUCKET_NAME environment variable is not set.");
        }

        log.info("ImageService initialized successfully with bucket: {}", bucketName);
    }

    public ResponseEntity<String> uploadImage(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // Extract metadata
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.contains(".")) {
                return ResponseEntity.badRequest().body("File extension is not supported");
            }
            String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
            long fileSize = file.getSize();
            String lastUpdateDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

            // Save file to S3
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileName)
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));

            // Save metadata to RDS
            String sql = "INSERT INTO image_metadata (file_name, file_size, file_extension, last_update_date) VALUES (?, ?, ?, ?)";


            jdbcTemplate.update(sql, fileName, fileSize, fileExtension, lastUpdateDate);

            String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/images/download/")
                    .path(fileName)
                    .toUriString();

            // Send message to SQS
            String message = String.format(
                    """
                            Image uploaded successfully!
                            
                            Details:
                            - Name: %s
                            - Size: %d bytes
                            - Extension: %s
                            
                            You can download the image using the following link:
                            %s""",
                    fileName, fileSize, fileExtension, downloadUrl
            );

            sqsProcessor.sendMessage(message);

            return ResponseEntity.ok("File uploaded successfully: " + fileName);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not upload the file: " + e.getMessage());
        }
    }

    public ResponseEntity<String> deleteImage(String imageName) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(imageName)
                    .build());

            // Delete metadata from RDS
            String sql = "DELETE FROM image_metadata WHERE file_name = ?";
            jdbcTemplate.update(sql, imageName);

            return ResponseEntity.ok("File deleted successfully");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not delete the file: " + e.getMessage());
        }
    }

    public ResponseEntity<ImageMetadata> getImageMetadata(String imageName) {
        String sql = "SELECT * FROM image_metadata WHERE file_name = ?";
        try {
            ImageMetadata metadata = jdbcTemplate.queryForObject(sql, new ImageMetadataRowMapper(), imageName);
            return ResponseEntity.ok(metadata);
        } catch (EmptyResultDataAccessException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<ImageMetadata> getRandomImageMetadata() {
        String sql = "SELECT * FROM image_metadata ORDER BY RAND() LIMIT 1";
        try {
            ImageMetadata metadata = jdbcTemplate.queryForObject(sql, new ImageMetadataRowMapper());
            return ResponseEntity.ok(metadata);
        } catch (EmptyResultDataAccessException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<Resource> downloadImage(String imageName) {
        try (var s3ObjectInputStream = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(imageName)
                .build())) {

            byte[] imageBytes = s3ObjectInputStream.readAllBytes();
            Resource resource = new ByteArrayResource(imageBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + imageName + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

    }
}
