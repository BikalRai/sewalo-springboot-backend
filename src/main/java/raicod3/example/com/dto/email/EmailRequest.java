package raicod3.example.com.dto.email;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class EmailRequest {

    private String email;
    private String subject;
    private String body;
    private String otpToken;
    private String templatePath;

    public EmailRequest(String email) {
        this.email = email;
    }
}
