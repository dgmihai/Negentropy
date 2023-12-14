package com.trajan.negentropy.util;

import java.time.LocalDateTime;

public interface CurrentTimeSettable {
    void manualTime(LocalDateTime time);
    LocalDateTime manualTime();

    default LocalDateTime now() {
        return manualTime() != null ? manualTime() : LocalDateTime.now();
    }
}