package raicod3.example.com.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {
    List<String> storeImages(List<MultipartFile> files);
}
