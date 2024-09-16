package com.epam.aws;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataConsistencyController {

    private final DataConsistencyService dataConsistencyService;

    public DataConsistencyController(DataConsistencyService dataConsistencyService) {
        this.dataConsistencyService = dataConsistencyService;
    }

    @GetMapping("/trigger-data-consistency")
    public String triggerDataConsistencyCheck() {
        return dataConsistencyService.triggerDataConsistencyLambda();
    }
}
