package com.epam.aws;

import com.epam.aws.model.SubscriptionEntity;
import com.epam.aws.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SubscriptionService {

    private final SnsClient snsClient;

    @Value("${aws.sns.topic.arn}")
    private String topicArn;

    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    public SubscriptionService(SnsClient snsClient, SubscriptionRepository subscriptionRepository) {
        this.snsClient = snsClient;
        this.subscriptionRepository = subscriptionRepository;
    }

    public ResponseEntity<String> subscribe(@RequestParam String email, List<String> messageTypes) {
        SubscribeRequest request = SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("email")
                .endpoint(email)
                .build();

        for (String messageType : messageTypes) {log.info("messageType = {}", messageType);}

        snsClient.subscribe(request);

        if (!messageTypes.isEmpty()) {
            //We could not set the filter here, since the subscriptionArn is "pending approval" until user confirm it
            //Thus, we will save info and update the filter after the confirmation
            SubscriptionEntity subscription = new SubscriptionEntity(email, messageTypes);
            subscriptionRepository.save(subscription);
        }


        log.info("{} subscribed", email);
        return ResponseEntity.ok("Subscription request received. Please check your email for confirmation.");
    }

    public ResponseEntity<String> unsubscribe(@RequestParam String email) {
        try {
            ListSubscriptionsByTopicRequest listRequest = ListSubscriptionsByTopicRequest.builder()
                    .topicArn(topicArn)
                    .build();

            snsClient.listSubscriptionsByTopic(listRequest).subscriptions().stream()
                    .filter(subscription -> subscription.endpoint().equals(email))
                    .findFirst()
                    .ifPresent(subscription -> {
                        UnsubscribeRequest unsubscribeRequest = UnsubscribeRequest.builder()
                                .subscriptionArn(subscription.subscriptionArn())
                                .build();
                        snsClient.unsubscribe(unsubscribeRequest);
                    });
            log.info("{} unsubscribed", email);
            return ResponseEntity.ok("Unsubscription request received. You will no longer receive notifications.");
        } catch (Exception ex){
            log.error("Error occurred while unsubscribing", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while processing your unsubscription request. Please try again later.");
        }
    }

    @Scheduled(fixedRateString = "PT1M")
    @Transactional
    public void checkAndProcessSubscriptions() {
        // Getting all the subscriptions waiting for filter set
        List<SubscriptionEntity> subscriptions = subscriptionRepository.findAll();
        if (subscriptions.isEmpty()) {return;}
        // Getting all subscriptions of topicArn with one request
        ListSubscriptionsByTopicRequest request = ListSubscriptionsByTopicRequest.builder()
                .topicArn(topicArn)
                .build();
        ListSubscriptionsByTopicResponse response = snsClient.listSubscriptionsByTopic(request);

        // Find the created subscriptionArn for the email to set the filter

        Map<String, String> emailToSubscriptionArnMap = response.subscriptions().stream()
                .filter(subscription -> subscription.subscriptionArn() != null)
                .filter(subscription -> subscription.subscriptionArn().startsWith("arn:aws:sns:"))
                .collect(Collectors.toMap(Subscription::endpoint, Subscription::subscriptionArn));


        for (SubscriptionEntity entity : subscriptions) {
            String email = entity.getEmail();
            String subscriptionArn = emailToSubscriptionArnMap.get(email);
            log.info("Setting the filter for {} with the subscription URL {}",
                    email, subscriptionArn);

            if (subscriptionArn != null) {
                addFilterToSubscription(subscriptionArn, entity.getMessageTypes());
                subscriptionRepository.delete(entity);
            }
        }
    }

    private void addFilterToSubscription(String subscriptionArn, List<String> messageTypes) {
        String filterPolicy = messageTypes.stream()
                .map(type -> "\"" + type + "\"")
                .collect(Collectors.joining(",", "{ \"imageExtension\": [", "] }"));

        SetSubscriptionAttributesRequest setSubscriptionAttributesRequest = SetSubscriptionAttributesRequest.builder()
                .subscriptionArn(subscriptionArn)
                .attributeName("FilterPolicy")
                .attributeValue(filterPolicy)
                .build();


        snsClient.setSubscriptionAttributes(setSubscriptionAttributesRequest);
    }
}
