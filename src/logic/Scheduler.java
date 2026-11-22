package logic;

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

	
	//REVISE GETTERS FOR OBJECTS, THEY SHOULD PROBABLY NOT RETURN THE ACTUAL OBJECT BUT A COPY
	public ArrayList<Job> getQueue() {
		return queue;
	}

	public ArrayList<GPU> getGpus() {
		return gpus;
	}

	public SimulationClock getClock() {
		return clock;
	}

	public AccountService getAccountService() {
		return accountService;
	}

	public ArrayList<Job> getJobs() {
		return jobs;
	}

	public int getNextJobNumber() {
		return nextJobNumber;
	}
	
	

}
