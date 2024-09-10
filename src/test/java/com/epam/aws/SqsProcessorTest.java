package com.epam.aws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SqsProcessorTest {

    @Mock
    private SqsClient sqsClient;


    private final String queueUrl = "http://localhost:4566/000000000000/test-queue";

    @InjectMocks
    private SqsProcessor sqsProcessor;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this).close();
        sqsProcessor = new SqsProcessor(sqsClient, queueUrl);
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
