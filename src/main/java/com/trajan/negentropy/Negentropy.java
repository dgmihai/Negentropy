package com.trajan.negentropy;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 */
@PWA(
        name = "Negentropy",
        shortName = "Negentropy",
        offlinePath="offline.html",
        offlineResources = { "./images/offline.png"}
)
@SpringBootApplication
@ComponentScan("com.trajan.negentropy.*")
@NpmPackage(value = "@fontsource/pt-sans-narrow", version = "5.0.4")
@Theme(value = "negentropy")
@EnableScheduling
@EnableAsync
public class Negentropy extends SpringBootServletInitializer implements AppShellConfigurator {
    public static void main(String[] args) {
        SpringApplication.run(Negentropy.class, args);
    }

}
