
package com.epam.aws.repository;

import com.epam.aws.model.ImageMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageMetadataRepository extends JpaRepository<ImageMetadata, Long> {
    Optional<ImageMetadata> findByFileName(String name);
}
