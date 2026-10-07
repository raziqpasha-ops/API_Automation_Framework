package com.booking.listeners;

import com.booking.retry.RetryAnalyzer;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * RetryListener wires the RetryAnalyzer to EVERY test automatically.
 *
 * Interview explanation: There is a catch with IRetryAnalyzer — normally you
 * would have to write @Test(retryAnalyzer = RetryAnalyzer.class) on EVERY single
 * test method. Forgetting it on even one test means that test gets no retry
 * protection. Teams forget; silent coverage gaps appear.
 *
 * The IAnnotationTransformer interface solves this elegantly. TestNG calls its
 * transform() method once for EVERY @Test annotation it discovers, BEFORE the
 * run starts, and hands us the annotation object to MODIFY programmatically.
 * We use that hook to inject our RetryAnalyzer into every test globally.
 *
 * Result: retry protection is a FRAMEWORK feature. Test writers never think
 * about it, never import it, and can't forget it. That is exactly the kind of
 * "make the right thing the easy thing" design FAANG interviews look for.
 *
 * Wiring: this listener is registered once in testng.xml alongside TestListener.
 */
public class RetryListener implements IAnnotationTransformer {

    /**
     * Called by TestNG for every @Test found. We overwrite the annotation's
     * retryAnalyzer attribute with our own class — for every test, globally.
     */
    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {

        // This ONE line gives every test in the suite automatic rerun-on-failure.
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
