package com.epam.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

public class S3LogsHandler implements RequestHandler<S3Event, String> {
    @Override
    public String handleRequest(S3Event s3Event, Context context) {
        s3Event.getRecords().forEach(s3Record -> {
            String bucketName = s3Record.getS3().getBucket().getName();
            String key = s3Record.getS3().getObject().getKey();
            context.getLogger().log("New object created in bucket: " + bucketName + " with key: " + key);
        });
        return "Event processed.";
    }
}
