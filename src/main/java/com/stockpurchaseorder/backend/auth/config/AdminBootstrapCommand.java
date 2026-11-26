package com.stockpurchaseorder.backend.auth.config;

import com.stockpurchaseorder.backend.auth.service.AdminBootstrapService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

@Component
@Profile("bootstrap-admin")
public class AdminBootstrapCommand implements ApplicationRunner {
    private final AdminBootstrapService bootstrap;
    private final ConfigurableApplicationContext context;
    private final Environment environment;

    public AdminBootstrapCommand(AdminBootstrapService bootstrap, ConfigurableApplicationContext context,
                                 Environment environment) {
        this.bootstrap = bootstrap;
        this.context = context;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"none".equalsIgnoreCase(environment.getProperty("spring.main.web-application-type")))
            throw new IllegalStateException("Bootstrap must run in non-web mode");
        bootstrap.provision(System.getenv("STOCKFLOW_BOOTSTRAP_EMAIL"),
                System.getenv("STOCKFLOW_BOOTSTRAP_PASSWORD"));
        System.out.println("Initial administrator provisioned.");
        SpringApplication.exit(context);
    }
}
