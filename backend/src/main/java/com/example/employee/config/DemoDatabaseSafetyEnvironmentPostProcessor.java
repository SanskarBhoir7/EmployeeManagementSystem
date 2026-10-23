package com.example.employee.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rejects an unsafe datasource target before Spring creates a DataSource or Flyway connects.
 */
public final class DemoDatabaseSafetyEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String ORIGINAL_DATABASE = "employee_db";
    private static final String DEMO_DATABASE = "ems_pro_demo";
    private static final Pattern MYSQL_DATABASE = Pattern.compile("^jdbc:mysql://[^/]+/([^?;]+)");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseName = environment.getProperty("DB_NAME");
        String datasourceUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        if (datasourceUrl == null && databaseName != null && !databaseName.isBlank()) {
            datasourceUrl = environment.getProperty("spring.datasource.url");
        }
        String urlDatabase = databaseFromUrl(datasourceUrl);

        if (ORIGINAL_DATABASE.equalsIgnoreCase(databaseName) || ORIGINAL_DATABASE.equalsIgnoreCase(urlDatabase)) {
            throw new IllegalStateException("Refusing to start against the protected employee_db database.");
        }

        if (environment.acceptsProfiles(Profiles.of("demo"))) {
            String demoDatabase = environment.getProperty("app.demo.database-name", DEMO_DATABASE);
            if (databaseName == null || databaseName.isBlank()
                    || !demoDatabase.equalsIgnoreCase(databaseName)
                    || !demoDatabase.equalsIgnoreCase(urlDatabase)) {
                throw new IllegalStateException("The demo profile requires DB_NAME and datasource URL to target " + demoDatabase + ".");
            }
            requireEnvironmentValue(environment, "DB_USERNAME");
            requireEnvironmentValue(environment, "DB_PASSWORD");
            requireEnvironmentValue(environment, "JWT_SECRET");
        }
    }

    private static String databaseFromUrl(String datasourceUrl) {
        if (datasourceUrl == null) {
            return null;
        }
        Matcher matcher = MYSQL_DATABASE.matcher(datasourceUrl);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static void requireEnvironmentValue(ConfigurableEnvironment environment, String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("The demo profile requires the " + name + " environment variable.");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
