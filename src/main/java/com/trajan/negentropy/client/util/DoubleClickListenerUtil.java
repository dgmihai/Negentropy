package com.trajan.negentropy.client.util;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.shared.Registration;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class DoubleClickListenerUtil {
    private static boolean isIOS() {
        WebBrowser webBrowser = VaadinSession.getCurrent().getBrowser();
        return webBrowser.isIPhone();
    }

    public static <T> Registration add(Grid<T> grid, Consumer<T> onClick) {
        if (isIOS()) {
            final AtomicReference<LocalDateTime> firstClickTime = new AtomicReference<>();
            final AtomicBoolean waitingSecondClick = new AtomicBoolean(false);

            int millisecondInterval = 500;

            return grid.addItemClickListener(e -> {
                if (!waitingSecondClick.get()) {
                    firstClickTime.set(LocalDateTime.now());
                    waitingSecondClick.set(true);

                    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                    executor.schedule(() -> waitingSecondClick.set(false), millisecondInterval, TimeUnit.MILLISECONDS);

                    executor.shutdown();
                } else {
                    waitingSecondClick.set(false);

                    if (LocalDateTime.now().compareTo(firstClickTime.get()) < millisecondInterval) {
                        if (e.getItem() != null) {
                            onClick.accept(e.getItem());
                        }
                    }
                }
            });
        } else {
            return grid.addItemDoubleClickListener(e -> {
                if (e.getItem() != null) {
                    onClick.accept(e.getItem());
                }
            });
        }
    }
}
