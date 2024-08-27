
package com.epam.aws.model;

import lombok.*;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ImageMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private long size;
    private String extension;
    private Date lastUpdateDate;

    public ImageMetadata(String name, long size, String extension, Date lastUpdateDate) {
        this.name = name;
        this.size = size;
        this.extension = extension;
        this.lastUpdateDate = lastUpdateDate;
    }
}
