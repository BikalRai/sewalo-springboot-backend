package raicod3.example.com.exception;

public class MethodArgumentNotValidException extends RuntimeException {
    public MethodArgumentNotValidException(String message) {
        super(message);
    }
}
