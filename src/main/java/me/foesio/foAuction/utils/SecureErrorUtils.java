package me.foesio.foAuction.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for secure error message handling to prevent information disclosure.
 * Provides generic error messages for external users while preserving detailed logs.
 */
public final class SecureErrorUtils {

    private static final String GENERIC_ERROR_MSG = "An error occurred. Please contact an administrator.";
    private static final String GENERIC_ECONOMY_ERROR = "Economy operation failed. Please try again later.";
    private static final String GENERIC_FILE_ERROR = "Data operation failed. Please contact an administrator.";
    private static final String GENERIC_PERMISSION_ERROR = "You do not have permission to perform this action.";

    private SecureErrorUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Sanitizes error messages for external display to prevent information disclosure
     * @param originalMessage The original error message
     * @param logger Logger to log the detailed error
     * @param context Context information for logging
     * @return Safe error message for external display
     */
    public static String sanitizeErrorMessage(String originalMessage, Logger logger, String context) {
         // Log the detailed error for administrators
         if (logger != null && context != null) {
             logger.log(Level.WARNING, ColorPalette.log("Error in " + context + ": " + originalMessage));
         }

        // Return generic message to prevent information disclosure
        return GENERIC_ERROR_MSG;
    }

    /**
     * Gets a safe economy error message
     * @param originalError The original economy error
     * @param logger Logger to log detailed error
     * @return Safe economy error message
     */
    public static String getSafeEconomyError(String originalError, Logger logger) {
         if (logger != null && originalError != null) {
             logger.log(Level.WARNING, ColorPalette.log("Economy error: " + originalError));
         }
        return GENERIC_ECONOMY_ERROR;
    }

    /**
     * Gets a safe file operation error message
     * @param originalError The original file error
     * @param logger Logger to log detailed error
     * @param context File operation context
     * @return Safe file error message
     */
    public static String getSafeFileError(String originalError, Logger logger, String context) {
         if (logger != null && originalError != null) {
             logger.log(Level.WARNING, ColorPalette.log("File error in " + context + ": " + originalError));
         }
        return GENERIC_FILE_ERROR;
    }

    /**
     * Gets a safe permission error message
     * @param originalError The original permission error
     * @param logger Logger to log detailed error
     * @return Safe permission error message
     */
    public static String getSafePermissionError(String originalError, Logger logger) {
         if (logger != null && originalError != null) {
             logger.log(Level.WARNING, ColorPalette.log("Permission error: " + originalError));
         }
        return GENERIC_PERMISSION_ERROR;
    }

    /**
     * Sanitizes exception messages for logging (removes potentially sensitive data)
     * @param exception The exception to sanitize
     * @return Sanitized exception message for logging
     */
    public static String sanitizeForLogging(Exception exception) {
        if (exception == null) {
            return "Null exception";
        }

        String message = exception.getMessage();
        if (message == null) {
            return exception.getClass().getSimpleName();
        }

        // Remove potential sensitive information (file paths, IPs, etc.)
        String sanitized = message
            .replaceAll("[a-zA-Z]:\\\\\\\\[^\\\\s]*", "[REDACTED_PATH]") // File paths
            .replaceAll("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b", "[REDACTED_IP]") // IP addresses
            .replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[REDACTED_EMAIL]"); // Email addresses

        // Limit length for logging
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 197) + "...";
        }

        return sanitized;
    }
}
