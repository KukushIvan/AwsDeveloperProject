package com.epam.aws;

import com.amazonaws.util.EC2MetadataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class RegionControllerTest {

    @InjectMocks
    private RegionController regionController;

    private MockedStatic<EC2MetadataUtils> ec2MetadataUtilsMock;

    @BeforeEach
    void setUp() {
        ec2MetadataUtilsMock = mockStatic(EC2MetadataUtils.class);
    }

    @Test
    void testGetRegionInfo() {
        String expectedRegion = "us-west-2";
        String expectedAz = "us-west-2a";

        ec2MetadataUtilsMock.when(EC2MetadataUtils::getEC2InstanceRegion).thenReturn(expectedRegion);
        ec2MetadataUtilsMock.when(EC2MetadataUtils::getAvailabilityZone).thenReturn(expectedAz);

        String response = regionController.getRegionInfo();

        String expectedResponse = "{\"Region\": \"" + expectedRegion + "\", \"AZ\": \"" + expectedAz + "\"}";
        assertEquals(expectedResponse, response);

        ec2MetadataUtilsMock.verify(EC2MetadataUtils::getEC2InstanceRegion);
        ec2MetadataUtilsMock.verify(EC2MetadataUtils::getAvailabilityZone);
    }
}
