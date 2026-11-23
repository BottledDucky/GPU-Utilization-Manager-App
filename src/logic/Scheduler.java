package logic;

import java.time.LocalDateTime;
import java.util.*;

import model.GPU;
import model.Job;
import model.Student;
import model.User;

public class Scheduler {
	private ArrayList<Job> queue;
	private ArrayList<GPU> gpus;
	private SimulationClock clock;
	private AccountService accountService;
	private ArrayList<Job> jobs;
	private int nextJobNumber = 101; //this should be saved between sessions somehow
	
	public Scheduler(AccountService accountService, SimulationClock clock) {
		if (accountService == null || clock == null) {
			throw new IllegalArgumentException("Accounts and simulation clock are required.");
		}
		
		this.clock = clock; this.accountService = accountService;
		
		gpus = new ArrayList<>(); jobs = new ArrayList<>(); queue = new ArrayList<>();
		
		for (int i = 1; i<=4; i++) {
			gpus.add(new GPU(i, null));
		}
	}
	
	private void assignWaitingJobs() {
		for (GPU g: gpus) {
			if (queue.isEmpty()) break;
			
			if (g.isFree()) {
				Job job = queue.remove(0);
				job.start(g.getGpuId(), clock.getCurrentTime());
				g.assign(job);
			}
		}
		
	}
	
	public Job submitJob(String studentId, String title, String applicationName, String description, String taskSize) {
		Student student = accountService.getStudent(studentId);
		
		if (student == null) {
			throw new IllegalArgumentException("Student not found.");
		}
		
		if (!accountService.isApproved(student.getUniId()) || student.getTimeAllowance() == null) {
			throw new IllegalStateException("Student account is not approved.");
		}
		
		ValidationService.requireJobSubmission(title, applicationName, description, taskSize, true, clock.isInitialized(), student.getTimeAllowance().getAvailable());
		
		String jobId = "J" + (nextJobNumber);
		
		Job job = new Job(jobId, studentId, title, applicationName, description, taskSize, clock.getCurrentTime());
		
		int duration = job.getEstimatedDuration();
		
		if (duration > student.getTimeAllowance().getAvailable()) {
			throw new IllegalArgumentException("Student doesnt have enough time allowance.");
		}
		
		student.getTimeAllowance().reserve(jobId, duration);
		
		
		
		queue.add(job);
		jobs.add(job);
		nextJobNumber++;
		assignWaitingJobs();
		return job;
		
	}
	
	public void updateRunningJobs() {
		for (GPU g: gpus) {
			if (g.isFree()) continue;
			Job job = g.getCurrentJob();
			
			if (job.getRemainingMinutes(clock.getCurrentTime()) == 0 && job.getStatus().equals("RUNNING")) {
				Student student = accountService.getStudent(job.getStudentId());
				student.getTimeAllowance().charge(job.getJobId(), (int) job.getWorkloadUnits());
				
				job.complete(clock.getCurrentTime());
				g.free();
			
			}
		}
		
		assignWaitingJobs();
		
		
	}
	
	public void cancelJob(String jobId, User cancelBy, String reason) {
		Job job = findJob(jobId);
		if (job == null) {
			throw new IllegalArgumentException("Job does not exist");
		}
		
		if (cancelBy == null) {
			throw new IllegalArgumentException("User does not exist.");
		}
		
		if (job.getStatus().equals("COMPLETED") || job.getStatus().equals("CANCELLED")) {
			throw new IllegalArgumentException("Job cannot be cancelled, it is already completed/cancelled.");
		}
		
		if (reason == null || reason.trim().isEmpty()) {
			throw new IllegalArgumentException("Cancellation reason is required.");
		}
		
		
		Student student = accountService.getStudent(job.getStudentId());
		if (student == null || student.getTimeAllowance() == null) {
			throw new IllegalStateException("Job owner has no active allowance.");
		}
		
		if (cancelBy instanceof Student) {
			Student cancellingStudent = (Student) cancelBy;
			if (!(cancellingStudent.getUniId().equals(job.getStudentId()))) {
				throw new IllegalArgumentException("Students can cancel their own jobs only.");
			}
			if (!accountService.isApproved(cancellingStudent.getUniId())) {
				throw new IllegalStateException("Student account is not approved.");
			}
		}
		
		if (job.getStatus().equals("QUEUED")) {
			queue.remove(job);
			student.getTimeAllowance().charge(jobId, 0);
			
		
			
		}
		
		else if (job.getStatus().equals("RUNNING")) {
			GPU gpu = findGpu(job.getAssignedGpuID());
			if (gpu == null) throw new IllegalStateException("Job is assigned to unidentified GPU.");
			
			student.getTimeAllowance().charge(jobId, (int) job.getElapsedMinutes(clock.getCurrentTime()));
			gpu.free();
			
			
			
		}
		
		job.cancel(clock.getCurrentTime(), cancelBy.getUsername(), reason);
		assignWaitingJobs();
		
	}

	
	public void advanceMinute() {
		clock.Advance();
		updateRunningJobs();	
	}
	
	public Job findJob(String jobId) {
		for (Job j: jobs) {
			if (j.getJobId().equals(jobId)) return j;
		}
		return null;
		
	}
	
	public GPU findGpu(int gpuId) {
		for (GPU g: gpus) {
			if (g.getGpuId() == gpuId) return g;
		}
		return null;
	}
	
	public void restoreData(ArrayList<Job> savedJobs, ArrayList<String> queueIds, int nextNumber)
	{
	    jobs.clear();
	    queue.clear();

	    for (int i = 0; i < gpus.size(); i++)
	    {
	        gpus.get(i).free();
	    }

	    for (int i = 0; i < savedJobs.size(); i++)
	    {
	        jobs.add(savedJobs.get(i));
	    }

	    for (int i = 0; i < queueIds.size(); i++)
	    {
	        Job job = findJob(queueIds.get(i));

	        if (job != null && job.getStatus().equals("QUEUED"))
	        {
	            queue.add(job);
	        }
	    }

	    for (int i = 0; i < jobs.size(); i++)
	    {
	        Job job = jobs.get(i);

	        if (job.getStatus().equals("RUNNING"))
	        {
	            GPU gpu = findGpu(job.getAssignedGpuID());

	            if (gpu != null)
	            {
	                gpu.assign(job);
	            }
	        }
	    }

	    nextJobNumber = nextNumber;
	}
	
	//REVISE GETTERS FOR OBJECTS, THEY SHOULD PROBABLY NOT RETURN THE ACTUAL OBJECT BUT A COPY
	public ArrayList<Job> getQueue() {
		return new ArrayList<Job>(queue);
	}

	public ArrayList<GPU> getGpus() {
		return new ArrayList<GPU>(gpus);
	}

	public SimulationClock getClock() {
		return clock;
	}

	public AccountService getAccountService() {
		return accountService;
	}

	public ArrayList<Job> getJobs() {
		return new ArrayList<Job>(jobs);
	}

	public int getNextJobNumber() {
		return nextJobNumber;
	}
	
	

}
