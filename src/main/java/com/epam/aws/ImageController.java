
package com.epam.aws;

import com.epam.aws.model.ImageMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/images")
public class ImageController {
    private final ImageService imageService;

    @Autowired
    public ImageController(ImageService imageService){
        this.imageService = imageService;
    }

    @GetMapping("/download/{imageName}")
    public ResponseEntity<Resource> downloadImage(@PathVariable String imageName) {
        return imageService.downloadImage(imageName);
    }

    @GetMapping("/metadata/{imageName}")
    public ResponseEntity<ImageMetadata> getImageMetadata(@PathVariable String imageName) {
        return imageService.getImageMetadata(imageName);
    }

    @GetMapping("/metadata/random")
    public ResponseEntity<ImageMetadata> getRandomImageMetadata() {
        return imageService.getRandomImageMetadata();
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile file) {
        return imageService.uploadImage(file);
    }

    @DeleteMapping("/delete/{imageName}")
    public ResponseEntity<String> deleteImage(@PathVariable String imageName) {
        return imageService.deleteImage(imageName);
    }
}
