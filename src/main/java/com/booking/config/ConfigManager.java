package com.booking.config;

// We keep this class inside a "config" package so that all configuration related
// code stays in one place. This package-level organization is what makes a framework
// clean and reusable instead of having files scattered everywhere.

import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigManager is the SINGLE point of access for all configuration values
 * like baseUrl, username, password.
 *
 * Interview explanation: This class uses two design ideas.
 * 1. Singleton-ish lazy loading  : we read the file only ONCE and reuse the values.
 * 2. System property override    : if Jenkins passes -DbaseUrl=xxx, that value wins
 *                                  over the value in the file. This is how the SAME
 *                                  framework runs against different environments
 *                                  without any code change.
 */
public class ConfigManager {

    // A Properties object is Java's built-in key-value store, perfect for reading
    // .properties files. We keep it private so no other class can change it directly.
    private static Properties properties = new Properties();

    // This static block runs ONCE when the class is first loaded into memory.
    // It loads config.properties from the resources folder and fills the properties.
    static {
        try (InputStream input = ConfigManager.class.getClassLoader()
                .getResourceAsStream("config.properties")) {

            // If the file is missing we fail fast with a clear message, because running
            // without configuration would give confusing errors later in the tests.
            if (input == null) {
                throw new RuntimeException("config.properties not found in resources!");
            }
            properties.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Could not load config.properties: " + e.getMessage(), e);
        }
    }

    // Private constructor: nobody should ever create an object of this class,
    // everything is static. This is the standard "utility class" pattern.
    private ConfigManager() { }

    // This is the main method everyone in the framework uses to read a config value.
    // Order of priority:  1) system property from command line (-Dkey=value)
    //                     2) value from config.properties file
    // So in Jenkins we can override the baseUrl without editing any file.
    public static String get(String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isEmpty()) {
            return systemValue;
        }
        return properties.getProperty(key);
    }

    // Small helper methods so callers can write ConfigManager.getBaseUrl() instead of
    // ConfigManager.get("baseUrl"). This reads better in tests and avoids typos in keys.
    public static String getBaseUrl() {
        return get("baseUrl");
    }

    public static String getUsername() {
        return get("username");
    }

    public static String getPassword() {
        return get("password");
    }
}
