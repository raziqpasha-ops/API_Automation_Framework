package com.booking.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * LoggerManager is the ONE place that creates Logger objects for the framework.
 *
 * Interview explanation: Log4j2's standard pattern is
 *     private static final Logger LOG = LogManager.getLogger(MyClass.class);
 * repeated in every class. That works, but it duplicates a line everywhere and
 * hardcodes the class name each time.
 *
 * This utility centralizes it: any class calls
 *     Logger log = LoggerManager.get(ThisClass.class);
 * and gets a correctly configured logger. One factory, consistent behaviour,
 * and if we ever switch logging libraries we change ONE file.
 *
 * The .getName() trick below uses the passed-in class to name the logger, so
 * every log line automatically shows WHICH class produced it — crucial when
 * reading a 500-line CI log at 2am.
 */
public class LoggerManager {

    private LoggerManager() { }

    /**
     * Returns a Log4j2 Logger named after the CALLING class.
     * Usage inside any framework class:
     *     private static final Logger log = LoggerManager.get(MyClass.class);
     */
    public static Logger get(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }
}
