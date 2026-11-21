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
		this.jobId = jobId; this.studentId = studentId; this.title = title;
		this.applicationName = applicationName; this.description = description;
		this.taskSize = taskSize; this.submissionTime = submissionTime;
		status = "QUEUED";
		
		initializeWorkloadUnits();
	}
	
	public void start(Integer gpuId, LocalDateTime time) {
		this.status = "RUNNING";
		this.assignedGpuID = gpuId;
		this.startTime = time;
	}
	
	public void complete(LocalDateTime time) {
		this.status = "COMPLETED";
		this.finishTime = time;
		
	}
	
	public void cancel(LocalDateTime time, String cancelledBy, String reason) {
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
		
		return Duration.between(startTime, end).toMinutes();
		
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
