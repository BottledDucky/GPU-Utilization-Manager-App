package logic;
import java.time.*;
import java.time.format.*;

public final class ValidationService {

    private ValidationService() {
    }
    public static String universityId(String value) {

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("University ID is required.");
        }
        String id = value;
        if (id.length() > 9) {
            throw new IllegalArgumentException("University ID cannot exceed 9 digits.");
        }

        for (int i = 0; i < id.length(); i++) {
            if (id.charAt(i) < '0' || id.charAt(i) > '9') {
                throw new IllegalArgumentException("University ID must contain only numbers.");
            }
        }

        return id;
    }
    public static String email(String value) {

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }

        String email = value;
        int at = email.indexOf('@');
        int dot = email.lastIndexOf('.');

        if (at <= 0 || dot <= at + 1 || dot == email.length() - 1) {
            throw new IllegalArgumentException("Invalid email address.");
        }

        if (email.indexOf('@', at + 1) != -1 || email.contains(" ")|| email.contains("\t")) {
            throw new IllegalArgumentException("Invalid email address.");
        }
        if (!email.endsWith(".com") && !email.endsWith(".ae")&& !email.endsWith(".org")) {
            throw new IllegalArgumentException("Email must end with .com, .ae, or .org please contact the admin if you belive this is a mistake.");
        }
        return email;
    }
public static String phone(String value) {
    if (value == null || value.trim().isEmpty()) {
        throw new IllegalArgumentException("Phone number is required.");
    }
    String phone = value;
    if (phone.length() != 10) {
        throw new IllegalArgumentException("Phone number must be exactly 10 digits.");
    }
    if (!phone.startsWith("05")) {
        throw new IllegalArgumentException("Phone number must start with 05.");
    }
    for (int i = 0; i < phone.length(); i++) {
        if (phone.charAt(i) < '0' || phone.charAt(i) > '9') {
            throw new IllegalArgumentException("Phone number must contain only numbers.");
        }
    }
    return phone;
}
    public static int positiveMinutes(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Minutes are required.");
        }
        int minutes;
        try {
            minutes = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Minutes must be a whole number.");
        }
        if (minutes <= 0) {
            throw new IllegalArgumentException("Minutes must be positive.");
        }
        return minutes;
    }
    public static int taskDuration(String taskSize) {

        if (taskSize == null || taskSize.trim().isEmpty()) {
            throw new IllegalArgumentException("Task size is required.");
        }
        String size = taskSize.trim().toUpperCase().replace('_', ' ');
        switch (size) {
            case "SMALL":
                return 5;
            case "MEDIUM":
                return 15;
            case "LARGE":
                return 30;
            case "VERY LARGE":
                return 60;
            default:
                throw new IllegalArgumentException("Invalid task size.");
        }
    }
    public static void requireAdmin(boolean admin) {
        if (!admin) {
            throw new SecurityException("Administrator permission required.");
        }
    }
    public static void requireApproved(boolean approved) {
        if (!approved) {
            throw new SecurityException("Student account is not approved.");
        }
    }
    public static void requireOwnerOrAdmin(String userId, String studentId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }

        if (userId == null || studentId == null || !userId.equals(studentId)) {
            throw new SecurityException("You cannot access another student's jobs.");
        }
    }
    public static void requireJobSubmission(String title, String application,String description, String size, boolean approved,boolean clockInitialized, int available){
        requireApproved(approved);

        if (!clockInitialized) {
            throw new IllegalStateException("Simulation clock is not initialized.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Job title is required.");
        }
        if (application == null || application.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name is required.");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required.");
        }

        int duration = taskDuration(size);

        if (duration > available) {
            throw new IllegalStateException("Not enough available GPU minutes.");
        }
    }
    public static void requireCancellation(String status, String userId,String studentId, boolean isAdmin, String reason) {
        requireOwnerOrAdmin(userId, studentId, isAdmin);
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason is required.");
        }
        if (!"QUEUED".equalsIgnoreCase(status)&& !"RUNNING".equalsIgnoreCase(status)) {
            throw new IllegalStateException("This job cannot be cancelled.");
        }
    }
    public static LocalDateTime parseSimulatedTime(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Date and time are required.");
        }
        try {
            		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/mm/yyyy hh:mm");
            		return LocalDateTime.parse(input.trim(), dateFormat);

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date and time. Use dd/mm/yyyy hh:mm.");
        }
    }
    public static String formatSimulatedTime(LocalDateTime time) {
        if (time == null) {
            throw new IllegalArgumentException("Date and time are required.");
        }
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return time.format(formatter);
    }
}
