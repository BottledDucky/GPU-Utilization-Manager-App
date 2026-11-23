package logic;

import java.time.*;

public class SimulationClock {
	private LocalDateTime currentTime, startTime;
	
	public SimulationClock(LocalDateTime time) {
		this.currentTime = time; 
		this.startTime = time;
	}

	public void initialize(LocalDateTime time) {
		if (time == null) {
			throw new IllegalArgumentException("Choose a simulation date and time.");
		}
		if (isInitialized()) {
			throw new IllegalStateException("Simulation clock is already initialized.");
		}
		currentTime = time;
		startTime = time;
	}

	public boolean isInitialized() {
		return startTime != null && currentTime != null;
	}
	
	public void initialize(LocalDateTime time) {
		if (time == null) {
			throw new IllegalArgumentException("Choose a simulation date and time.");
		}
		if (isInitialized()) {
			throw new IllegalStateException("Simulation clock is already initialized.");
		}
		currentTime = time;
		startTime = time;
	}
	
	public boolean isInitialized() {
		return startTime != null && currentTime != null;
	}
	
	public void Advance() {
		if (!isInitialized()) {
			throw new IllegalStateException("Simulation clock is not initialized.");
		}
		currentTime = currentTime.plusMinutes(1);
	}
	
	public long getElapsedTime() {
		if (!isInitialized()) {
			return 0;
		}
		return Duration.between(startTime, currentTime).toMinutes();
	}

	public LocalDateTime getCurrentTime() {
		return currentTime;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}
	
	

}
