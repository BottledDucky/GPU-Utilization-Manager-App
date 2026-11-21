package logic;

import java.time.*;

public class SimulationClock {
	private LocalDateTime currentTime, startTime;
	
	public SimulationClock(LocalDateTime time) {
		this.currentTime = time; 
		this.startTime = time;
	}
	
	public void Advance() {
		currentTime = currentTime.plusMinutes(1);
	}
	
	public long getElapsedTime() {
		return Duration.between(startTime, currentTime).toMinutes();
	}

	public LocalDateTime getCurrentTime() {
		return currentTime;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}
	
	

}
