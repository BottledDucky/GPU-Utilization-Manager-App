package model;

public class GPU {
	private Integer gpuId; private Job currentJob;
	
	public GPU(Integer gpuId, Job currentJob) {
		this.gpuId = gpuId; this.currentJob = currentJob;
	}
	
	public boolean isFree() {
		
	}
	
	public void assign(Job job) {
		
	}
	
	public void free() {
		
	}

	public Integer getGpuId() {
		return gpuId;
	}

	public Job getCurrentJob() {
		return currentJob;
	}
	

}
