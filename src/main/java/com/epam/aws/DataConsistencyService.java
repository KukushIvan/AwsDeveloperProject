package com.epam.aws;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

@Service
public class DataConsistencyService {

    private final LambdaClient lambdaClient;

    public DataConsistencyService(LambdaClient lambdaClient) {
        this.lambdaClient = lambdaClient;
    }

    public String triggerDataConsistencyLambda() {
        String functionName = "AwsDeveloper-Project-DataConsistencyFunction";

        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName(functionName)
                .payload(SdkBytes.fromUtf8String("{\"detail-type\": \"WebApp\"}"))
                .build();

        InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);

        return invokeResponse.payload().asUtf8String();
    }
}
