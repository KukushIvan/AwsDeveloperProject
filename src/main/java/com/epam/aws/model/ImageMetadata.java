
package com.epam.aws.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ImageMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private long fileSize;
    private String fileExtension;
    private Date lastUpdateDate;

    public ImageMetadata(String fileName, long fileSize, String fileExtension, Date lastUpdateDate) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileExtension = fileExtension;
        this.lastUpdateDate = lastUpdateDate;
    }
}
