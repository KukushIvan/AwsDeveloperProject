package com.epam.aws;

import com.epam.aws.model.SubscriptionEntity;
import com.epam.aws.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubscriptionServiceTest {

    @Mock
    private SnsClient snsClient;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this).close();
    }

    @Test
    void subscribe_ShouldSaveSubscription_WhenMessageTypesAreProvided() {
        String email = "test@example.com";
        List<String> messageTypes = List.of("jpg", "png");

        ResponseEntity<String> response = subscriptionService.subscribe(email, messageTypes);

        verify(subscriptionRepository, times(1)).save(any(SubscriptionEntity.class));
        verify(snsClient, times(1)).subscribe(any(SubscribeRequest.class));
        assertEquals("Subscription request received. Please check your email for confirmation.", response.getBody());
    }

    @Test
    void subscribe_ShouldNotSaveSubscription_WhenMessageTypesAreEmpty() {
        String email = "test@example.com";
        List<String> messageTypes = List.of();

        ResponseEntity<String> response = subscriptionService.subscribe(email, messageTypes);

        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
        verify(snsClient, times(1)).subscribe(any(SubscribeRequest.class));
        assertEquals("Subscription request received. Please check your email for confirmation.", response.getBody());
    }

    @Test
    void unsubscribe_ShouldUnsubscribeUser_WhenEmailIsValid() {
        String email = "test@example.com";
        Subscription subscription = Subscription.builder()
                .subscriptionArn("arn:aws:sns:us-west-2:123456789012:test-subscription")
                .endpoint(email)
                .build();
        ListSubscriptionsByTopicResponse mockResponse = ListSubscriptionsByTopicResponse.builder()
                .subscriptions(List.of(subscription))
                .build();

        when(snsClient.listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class)))
                .thenReturn(mockResponse);

        ResponseEntity<String> response = subscriptionService.unsubscribe(email);

        verify(snsClient, times(1)).unsubscribe(any(UnsubscribeRequest.class));
        assertEquals("Unsubscription request received. You will no longer receive notifications.", response.getBody());
    }

    @Test
    void checkAndProcessSubscriptions_ShouldAddFilterToSubscription_WhenSubscriptionIsConfirmed() {
        SubscriptionEntity entity = new SubscriptionEntity("test@example.com", List.of("jpg", "png"));
        when(subscriptionRepository.findAll()).thenReturn(List.of(entity));

        Subscription subscription = Subscription.builder()
                .subscriptionArn("arn:aws:sns:us-west-2:123456789012:test-subscription")
                .endpoint("test@example.com")
                .build();
        ListSubscriptionsByTopicResponse response = ListSubscriptionsByTopicResponse.builder()
                .subscriptions(List.of(subscription))
                .build();
        when(snsClient.listSubscriptionsByTopic(any(ListSubscriptionsByTopicRequest.class)))
                .thenReturn(response);

        subscriptionService.checkAndProcessSubscriptions();

        verify(snsClient, times(1)).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
        verify(subscriptionRepository, times(1)).delete(entity);
    }

    @Test
    void checkAndProcessSubscriptions_ShouldDoNothing_WhenNoSubscriptionsFound() {
        when(subscriptionRepository.findAll()).thenReturn(List.of());

        subscriptionService.checkAndProcessSubscriptions();

        verify(snsClient, never()).setSubscriptionAttributes(any(SetSubscriptionAttributesRequest.class));
    }
}
