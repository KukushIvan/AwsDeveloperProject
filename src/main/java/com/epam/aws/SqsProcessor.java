package com.epam.aws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
public class SqsProcessor {

    private final SqsClient sqsClient;

    private final String queueUrl;

    @Autowired
    public SqsProcessor(SqsClient sqsClient,
                        @Value("${aws.sqs.queue.url}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
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