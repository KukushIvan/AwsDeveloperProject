package com.epam.aws;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.amazonaws.util.EC2MetadataUtils;

@RestController
public class RegionController {
    @GetMapping("/region-info")
    public String getRegionInfo() {
        String region = EC2MetadataUtils.getEC2InstanceRegion();
        String az = EC2MetadataUtils.getAvailabilityZone();
        return "Region: " + region + ", AZ: " + az;
    }
}
