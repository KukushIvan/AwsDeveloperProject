
package com.epam.aws;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class ImageMetadata {

    private String fileName;
    private long size;
    private String type;
    private String creationTime;
}
