package com.trajan.negentropy.util;

import com.helger.commons.annotation.VisibleForTesting;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Getter
@Setter
@Service
public class ServerClockService {
    @VisibleForTesting
    private LocalDateTime manualTime = null;

    public LocalDateTime time() {
        return manualTime != null ? manualTime : LocalDateTime.now();
    }

    public static LocalDateTime now() {
        return SpringContext.getBean(ServerClockService.class).time();
    }
}