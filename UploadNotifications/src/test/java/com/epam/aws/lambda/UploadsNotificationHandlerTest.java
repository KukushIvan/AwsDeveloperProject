package com.epam.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
class UploadsNotificationHandlerTest {
    @SystemStub
    private EnvironmentVariables variables =
            new EnvironmentVariables("SNS_TOPIC_ARN", "arn:aws:sns:us-west-2:000000000000:test-topic");

    private UploadsNotificationHandler handler;
    private SnsClient mockSnsClient;
    private Context context;

    @BeforeEach
    public void setUp() {
        handler = new UploadsNotificationHandler();
        mockSnsClient = mock(SnsClient.class);
        handler.snsClient = mockSnsClient;
        context = mock(Context.class);
    }

    @Test
    void testHandleRequestWithValidMessage() {
        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setBody("Some message - Extension: jpg");
        event.setRecords(Collections.singletonList(message));

        handler.handleRequest(event, context);
        String topic = System.getenv("SNS_TOPIC_ARN");

        PublishRequest expectedPublishRequest = PublishRequest.builder()
                .topicArn(topic)
                .message("Some message - Extension: jpg")
                .messageAttributes(Collections.singletonMap(
                        "imageExtension", MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue("jpg")
                                .build()))
                .build();

        verify(mockSnsClient).publish(expectedPublishRequest);
    }

    @Test
    void testHandleRequestWithEmptyMessageBody() {
        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setBody("");
        event.setRecords(Collections.singletonList(message));

        assertThrows(IllegalArgumentException.class, () -> handler.handleRequest(event, context));

        verify(mockSnsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void testHandleRequestWithNoExtensionMarker() {
        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setBody("Some invalid message");
        event.setRecords(Collections.singletonList(message));

        assertThrows(IllegalArgumentException.class, () -> handler.handleRequest(event, context));

        verify(mockSnsClient, never()).publish(any(PublishRequest.class));
    }
}
