package com.epam.aws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SqsProcessorTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private SnsClient snsClient;

    private final String queueUrl = "http://localhost:4566/000000000000/test-queue";
    private final String topicArn = "arn:aws:sns:us-west-2:000000000000:test-topic";

    @InjectMocks
    private SqsProcessor sqsProcessor;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this).close();
        when(sqsClient.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());

        when(snsClient.getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenReturn(GetTopicAttributesResponse.builder().attributes(Map.of("TopicArn", topicArn)).build());
        sqsProcessor = new SqsProcessor(sqsClient, snsClient, queueUrl, topicArn);
    }

    @Test
    void testProcessMessages() {
        Message message = Message.builder()
                .body("Sample message\n- Extension: jpg\nMore text here")
                .receiptHandle("receipt-handle")
                .build();

        ReceiveMessageResponse receiveResponse = ReceiveMessageResponse.builder()
                .messages(List.of(message))
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(receiveResponse);

        sqsProcessor.processMessages();

        verify(snsClient, times(1)).publish(argThat((PublishRequest request) ->
                request.topicArn().equals(topicArn) &&
                        request.message().equals(message.body()) &&
                        request.messageAttributes().get("imageExtension").stringValue().equals("jpg")
        ));

        verify(sqsClient, times(1)).deleteMessage(argThat((DeleteMessageRequest deleteRequest) ->
                deleteRequest.queueUrl().equals(queueUrl) &&
                        deleteRequest.receiptHandle().equals("receipt-handle")
        ));
    }


    @Test
    void testProcessMessagesWithEmptyExtensionThrowsException() {
        // Подготовка данных

        String messageBody = """
                            Image uploaded successfully!
                            
                            Details:
                            - Name: image
                            - Size: 100 bytes
                            - Extension:
                            
                            You can download the image using the following link:
                            %s""";

        Message message = Message.builder()
                .body(messageBody)
                .receiptHandle("receipt-handle")
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(message)).build());

        try {
            sqsProcessor.processMessages();
        } catch (IllegalArgumentException ex) {
            assertEquals("No extension marker found in message body", ex.getMessage());
        }

        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void testSendMessage() {
        String message = "Test SQS message";

        sqsProcessor.sendMessage(message);

        verify(sqsClient, times(1)).sendMessage(argThat((SendMessageRequest sendRequest) ->
                sendRequest.queueUrl().equals(queueUrl) &&
                        sendRequest.messageBody().equals(message)
        ));
    }
}
