package cn.edu.ncu.medical.registration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.registration.reservation")
public class AppointmentReservationProperties {
    private boolean enabled = true;
    private int processingTtlSeconds = 20;
    private int failedKeepSeconds = 600;
    private int scanPastDays = 1;
    private int scanFutureDays = 30;
    private int scanBatchSize = 100;
    private int consumerMaxRetries = 5;
}
