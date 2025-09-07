package solid.humank.genaidemo.infrastructure.config;

/**
 * Database Configuration Exception
 * Thrown when database configuration or validation fails
 */
public class DatabaseConfigurationException extends RuntimeException {
    
    public DatabaseConfigurationException(String message) {
        super(message);
    }
    
    public DatabaseConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DatabaseConfigurationException(Throwable cause) {
        super(cause);
    }
}