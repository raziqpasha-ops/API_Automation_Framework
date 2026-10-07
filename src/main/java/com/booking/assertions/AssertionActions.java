package com.booking.assertions;

// JAVA "package" keyword: declares this reusable assertion library lives in
// com.booking.assertions. Returns nothing — a compile-time folder declaration.

import org.testng.Assert;
// We IMPORT TestNG's Assert because our reusable keywords WRAP it — delegation,
// not reinvention. Each method below forwards to TestNG, which THROWS
// AssertionError on failure. Interview line: "we delegate to TestNG's proven
// engine but expose our own vocabulary, so if we ever switch runners
// (JUnit, etc.) only THIS file changes — zero test changes."

import org.apache.logging.log4j.Logger;
// Log4j2's Logger interface. RETURN TYPE of LoggerManager.get(...) below.
// Every assertion logs what it checked and its result, so a failed run can be
// diagnosed from logs/framework.log alone, without opening the IDE.

import com.booking.utils.LoggerManager;
// Our one-line logger factory. get(Class) RETURN TYPE: org.apache.logging.log4j.Logger.

/**
 * AssertionActions is the framework's REUSABLE assertion library.
 *
 * Interview explanation — why not call TestNG's Assert directly in tests?
 *
 *   1. SINGLE SOURCE OF TRUTH : right now tests say
 *          AssertionActions.assertEquals(actual, expected, "msg")
 *      If tomorrow we want EVERY assertion logged, or reported to Allure, or
 *      localized, we change THIS class once — not 200 call sites in tests.
 *
 *   2. READABLE KEYWORDS      : method names are chosen to read like English
 *      sentences inside tests: "assertThatStatusIs(200, response)". A manual
 *      QA reading the test understands it without knowing TestNG at all.
 *
 *   3. HIDDEN COMPLEXITY      : a keyword can do MORE than the raw call — for
 *      example assertThatStatusIs both extracts the status code AND asserts it,
 *      removing two lines of boilerplate from every test that checks a status.
 *
 *   4. DELEGATION PATTERN     : each method delegates to org.testng.Assert,
 *      so behaviour (hard assert, throws AssertionError, stops the test) is
 *      identical to stock TestNG — proven code, our vocabulary on top.
 *
 * All methods are STATIC (call as AssertionActions.methodName) and the class
 * has a private constructor because utility classes are never instantiated.
 */
public class AssertionActions {

    // Framework logger. RETURN TYPE: org.apache.logging.log4j.Logger.
    // Every log line from this class is tagged with its full class name, so
    // assertion activity is easy to grep in the rolling log file.
    private static final Logger log = LoggerManager.get(AssertionActions.class);

    // JAVA "private" constructor: nobody can create an object of this class.
    // Assertion utilities are used as AssertionActions.assertEquals(...) —
    // static style. Creating instances would add memory for no benefit.
    private AssertionActions() { }

    /**
     * Reusable keyword: assert two OBJECTS are equal (Strings, Integers, POJOs...).
     * DELEGATES TO: org.testng.Assert.assertEquals(Object, Object, String)
     *               RETURN TYPE: void — THROWS AssertionError when not equal.
     */
    public static void assertEquals(Object actual, Object expected, String message) {

        // Log BEFORE asserting: if this assertion fails, the log already shows
        // exactly which two values were compared — no guessing from the exception.
        log.info("ASSERT equals [{}] :: actual='{}', expected='{}'",
                message, actual, expected);

        // Delegation: TestNG does the real comparison (uses .equals() for
        // objects, so Integer 180 vs Integer 180 compares by VALUE, correctly).
        Assert.assertEquals(actual, expected, message);
    }

    /**
     * Reusable keyword: assert a condition is TRUE.
     * DELEGATES TO: org.testng.Assert.assertTrue(boolean, String)
     *               RETURN TYPE: void — THROWS AssertionError when condition is false.
     */
    public static void assertTrue(boolean condition, String message) {

        // Log the condition and its boolean value BEFORE asserting.
        log.info("ASSERT true [{}] :: condition={}", message, condition);

        // Delegation: TestNG throws AssertionError if condition is false.
        Assert.assertTrue(condition, message);
    }

    /**
     * Reusable keyword: assert a condition is FALSE.
     * DELEGATES TO: org.testng.Assert.assertFalse(boolean, String)
     *               RETURN TYPE: void — THROWS AssertionError when condition is true.
     */
    public static void assertFalse(boolean condition, String message) {

        // Log first — same evidence-first pattern as every other keyword here.
        log.info("ASSERT false [{}] :: condition={}", message, condition);

        // Delegation: TestNG throws AssertionError if condition turned out true.
        Assert.assertFalse(condition, message);
    }

    /**
     * Reusable keyword: assert an object is NOT null (token, POJO fields...).
     * DELEGATES TO: org.testng.Assert.assertNotNull(Object, String)
     *               RETURN TYPE: void — THROWS AssertionError when object is null.
     */
    public static void assertNotNull(Object object, String message) {

        // Log the check. Note we print only the PRESENCE, not the object itself,
        // so we never accidentally dump a token/password into log files.
        log.info("ASSERT notNull [{}] :: value present = {}", message, object != null);

        // Delegation: TestNG throws AssertionError if the object is null.
        Assert.assertNotNull(object, message);
    }

    // ---------------- COMPOSITE (higher-level) KEYWORDS ----------------
    // The methods below combine EXTRACT + ASSERT into one keyword. This is the
    // real power of a reusable assertion layer: each one removes boilerplate
    // that would otherwise repeat in every single API test.

    /**
     * Composite keyword: assert an io.restassured.response.Response has the
     * expected HTTP status code.
     * Wraps: response.getStatusCode()  RETURN TYPE: int
     * then : assertEquals(int, int)    RETURN TYPE: void (throws on mismatch).
     */
    public static void assertThatStatusIs(io.restassured.response.Response response,
                                          int expectedStatus, String message) {

        // getStatusCode() RETURN TYPE: int — RestAssured's raw HTTP status.
        int actualStatus = response.getStatusCode();

        // Reuse our own keyword (keyword building on keyword — layered reuse).
        assertEquals(actualStatus, expectedStatus, message);
    }

    /**
     * Composite keyword: assert a Response can be DESERIALIZED into the given
     * POJO class and is not null. Proves the response shape is intact.
     * Wraps: response.as(clazz) RETURN TYPE: whatever class is passed
     * then : assertNotNull      RETURN TYPE: void (throws if mapping produced null).
     *
     * JAVA GENERICS EXPLAINED — what "<T> T" means, piece by piece:
     *
     *   1. THE FIRST <T>  (before the return type) is the "generic type
     *      declaration". It tells the JAVA COMPILER: "this method uses a
     *      TYPE PLACEHOLDER called T, and the caller will decide what T is
     *      when they call the method." It must come BEFORE the return type —
     *      that is Java's fixed syntax order: <T> then the method signature.
     *
     *   2. THE SECOND T   (alone, as the return type) means: "this method
     *      returns an object of type T — the SAME type the caller chose."
     *      So the return type is not fixed in the code; it FOLLOWS the caller.
     *
     *   3. Class<T>       (the parameter type) means: "pass me the .class
     *      object of type T", e.g. Booking.class. Passing Booking.class tells
     *      Java T = Booking, which then makes the return type Booking too.
     *
     *   4. HOW IT WORKS AT A CALL SITE — the compiler INFERS T automatically:
     *          Booking b = AssertionActions.assertThatBodyMapsTo(
     *                  response, Booking.class, "msg");
     *      Java sees Booking.class  -> T becomes Booking
     *      -> the method is guaranteed to RETURN a Booking
     *      -> so it assigns straight into a Booking variable.
     *      NO CASTING NEEDED: without generics the caller would be forced to
     *      write "Booking b = (Booking) someObject" and risk a ClassCastException
     *      at RUNTIME. Generics move that risk to COMPILE time — the error is
     *      caught by the compiler before the code ever runs. That is the single
     *      biggest benefit of generics and a favourite interview question.
     *
     *   5. WHY THIS MATTERS FOR THE FRAMEWORK: this ONE generic method serves
     *      EVERY POJO we will ever have — Booking, AuthResponse, future models —
     *      with type safety each time. That is reusability without sacrificing
     *      safety: one keyword, infinite types, zero casts.
     */
    public static <T> T assertThatBodyMapsTo(io.restassured.response.Response response,
                                             Class<T> pojoClass, String message) {

        // Response.as(Class) RETURN TYPE: T — RestAssured + Jackson deserialization
        // of the JSON body into our POJO. Because the parameter was Class<T>,
        // RestAssured itself returns exactly T. If the body is HTML/garbage this
        // call itself throws, which IS the shape-validation failure we want.
        T pojo = response.as(pojoClass);

        // Guard that deserialization produced a real object, then hand it back
        // so the calling test can chain further field assertions on it.
        assertNotNull(pojo, message);

        // RETURN TYPE: T — the caller's chosen type comes back unchanged.
        // Booking.class in -> Booking object out. AuthResponse.class in ->
        // AuthResponse object out. One method, every model, fully type-safe.
        return pojo;
    }
}
