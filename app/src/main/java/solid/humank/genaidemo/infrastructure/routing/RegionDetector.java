package solid.humank.genaidemo.infrastructure.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

/**
 * RegionDetector automatically detects the current AWS region from multiple sources.
 * 
 * Detection priority:
 * 1. Environment variable AWS_REGION
 * 2. System property aws.region
 * 3. EC2 instance metadata service
 * 4. Availability zone parsing
 * 5. Default fallback to ap-northeast-1 (Taiwan)
 * 
 * This component is essential for Active-Active multi-region architecture,
 * enabling intelligent routing decisions based on the current deployment region.
 */
@Component
public class RegionDetector {    private static final Logger logger = LoggerFactory.getLogger(RegionDetector.class);
    
    private static final String EC2_METADATA_URL = "http://169.254.169.254/latest/meta-data/placement/region";
    private static final String EC2_AZ_METADATA_URL = "http://169.254.169.254/latest/meta-data/placement/availability-zone";
    private static final int METADATA_TIMEOUT_MS = 1000;
    private static final String DEFAULT_REGION = "ap-northeast-1"; // Taiwan
    
    private volatile String cachedRegion;
    
    /**
     * Detects the current AWS region using multiple detection strategies.
     * Results are cached after first successful detection.
     * 
     * @return the detected AWS region code (e.g., "ap-northeast-1")
     */
    public String detectRegion() {
        if (cachedRegion != null) {
            return cachedRegion;
        }
        
        // Strategy 1: Check environment variable
        Optional<String> envRegion = detectFromEnvironment();
        if (envRegion.isPresent()) {
            cachedRegion = envRegion.get();
            logger.info("Region detected from environment variable: {}", cachedRegion);
            return cachedRegion;
        }
        
        // Strategy 2: Check system property
        Optional<String> sysPropRegion = detectFromSystemProperty();
        if (sysPropRegion.isPresent()) {
            cachedRegion = sysPropRegion.get();
            logger.info("Region detected from system property: {}", cachedRegion);
            return cachedRegion;
        }
        
        // Strategy 3: Query EC2 metadata service
        Optional<String> metadataRegion = detectFromEC2Metadata();
        if (metadataRegion.isPresent()) {
            cachedRegion = metadataRegion.get();
            logger.info("Region detected from EC2 metadata: {}", cachedRegion);
            return cachedRegion;
        }
        
        // Strategy 4: Parse from availability zone
        Optional<String> azRegion = detectFromAvailabilityZone();
        if (azRegion.isPresent()) {
            cachedRegion = azRegion.get();
            logger.info("Region detected from availability zone: {}", cachedRegion);
            return cachedRegion;
        }
        
        // Fallback to default region
        cachedRegion = DEFAULT_REGION;
        logger.warn("Unable to detect region, using default: {}", cachedRegion);
        return cachedRegion;
    }
    
    /**
     * Checks if the current region is Taiwan (ap-northeast-1).
     * 
     * @return true if current region is Taiwan
     */
    public boolean isTaiwanRegion() {
        return "ap-northeast-1".equals(detectRegion());
    }
    
    /**
     * Checks if the current region is Japan (ap-northeast-2).
     * 
     * @return true if current region is Japan
     */
    public boolean isJapanRegion() {
        return "ap-northeast-2".equals(detectRegion());
    }
    
    /**
     * Clears the cached region, forcing re-detection on next call.
     * Useful for testing or dynamic region changes.
     */
    public void clearCache() {
        cachedRegion = null;
        logger.debug("Region cache cleared");
    }
    
    private Optional<String> detectFromEnvironment() {
        String region = System.getenv("AWS_REGION");
        if (region != null && !region.trim().isEmpty()) {
            return Optional.of(region.trim());
        }
        
        // Also check AWS_DEFAULT_REGION
        region = System.getenv("AWS_DEFAULT_REGION");
        if (region != null && !region.trim().isEmpty()) {
            return Optional.of(region.trim());
        }
        
        return Optional.empty();
    }
    
    private Optional<String> detectFromSystemProperty() {
        String region = System.getProperty("aws.region");
        if (region != null && !region.trim().isEmpty()) {
            return Optional.of(region.trim());
        }
        return Optional.empty();
    }
    
    private Optional<String> detectFromEC2Metadata() {
        try {
            URI uri = URI.create(EC2_METADATA_URL);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(METADATA_TIMEOUT_MS);
            connection.setReadTimeout(METADATA_TIMEOUT_MS);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String region = reader.readLine();
                    if (region != null && !region.trim().isEmpty()) {
                        return Optional.of(region.trim());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Unable to query EC2 metadata service: {}", e.getMessage());
        }
        return Optional.empty();
    }
    
    private Optional<String> detectFromAvailabilityZone() {
        try {
            URI uri = URI.create(EC2_AZ_METADATA_URL);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(METADATA_TIMEOUT_MS);
            connection.setReadTimeout(METADATA_TIMEOUT_MS);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String az = reader.readLine();
                    if (az != null && !az.trim().isEmpty()) {
                        // Extract region from AZ (e.g., "ap-northeast-1a" -> "ap-northeast-1")
                        String region = az.substring(0, az.length() - 1);
                        return Optional.of(region);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Unable to query EC2 AZ metadata: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
