package model;

import java.time.*;

public class Job {
	private String jobId, studentId, title, applicationName, description, 
	cancelledBy, cancellationReason;
	
	private String status; //QUEUED, RUNNING, COMPLETED, CANCELLED	
	private String taskSize; //SMALL, MEDIUM, LARGE, VERY LARGE
	private int workloadUnits; //5, 15, 30, 60
	
	private Integer assignedGpuID;
	private LocalDateTime submissionTime, startTime, finishTime, cancellationTime;
	
	public Job(String jobId, String studentId, String title, String applicationName, String description, String taskSize, LocalDateTime submissionTime) {
		if (jobId == null || jobId.trim().isEmpty() || studentId == null || studentId.trim().isEmpty() || submissionTime == null) {
			throw new IllegalArgumentException("Job ID, student ID and submission time are required.");
		}
		
		if (taskSize == null) {
			throw new IllegalArgumentException("Task size is required.");
		}
		
		this.jobId = jobId; this.studentId = studentId; this.title = title;
		this.applicationName = applicationName; this.description = description;
		this.taskSize = taskSize; this.submissionTime = submissionTime;
		status = "QUEUED";
		
		initializeWorkloadUnits();
	}
	
	public void start(Integer gpuId, LocalDateTime time) {
		if (!status.equals("QUEUED")) throw new IllegalStateException("Only queued jobs can start.");
		if (gpuId == null || gpuId < 1 || gpuId > 4 || time == null || time.isBefore(submissionTime)) {
			throw new IllegalArgumentException("Invalid GPU or job start time.");
		}
		
		this.status = "RUNNING";
		this.assignedGpuID = gpuId;
		this.startTime = time;
	}
	
	public void complete(LocalDateTime time) {
		this.status = "COMPLETED";
		this.finishTime = time;
		
	}
	
	public void cancel(LocalDateTime time, String cancelledBy, String reason) {
		if (!status.equals("QUEUED") && !status.equals("RUNNING")) {
			throw new IllegalStateException("Only queued or running jobs can be cancelled.");
		}
		if (time == null || time.isBefore(submissionTime) || (startTime != null && time.isBefore(startTime))) {
			throw new IllegalArgumentException("Invalid cancellation time.");
		}
		if (cancelledBy == null || cancelledBy.trim().isEmpty() || reason == null || reason.trim().isEmpty()) {
			throw new IllegalArgumentException("Cancellation user and reason are required.");
		}
		if (status.equalsIgnoreCase("RUNNING")) this.finishTime = time;
		this.status = "CANCELLED";
		cancellationTime = time;
		this.cancelledBy = cancelledBy;
		this.cancellationReason = reason;
		
	}

	
	
	private void initializeWorkloadUnits() {
		switch (taskSize.toUpperCase()) {
		case "SMALL": workloadUnits = 5; break;
		case "MEDIUM": workloadUnits = 15; break;
		case "LARGE": workloadUnits = 30; break;
		case "VERY LARGE": workloadUnits = 60; break;
		default: throw new IllegalArgumentException("Invalid task size.");
		}
	}
	
	public long getRemainingMinutes(LocalDateTime currentTime) {
		if (!(status.equalsIgnoreCase("Running"))) {
			return 0;
		}
		return workloadUnits - getElapsedMinutes(currentTime);
	}
	
	public long getElapsedMinutes(LocalDateTime currentTime) {
		if (startTime == null) {
			return 0;
		}
		
		LocalDateTime end = currentTime;
		
		if (finishTime != null) {
			end = finishTime;
		}
		
		if (end == null) {
			return 0;
		}
		
		long elapsed = Duration.between(startTime, end).toMinutes();
		if (elapsed < 0) return 0;
		if (elapsed > workloadUnits) return workloadUnits;
		return elapsed;
		
	}
	
	public LocalDateTime getExpectedFinishTime() {
		if (startTime == null) {
			return null;
		}
		
		return startTime.plusMinutes(workloadUnits);
	}
	
	public String getJobId() {
		return jobId;
	}

	public String getStudentId() {
		return studentId;
	}

	public String getTitle() {
		return title;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public String getDescription() {
		return description;
	}

	public String getCancelledBy() {
		return cancelledBy;
	}

	public String getCancellationReason() {
		return cancellationReason;
	}

	public String getStatus() {
		return status;
	}

	public String getTaskSize() {
		return taskSize;
	}

	public int getWorkloadUnits() {
		return workloadUnits;
	}

	public Integer getAssignedGpuID() {
		return assignedGpuID;
	}

	public LocalDateTime getSubmissionTime() {
		return submissionTime;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public LocalDateTime getFinishTime() {
		return finishTime;
	}

	public LocalDateTime getCancellationTime() {
		return cancellationTime;
	}
	
	public int getEstimatedDuration() {
		return workloadUnits;
	}
	

}
