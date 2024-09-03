package com.epam.aws;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@Component
public class SqsProcessor {

    private final SqsClient sqsClient;

    private final SnsClient snsClient;

    @Value("${aws.sqs.queue.url}")
    private String queueUrl;

    @Value("${aws.sns.topic.arn}")
    private String topicArn;

    @Autowired
    public SqsProcessor(SqsClient sqsClient, SnsClient snsClient) {
        this.sqsClient = sqsClient;
        this.snsClient = snsClient;
    }

    @Scheduled(fixedRateString = "PT1M")
    @Transactional
    public void processMessages() {
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .build();

        sqsClient.receiveMessage(receiveRequest).messages().forEach(message -> {
            String body = message.body();

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

            // Removing from sqs
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();
            sqsClient.deleteMessage(deleteRequest);
        });
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

    protected void sendMessage(String message){
        // Send message to SQS
        SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message)
                .build();
        sqsClient.sendMessage(sendMsgRequest);
    }
}