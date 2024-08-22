
package com.epam.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

@Service
public class ImageService {

    private final JdbcTemplate jdbcTemplate;

    private final AmazonS3 s3Client;
    private final String bucketName;

    @Autowired
    public ImageService(JdbcTemplate jdbcTemplate, AmazonS3 s3Client) {
        this.jdbcTemplate = jdbcTemplate;
        this.s3Client = s3Client;

        AWSSimpleSystemsManagement ssmClient = AWSSimpleSystemsManagementClientBuilder.defaultClient();
        GetParameterRequest parameterRequest = new GetParameterRequest().withName("/config/s3/bucketName");
        parameterRequest.setWithDecryption(true);
        GetParameterResult parameterResult = ssmClient.getParameter(parameterRequest);
        this.bucketName = parameterResult.getParameter().getValue();
    }


    public ResponseEntity<String> uploadImage(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            s3Client.putObject(new PutObjectRequest(bucketName, file.getOriginalFilename(), file.getInputStream(), metadata));

            // Saving metadata to RDS
            String sql = "INSERT INTO image_metadata (file_name, file_size, content_type) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, file.getOriginalFilename(), file.getSize(), file.getContentType());

            return ResponseEntity.ok("File uploaded successfully: " + file.getOriginalFilename());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not upload the file: " + ex.getMessage());
        }
    }

    public ResponseEntity<ImageMetadata> getImageMetadata(String imageName) {
        try {
            String sql = "SELECT * FROM image_metadata WHERE file_name = ?";
            ImageMetadata metadata = jdbcTemplate.query(
                    sql,
                    ps -> ps.setString(1, imageName),
                    new BeanPropertyRowMapper<>(ImageMetadata.class)
            ).stream().findFirst().orElse(null);

            if (metadata != null) {
                return ResponseEntity.ok(metadata);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<ImageMetadata> getRandomImageMetadata() {
        try {
            String sql = "SELECT * FROM image_metadata ORDER BY RAND() LIMIT 1";
            ImageMetadata metadata = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper<>(ImageMetadata.class)
            ).stream().findFirst().orElse(null);

            if (metadata != null) {
                return ResponseEntity.ok(metadata);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<Resource> downloadImage(String imageName) {
        try {
            URL url = s3Client.getUrl(bucketName, imageName);
            Resource resource = new UrlResource(url);

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<String> deleteImage(String imageName) {
        try {
            s3Client.deleteObject(bucketName, imageName);

            // Deleting metadata from RDS
            String sql = "DELETE FROM image_metadata WHERE file_name = ?";
            jdbcTemplate.update(sql, imageName);

            return ResponseEntity.ok("File deleted successfully");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not delete the file: " + ex.getMessage());
        }
    }
}
