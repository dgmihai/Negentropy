package com.trajan.negentropy.client.components.wellness;

import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.util.ServerClockService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@SpringComponent
@UIScope
public class WellnessInputScheduler {
    private final UILogger log = new UILogger();

    @Autowired private ServerClockService clock;
    @Autowired private WellnessDialog dialog;
    @Autowired private UI ui;

    private static final CronExpression cron = CronExpression.parse("0 0 5-23/4 * * ?");

    @PostConstruct
    public void init() {
        CompletableFuture.delayedExecutor(1, TimeUnit.HOURS)
                .execute(this::createDialog);
    }

    public void scheduleTask() {
        if ((dialog == null || !dialog.isOpened())) {
            long secondsUntilNextCron = Duration.between(clock.time(), cron.next(clock.time().plusHours(4))).toSeconds();
            CompletableFuture.delayedExecutor(secondsUntilNextCron, TimeUnit.SECONDS)
                    .execute(this::createDialog);
        }
    }

    private void createDialog() {
        try {
            ui.access(() -> {
                log.debug("Creating scheduled wellness dialog");
                dialog.addDialogCloseActionListener(e -> {
                    dialog.close();
                    scheduleTask();
                });
                dialog.open();
            });
        } catch (UIDetachedException e) {
            log.error("UI detached, cannot create scheduled dialog");
            scheduleTask();
        }
    }
}
