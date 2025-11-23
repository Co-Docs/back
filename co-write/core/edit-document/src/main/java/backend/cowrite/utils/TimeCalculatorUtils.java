package backend.cowrite.utils;

import java.time.Duration;
import java.time.LocalDateTime;

public class TimeCalculatorUtils {

    public static Duration calculateDurationToMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.plusDays(1).toLocalDate().atStartOfDay();
        return Duration.between(now, midnight);
    }
}