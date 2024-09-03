package com.epam.aws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionController subscriptionController;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this).close();
    }

    @Test
    void testSubscribeSuccess() {
        String email = "test@example.com";
        List<String> messageTypes = List.of("jpg", "jpeg");

        ResponseEntity<String> expectedResponse = new ResponseEntity<>("Subscription request received. Please check your email for confirmation.", HttpStatus.OK);
        when(subscriptionService.subscribe(anyString(), anyList())).thenReturn(expectedResponse);

        ResponseEntity<String> response = subscriptionController.subscribe(email, messageTypes);

        assertEquals(expectedResponse, response);
        verify(subscriptionService, times(1)).subscribe(email, messageTypes);
    }

    @Test
    void testSubscribeWithoutMessageTypes() {
        String email = "test@example.com";
        List<String> messageTypes = List.of(); // Пустой список, если messageTypes не передан

        ResponseEntity<String> expectedResponse = new ResponseEntity<>("Subscription request received. Please check your email for confirmation.", HttpStatus.OK);
        when(subscriptionService.subscribe(anyString(), anyList())).thenReturn(expectedResponse);

        ResponseEntity<String> response = subscriptionController.subscribe(email, messageTypes);

        assertEquals(expectedResponse, response);
        verify(subscriptionService, times(1)).subscribe(email, messageTypes);
    }

    @Test
    void testUnsubscribeSuccess() {
        String email = "test@example.com";

        ResponseEntity<String> expectedResponse = new ResponseEntity<>("Unsubscription request received. You will no longer receive notifications.", HttpStatus.OK);
        when(subscriptionService.unsubscribe(anyString())).thenReturn(expectedResponse);

        ResponseEntity<String> response = subscriptionController.unsubscribe(email);

        assertEquals(expectedResponse, response);
        verify(subscriptionService, times(1)).unsubscribe(email);
    }

    @Test
    void testUnsubscribeFailure() {
        String email = "test@example.com";

        ResponseEntity<String> expectedResponse = new ResponseEntity<>("An error occurred while processing your unsubscription request. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
        when(subscriptionService.unsubscribe(anyString())).thenReturn(expectedResponse);

        ResponseEntity<String> response = subscriptionController.unsubscribe(email);

        assertEquals(expectedResponse, response);
        verify(subscriptionService, times(1)).unsubscribe(email);
    }
}
