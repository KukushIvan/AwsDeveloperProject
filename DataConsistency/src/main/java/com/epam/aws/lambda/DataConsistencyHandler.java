package com.epam.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public class DataConsistencyHandler implements RequestHandler<Map<String, Object>, String> {

    private static Connection dbConnection = null;
    private static S3Client s3Client = null;

    public DataConsistencyHandler(Connection dbConnection, S3Client s3Client) {
        DataConsistencyHandler.dbConnection = dbConnection;
        DataConsistencyHandler.s3Client = s3Client;
    }

    private static Connection getDbConnection() {
        if (dbConnection == null) {
            dbConnection = initDbConnection();
        }
        return dbConnection;
    }

    private static S3Client getS3Client() {
        if (s3Client == null) {
            s3Client = S3Client.builder()
                    .region(Region.of(System.getenv("AWS_REGION")))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }
        return s3Client;
    }


    public DataConsistencyHandler() {
        //this is required for lambda initialization
    }


    private static Connection initDbConnection() {
        try {
            String dbUrl = System.getenv("DB_ENDPOINT");
            String dbUsername = System.getenv("DB_USERNAME");
            String dbPassword = System.getenv("DB_PASSWORD");
            return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize DB connection", e);
        }
    }

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        // Get the trigger type from the detail-type field
        String detailType = (String) input.get("detail-type");
        String triggerSource = switch (detailType) {
            case "Scheduled Event" -> "EventBridge schedule";
            case "APIGateway" -> "API Gateway";
            case "WebApp" -> "WebApp";
            default -> "Unknown trigger";
        };

        context.getLogger().log("Triggered by " + triggerSource + "\n");

        boolean isDataConsistent = checkDataConsistency(context);

        return isDataConsistent ? "Data is consistent" : "Data inconsistency detected";
    }

    /**
     * Checks data consistency between RDS and S3
     */
    private boolean checkDataConsistency(Context context) {
        String dbQuery = "SELECT file_name FROM image_metadata";
        String s3BucketName = System.getenv("S3_BUCKET");

        try (Statement statement = getDbConnection().createStatement();
             ResultSet resultSet = statement.executeQuery(dbQuery)) {

            // Check each record from the database
            while (resultSet.next()) {
                String imageName = resultSet.getString("file_name");

                // Check if the object with this name exists in S3
                if (!doesObjectExist(s3BucketName, imageName)) {
                    context.getLogger().log("Image " + imageName + " not found in S3\n");
                    return false;
                }
            }
        } catch (SQLException e) {
            context.getLogger().log("Error querying database: " + e.getMessage() + "\n");
            return false;
        }

        context.getLogger().log("Data consistency check passed\n");
        return true;
    }

    public static boolean doesObjectExist(String bucketName, String objectKey) {
        try {
            getS3Client().headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
