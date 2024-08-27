
package com.epam.aws;

import org.springframework.core.io.ByteArrayResource;
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
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;

@Service
public class ImageService {

    @Autowired
    private S3Client s3Client;
    String bucketName;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public ImageService(S3Client s3Client, JdbcTemplate jdbcTemplate) {
        this.s3Client = s3Client;
        this.jdbcTemplate = jdbcTemplate;
        this.bucketName = System.getenv("S3_BUCKET_NAME");
    }

    @PostConstruct
    public void init() {
        // Initialize variables from environment variables
        bucketName = System.getenv("S3_BUCKET_NAME");
        String dbHost = System.getenv("DB_HOST");
        String dbName = System.getenv("DB_NAME");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        // Логирование значений переменных окружения
        System.out.println("DB_HOST: " + System.getenv("DB_HOST"));
        System.out.println("DB_NAME: " + System.getenv("DB_NAME"));
        System.out.println("DB_USER: " + System.getenv("DB_USER"));
        System.out.println("DB_PASSWORD: " + System.getenv("DB_PASSWORD"));

        try {
            // Configure DataSource for JdbcTemplate
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl("jdbc:mysql://" + dbHost + "/" + dbName);
            dataSource.setUsername(dbUser);
            dataSource.setPassword(dbPassword);
            // Логирование перед созданием JdbcTemplate
            System.out.println("DataSource configured successfully.");

            this.jdbcTemplate = new JdbcTemplate(dataSource);

            // Логирование после создания JdbcTemplate
            System.out.println("JdbcTemplate initialized successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error occurred during DataSource or JdbcTemplate initialization: " + e.getMessage());
        }


    }

    public ResponseEntity<String> uploadImage(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // Extract metadata
            String fileName = file.getOriginalFilename();
            String fileExtension = "";
            if (fileName != null && fileName.contains(".")) {
                fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
            }
            long fileSize = file.getSize();
            String lastUpdateDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

            // Save file to S3
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileName)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), fileSize)
            );

            // Save metadata to RDS
            String sql = "INSERT INTO image_metadata (file_name, file_size, file_extension, last_update_date) VALUES (?, ?, ?, ?)";


            jdbcTemplate.update(sql, fileName, fileSize, fileExtension, lastUpdateDate);

            return ResponseEntity.ok("File uploaded successfully: " + fileName);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not upload the file: " + e.getMessage());
        }
    }

    public ResponseEntity<String> deleteImage(String imageName) {
        try {
            // Delete file from S3
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
        ImageMetadata metadata = jdbcTemplate.queryForObject(sql, new Object[]{imageName}, new ImageMetadataRowMapper());
        return ResponseEntity.ok(metadata);
    }

    public ResponseEntity<ImageMetadata> getRandomImageMetadata() {
        String sql = "SELECT * FROM image_metadata ORDER BY RAND() LIMIT 1";
        ImageMetadata metadata = jdbcTemplate.queryForObject(sql, new ImageMetadataRowMapper());
        return ResponseEntity.ok(metadata);
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
