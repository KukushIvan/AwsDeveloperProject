package com.epam.aws.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@NoArgsConstructor
public class SubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private Long id;
    @Getter
    @Setter
    private String email;

    private String messageTypes;

    public SubscriptionEntity(String email, List<String> messageTypes) {
        this.email = email;
        this.messageTypes = String.join(",", messageTypes);
    }

    public List<String> getMessageTypes() {
        return List.of(messageTypes.split(","));
    }

    public void setMessageTypes(List<String> messageTypes) {
        this.messageTypes = String.join(",", messageTypes);
    }
}

