package raicod3.example.com.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import raicod3.example.com.exception.BadRequestException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadDir = Paths.get("uploads");

    public LocalFileStorageService() {
        try {
            // Create the directory on startup if it doesn't exist
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

  @Override
  public List<String> storeImages(List<MultipartFile> files) {
        List<String> storedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            try {
                // Generate a unique filename to prevent overwriting
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path targetLocation = this.uploadDir.resolve(filename);

                // Save the file to disk
                Files.copy(file.getInputStream(), targetLocation);

                // Add the path to our list (Ollama will read from this path later)
                storedUrls.add(targetLocation.toString());

            } catch (IOException ex) {
                log.error("Failed to store file", ex);
                throw new BadRequestException("Failed to store file: " + file.getOriginalFilename());
            }
        }

        return storedUrls;
    }
}
