package com.trajan.negentropy.util;

import com.helger.commons.annotation.VisibleForTesting;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Getter
@Setter
@Service
@Slf4j
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