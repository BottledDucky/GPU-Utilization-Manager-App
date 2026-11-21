package model;
import java.time.LocalDateTime;
import java.util.*;

public final class TimeAllowance {
public static final int DEFAULT_MINUTES = 100;
private int allocated;
private int used;
private final Map<String, Integer> reservations =new LinkedHashMap<>();
private final Set<String> settledJobs =new LinkedHashSet<>();
private final List<TimeGrant> grants =new ArrayList<>();
   
    public TimeAllowance() {
        this(DEFAULT_MINUTES); }
    public TimeAllowance(int initialAllocated) {
        if (initialAllocated < 0) {
            throw new IllegalArgumentException("Allocated minutes cannot be negative."); }
        this.allocated = initialAllocated;
    }
    public synchronized int getAllocated() {
        return allocated;
    }
    public synchronized int getUsed() {
        return used;
    }
    public synchronized int getReserved() {
        int sum = 0;
        for (int minutes : reservations.values()) {
            sum += minutes;
        }
        return sum;
    }
    public synchronized int getAvailable() {
        return allocated - used - getReserved();
    }
    public synchronized int available() {
        return getAvailable();
    }
    public synchronized Map<String, Integer> getReservations() {
        return new LinkedHashMap<>(reservations);
    }
    public synchronized Set<String> getSettledJobIds() {
        return new LinkedHashSet<>(settledJobs);
    }
    public synchronized List<TimeGrant> getGrants() {
        return new ArrayList<>(grants);
    }
    public synchronized void reserve(String jobId, int duration) {
     requireId(jobId);
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive.");
        }
        if (reservations.containsKey(jobId) || settledJobs.contains(jobId)) {
            throw new IllegalStateException("Job already reserved or settled: " + jobId);
        }
        if (duration > getAvailable()) {
            throw new IllegalStateException("Insufficient available GPU minutes.");
        }
        reservations.put(jobId, duration);
    }
    public synchronized void release(String jobId) {
        reservationFor(jobId);
        reservations.remove(jobId);
        settledJobs.add(jobId);
    }
    public synchronized void complete(String jobId) {
        charge(jobId, reservationFor(jobId));
    }
    public synchronized int charge(String jobId, int elapsedGpuMinutes) {
        int duration = reservationFor(jobId);
        if (elapsedGpuMinutes < 0|| elapsedGpuMinutes > duration) {
            throw new IllegalArgumentException("Runtime must be between 0 and reserved duration.");
        }
        int newUsed = used + elapsedGpuMinutes;
        if (newUsed > allocated) {
            throw new IllegalStateException("Charge exceeds allocated GPU minutes.");
        }
        reservations.remove(jobId);
        used = newUsed;
        settledJobs.add(jobId);
        return elapsedGpuMinutes;
    }
    public synchronized TimeGrant grantTime(
            int minutes,
            String adminUsername,
            String reason,
            LocalDateTime when) {
        TimeGrant grant =new TimeGrant(minutes, adminUsername, reason, when);
        allocated += minutes;
        allocated = updated;
        grants.add(grant);
        return grant;
    }
    private static void requireId(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) {
        	throw new IllegalArgumentException("Job ID is required.");
        }
    }
    private int reservationFor(String jobId) {
        requireId(jobId);
        Integer duration = reservations.get(jobId);
        if (duration == null) {
            throw new IllegalStateException("Job has no active reservation (or already settled): "+ jobId);
        }
        return duration;
    }
    public static TimeAllowance restore(
            int allocated,
            int used,
            Map<String, Integer> reserved,
            Set<String> settled,
            List<TimeGrant> audit) {
        if (allocated < 0 || used < 0 || used > allocated) {
            throw new IllegalArgumentException("Invalid allocated/used balance.");
        }
        if (reserved == null|| settled == null|| audit == null) {
            throw new IllegalArgumentException("Missing allowance data.");
        }
        TimeAllowance allowance =new TimeAllowance(allocated);
        allowance.used = used;
        for (Map.Entry<String, Integer> entry: reserved.entrySet()) {
            requireId(entry.getKey());
            if (entry.getValue() == null|| entry.getValue() <= 0) {
                throw new IllegalArgumentException("Invalid reservation.");
            }
            if (allowance.reservations.putIfAbsent(entry.getKey(),entry.getValue()) != null) {
                throw new IllegalArgumentException("Duplicate job reservation.");
            }
        }
        for (String id : settled) {
            requireId(id);
            if (allowance.reservations.containsKey(id)) {
                throw new IllegalArgumentException("Settled job is reserved.");
            }
            allowance.settledJobs.add(id);
        }
        for (TimeGrant grant : audit) {
            allowance.grants.add(
                java.util.Objects.requireNonNull(grant));
        }
        if (allowance.getAvailable() < 0) {
            throw new IllegalArgumentException("Restored allowance is negative.");
        }
        return allowance;
    }
}
