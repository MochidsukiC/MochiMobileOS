package jp.moyashi.phoneos.core.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * System clock service for the phone OS.
 * This class provides time and date functionality for the operating system.
 * Handles system time, formatting, and time-related operations.
 * 
 * @author YourName
 * @version 1.0
 */
public class SystemClock {
    
    /** Formatter for displaying time in HH:mm format */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    
    /** Formatter for displaying date in yyyy-MM-dd format */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /** Formatter for displaying full date and time */
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /** System start time for uptime calculations */
    private final LocalDateTime systemStartTime;
    
    /**
     * Constructs a new SystemClock instance.
     * Records the system start time for uptime tracking.
     */
    public SystemClock() {
        systemStartTime = LocalDateTime.now();
        System.out.println("SystemClock: System clock initialized at " + getFormattedDateTime());
    }
    
    /**
     * Gets the current system time.
     * 
     * @return Current LocalDateTime
     */
    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }
    
    /**
     * Gets the current time formatted as HH:mm.
     * 
     * @return Current time as formatted string
     */
    public String getFormattedTime() {
        return getCurrentTime().format(TIME_FORMAT);
    }
    
    /**
     * Gets the current date formatted as yyyy-MM-dd.
     * 
     * @return Current date as formatted string
     */
    public String getFormattedDate() {
        return getCurrentTime().format(DATE_FORMAT);
    }
    
    /**
     * Gets the current date and time formatted as yyyy-MM-dd HH:mm:ss.
     * 
     * @return Current date and time as formatted string
     */
    public String getFormattedDateTime() {
        return getCurrentTime().format(DATETIME_FORMAT);
    }
    
    /**
     * Gets the system start time.
     * 
     * @return The LocalDateTime when the system was started
     */
    public LocalDateTime getSystemStartTime() {
        return systemStartTime;
    }
    
    /**
     * Calculates the system uptime in milliseconds.
     * 
     * @return System uptime in milliseconds
     */
    public long getUptimeMillis() {
        return java.time.Duration.between(systemStartTime, getCurrentTime()).toMillis();
    }
    
    /**
     * Gets a formatted uptime string in hours:minutes:seconds format.
     * 
     * @return Formatted uptime string
     */
    public String getFormattedUptime() {
        long uptimeMs = getUptimeMillis();
        long hours = uptimeMs / (1000 * 60 * 60);
        long minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (uptimeMs % (1000 * 60)) / 1000;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    /**
     * Checks if the current time is within business hours (9 AM - 5 PM).
     * 
     * @return true if current time is within business hours, false otherwise
     */
    public boolean isBusinessHours() {
        int currentHour = getCurrentTime().getHour();
        return currentHour >= 9 && currentHour < 17;
    }
}