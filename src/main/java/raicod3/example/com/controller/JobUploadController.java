package raicod3.example.com.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import raicod3.example.com.config.RabbitMQConfig;
import raicod3.example.com.constants.Http_Constants;
import raicod3.example.com.enums.ErrorCode;
import raicod3.example.com.payload.ImageProcessingTask;
import raicod3.example.com.utilities.APIResponse;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobUploadController {
    private final RabbitTemplate rabbitTemplate;

    @PostMapping("/upload-draft")
    public ResponseEntity<APIResponse> uploadJobImageDraft(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String username
    ) {
        if (file.isEmpty()) {
            return new ResponseEntity<>(
                    APIResponse.error("File cannot be empty", Http_Constants.BAD_REQUEST, ErrorCode.ERR_BAD),
                    HttpStatus.BAD_REQUEST
            );
        }

        try {
            log.info("User '{}' is uploading a job image draft", username);

            // 1. TODO: Save a draft job to your database
            Long mockJobId = 105L;

            // 2. Convert bytes to Base64
            byte[] fileBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(fileBytes);

            // 3. Construct payload
            ImageProcessingTask task = new ImageProcessingTask(mockJobId, username, base64Image);

            // 4. Push to RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.JOB_ANALYSIS_ROUTING_KEY,
                    task
            );

            log.info("Successfully pushed Job ID {} to the analysis queue", mockJobId);

            // 5. Wrap the data in your standard APIResponse
            Map<String, Object> responseData = Map.of(
                    "jobId", mockJobId,
                    "status", "DRAFT"
            );

            return ResponseEntity.ok(
                    APIResponse.success(
                            responseData,
                            "Image accepted. Processing started in the background.",
                            HttpStatus.OK.value() // Assuming you use standard int values here
                    )
            );

        } catch (IOException e) {
            log.error("Failed to read image bytes during upload", e);

            // Return your standard APIResponse for errors
            return new ResponseEntity<>(
                    APIResponse.error(
                            "Failed to parse image file",
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            ErrorCode.ERR_BAD // Update this enum if you have a specific SERVER_ERROR code
                    ),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

}
