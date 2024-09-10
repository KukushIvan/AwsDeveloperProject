package com.epam.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

public class UploadsNotificationHandler implements RequestHandler<SQSEvent, String> {
    private static final Logger logger = LoggerFactory.getLogger(UploadsNotificationHandler.class);

    SnsClient snsClient;
    private final String topicArn;

    public UploadsNotificationHandler() {
        this.snsClient = SnsClient.builder().build();
        this.topicArn = System.getenv("SNS_TOPIC_ARN");
        if (this.topicArn == null || this.topicArn.isEmpty()) {
            throw new RuntimeException("Environment variable SNS_TOPIC_ARN is not set");
        }
    }
    @Override
    public String handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                String body = message.getBody();


                // Pushing to SNS
                PublishRequest publishRequest = PublishRequest.builder()
                        .topicArn(topicArn)
                        .message(body)
                        .messageAttributes(Map.of(
                                "imageExtension", MessageAttributeValue.builder()
                                        .dataType("String")
                                        .stringValue(extractFileExtension(body))
                                        .build()))
                        .build();

                snsClient.publish(publishRequest);
            } catch (Exception e) {
                logger.error("Failed to process message: {}", message.getBody(), e);
                throw e;
            }
        }
        return "Success";
    }

    private static int getStartIndex(String messageBody) {
        if (messageBody == null || messageBody.isBlank()) {
            throw new IllegalArgumentException("Message body is empty or null");
        }

        String extensionMarker = "- Extension: ";
        // Find the starting index of the extension marker
        int startIndex = messageBody.indexOf(extensionMarker);

        // If the marker is not found, throw an exception
        if (startIndex == -1) {
            throw new IllegalArgumentException("No extension marker found in message body");
        }

        // Move the start index to the position right after the marker
        startIndex += extensionMarker.length();
        return startIndex;
    }

    private String extractFileExtension(String messageBody) {
        // Check if the message body is null or contains only whitespace
        int startIndex = getStartIndex(messageBody);
        // Extract the extension by splitting at the newline and trimming any extra spaces
        String extension = messageBody.substring(startIndex).split("\n")[0].trim();

        // If the extracted extension is empty, throw an exception
        if (extension.isEmpty()) {
            throw new IllegalArgumentException("File extension is empty");
        }

        // Return the extension in lowercase for consistency
        return extension.toLowerCase();
    }
}
