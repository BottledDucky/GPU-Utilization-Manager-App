package model;

import java.io.*;
import java.util.*;
import java.time.*;
public final class TimeGrant implements Serializable {

    private static final long serialVersionUID = 1L;
    private final int minutes;
    private final String adminUsername;
    private final String reason;
    private final LocalDateTime simulatedTime;

    public TimeGrant(int minutes,String adminUsername,String reason,LocalDateTime simulatedTime) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Grant minutes must be positive.");
        }
        if (adminUsername == null || adminUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("Admin username is required.");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason is required.");
        }
        this.adminUsername = adminUsername.trim();
        this.reason = reason.trim();
        
        if (simulatedTime == null) {
            throw new IllegalArgumentException("Simulated time is required.");
        }

        this.minutes = minutes;
        this.simulatedTime = simulatedTime;
    }
    public int getMinutes() {
        return minutes;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getSimulatedTime() {
        return simulatedTime;
    }
}
