package logic;

import java.time.LocalDateTime;
import java.util.*;

import model.GPU;
import model.Job;
import model.Student;

public class Scheduler {
	private ArrayList<Job> queue;
	private ArrayList<GPU> gpus;
	private SimulationClock clock;
	private AccountService accountService;
	private ArrayList<Job> jobs;
	private int nextJobNumber = 101; //this should be saved between sessions somehow
	
	public Scheduler(AccountService accountService, SimulationClock clock) {
		this.clock = clock; this.accountService = accountService;
		
		gpus = new ArrayList<>(); jobs = new ArrayList<>(); queue = new ArrayList<>();
		
		for (int i = 1; i<=4; i++) {
			gpus.add(new GPU(i, null));
		}
	}
	
	public void assignWaitingJobs() {
		for (GPU g: gpus) {
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
	
	private void completeJob() {
		
	}
	
	public void cancelJob(String jobId, String cancelBy, String reason) {
		
	}

	
	public void advanceMinute() {
		
	}
	
	public void findJob(String jobId) {
		
	}
	
	
	
	//REVISE GETTERS FOR OBJECTS, THEY SHOULD PROBABLY NOT RETURN THE ACTUAL OBJECT BUT A COPY
	public ArrayList<Job> getQueue() {
		return new ArrayList<Job>(queue);
	}

	public ArrayList<GPU> getGpus() {
		return new ArrayList<Job>(gpus);
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
