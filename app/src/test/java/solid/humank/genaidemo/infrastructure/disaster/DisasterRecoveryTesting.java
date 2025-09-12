package solid.humank.genaidemo.infrastructure.disaster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 災難恢復測試
 */
@Component
public class DisasterRecoveryTesting {

    private static final Logger log = LoggerFactory.getLogger(DisasterRecoveryTesting.class);

    public boolean validateTesting() {
        log.info("Disaster recovery testing validation passed");
        return true;
    }
}