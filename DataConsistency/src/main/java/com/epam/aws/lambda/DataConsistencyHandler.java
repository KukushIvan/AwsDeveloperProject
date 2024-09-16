package com.epam.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DataConsistencyHandler implements RequestHandler<Map<String, Object>, String> {
    private Connection dbConnection = null;
    private S3Client s3Client = null;

    public DataConsistencyHandler(Connection dbConnection, S3Client s3Client) {
        this.dbConnection = dbConnection;
        this.s3Client = s3Client;
    }

    private Connection getDbConnection() {
        if (dbConnection == null) {
            dbConnection = initDbConnection();
        }
        return dbConnection;
    }

    private S3Client getS3Client() {
        if (s3Client == null) {
            String region = System.getenv("AWS_REGION");
            s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }
        return s3Client;
    }


    public DataConsistencyHandler() {
        //this is required for lambda initialization
        this.dbConnection = getDbConnection();
        this.s3Client = getS3Client();
    }


    private Connection initDbConnection() {
        try {
            String dbEndpoint = System.getenv("DB_ENDPOINT");
            String dbName = System.getenv("DB_NAME");
            String dbUsername = System.getenv("DB_USERNAME");
            String dbPassword = System.getenv("DB_PASSWORD");
            String dbUrl = "jdbc:mysql://" + dbEndpoint + "/" + dbName;
            return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize DB connection", e);
        }
    }

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        // Get the trigger type from the detail-type field
        String detailType = (String) input.get("detail-type");
        if (detailType == null) {
            context.getLogger().log("Missing 'detail-type' in the input\n");
            detailType = "Unknown";
        }

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

        Set<String> dbFileNames = getFileNamesFromDb(dbQuery, context);

        if (dbFileNames.isEmpty()) {
            context.getLogger().log("No files found in DB or an error occurred");
            return false;
        }

        context.getLogger().log("Fetched " + dbFileNames.size() + " file names from DB");

        Set<String> s3FileNames = getAllS3FileNames(s3BucketName, context);

        context.getLogger().log("Fetched " + s3FileNames.size() + " file names from S3");

        for (String dbFileName : dbFileNames) {
            if (!s3FileNames.contains(dbFileName)) {
                context.getLogger().log("File " + dbFileName + " from DB not found in S3");
                return false;
            }
        }

        context.getLogger().log("Data consistency check passed\n");
        return true;
    }

    private Set<String> getFileNamesFromDb(String dbQuery, Context context) {
        Set<String> fileNames = new HashSet<>();
        try (Statement statement = dbConnection.createStatement();
             ResultSet resultSet = statement.executeQuery(dbQuery)) {
            if (Objects.isNull(resultSet)) {
                context.getLogger().log("Nothings received from the DB");
                return fileNames;
            }
            while (resultSet.next()) {
                fileNames.add(resultSet.getString("file_name"));
            }
        } catch (SQLException e) {
            context.getLogger().log("Error querying database: " + e.getMessage());
        }
        return fileNames;
    }

    private Set<String> getAllS3FileNames(String bucketName, Context context) {
        Set<String> s3FileNames = new HashSet<>();
        String continuationToken = null;
        try {
            do {
                ListObjectsV2Request request = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .continuationToken(continuationToken)
                        .build();

                ListObjectsV2Response response = s3Client.listObjectsV2(request);

                s3FileNames.addAll(response.contents().stream()
                        .map(S3Object::key)
                        .collect(Collectors.toSet()));

                continuationToken = response.nextContinuationToken();
            } while (continuationToken != null);
        } catch (S3Exception e) {
            context.getLogger().log("Error while retrieving objects from S3: " + e.awsErrorDetails().errorMessage());
            throw e;
        }
        return s3FileNames;
    }
}
