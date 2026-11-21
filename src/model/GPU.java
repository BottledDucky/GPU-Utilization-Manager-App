package model;

public class GPU {
	private Integer gpuId; private Job currentJob;
	
	public GPU(Integer gpuId, Job currentJob) {
		this.gpuId = gpuId; this.currentJob = currentJob;
	}
	
	public boolean isFree() {
		return currentJob == null;
		
	}
	
	public void assign(Job job) {
		currentJob = job;
		
	}
	
	public void free() {
		currentJob = null;
		
	}

	public Integer getGpuId() {
		return gpuId;
	}

	public Job getCurrentJob() {
		return currentJob;
	}
	

}
