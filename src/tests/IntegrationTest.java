package tests;
import logic.ValidationService;
import model.*;
import storage.*;
import java.io.*;
import java.time.*;
import java.util.*;

public class IntegrationTest {
    public static void main(String[] args) throws IOException {
        LocalDateTime start=LocalDateTime.of(2026,11,16,10,0);
        ValidationService.universityId("100067158");
        ValidationService.email("100067158@ku.ac.ae");
        ValidationService.phone("0501234567");
        if(ValidationService.taskDuration("MEDIUM")!=15) throw new AssertionError("A medium job should take 15 minutes.");
        TimeAllowance allowance=new TimeAllowance();
        Student student=new Student("100067158","Abdulsalam","100067158@ku.ac.ae","hassedPassword","0501234567",allowance);
        ValidationService.requireJobSubmission("Imputing Data","DSAI","fixing missing values in the dataset","MEDIUM",true,true,allowance.getAvailable());
        Job job=new Job("J101",student.getUniId(),"Imputing Data","DSAI","fixing missing values in the dataset","MEDIUM",start);
        allowance.reserve(job.getJobId(),15);
        job.start(1,start);
        LocalDateTime cancelTime=start.plusMinutes(7);
        ValidationService.requireCancellation(job.getStatus(),student.getUniId(),job.getStudentId(),false,"Selected the wrong dataset");
        allowance.charge(job.getJobId(),(int)job.getElapsedMinutes(cancelTime));
        job.cancel(cancelTime,student.getUniId(),"Selected the wrong dataset");
        if(allowance.getUsed()!=7 || allowance.getReserved()!=0 || allowance.getAvailable()!=93) throw new AssertionError("Cancelling after 7 minutes should leave 93 minutes available.");
        ValidationService.requireJobSubmission("Evaluating a Classifier","Scikit-learn","Testing a classifier on the validation dataset","SMALL",true,true,allowance.getAvailable());
        Job second=new Job("J102",student.getUniId(),"Evaluating a Classifier","Scikit-learn","Testing a classifier on the validation dataset","SMALL",cancelTime);
        allowance.reserve(second.getJobId(),5);
        second.start(1,cancelTime);
        LocalDateTime finishTime=cancelTime.plusMinutes(5);
        second.complete(finishTime);
        allowance.complete(second.getJobId());
        if(allowance.getUsed()!=12 || allowance.getReserved()!=0 || allowance.getAvailable()!=88) throw new AssertionError("Both jobs should use 12 minutes and leave 88 available.");
        SavedState state=new SavedState();
        state.setClock(start,finishTime);
        state.addStudent(student);
        state.addJob(job);
        state.addJob(second);
        state.setNextJobNumber(103);
        File file=File.createTempFile("integration-",".txt");
        try {
            DataStore store=new DataStore(file);
            store.save(state);
            DataStore reopened=new DataStore(file);
            Optional<SavedState> result=reopened.load();
            if(!result.isPresent()) throw new AssertionError("The saved data could not be found.");
            SavedState loaded=result.get();
            Student restored=loaded.restoreStudents().get("100067158");
            if(restored==null) throw new AssertionError("The student was not loaded.");
            TimeAllowance restoredAllowance=restored.getTimeAllowance();
            if(restoredAllowance==null) throw new AssertionError("The student's allowance was not loaded.");
            if(restoredAllowance.getAllocated()!=100 || restoredAllowance.getUsed()!=12 || restoredAllowance.getReserved()!=0 || restoredAllowance.getAvailable()!=88) throw new AssertionError("The allowance changed after loading.");
            List<Job> jobs=loaded.restoreJobs();
            if(jobs.size()!=2) throw new AssertionError("There should be two saved jobs.");
            Job cancelled=jobs.get(0);
            Job completed=jobs.get(1);
            if(!cancelled.getJobId().equals("J101") || !cancelled.getStatus().equals("CANCELLED")) throw new AssertionError("J101 should still be cancelled after loading.");
            if(!cancelled.getCancellationTime().equals(cancelTime)) throw new AssertionError("The cancellation time changed after loading.");
            if(!cancelled.getCancelledBy().equals(student.getUniId())) throw new AssertionError("The wrong user was saved for the cancellation.");
            if(!cancelled.getCancellationReason().equals("Selected the wrong dataset")) throw new AssertionError("The cancellation reason changed after loading.");
            if(!completed.getJobId().equals("J102") || !completed.getStatus().equals("COMPLETED")) throw new AssertionError("J102 should still be completed after loading.");
            if(!completed.getFinishTime().equals(finishTime)) throw new AssertionError("The finish time changed after loading.");
            if(!loaded.getClockStart().equals(start) || !loaded.getClockNow().equals(finishTime)) throw new AssertionError("The simulation clock changed after loading.");
            if(loaded.getNextJobNumber()!=103) throw new AssertionError("The next job number should be 103.");
            if(!loaded.getQueueJobIds().isEmpty() || !loaded.getGpuAssignments().isEmpty()) throw new AssertionError("Finished jobs should not be waiting or using a GPU.");
            try {
                restoredAllowance.charge("J101",7);
                throw new AssertionError("J101 was charged a second time after loading.");
            }
            catch(IllegalStateException e) {
            }
            if(restoredAllowance.getUsed()!=12 || restoredAllowance.getAvailable()!=88) throw new AssertionError("Rejecting the second charge should not change the balance.");
            System.out.println("passed IntegrationTest");
        }
        finally {
            if(file.exists() && !file.delete()) throw new IOException("Could not delete the test file.");
        }
    }
}
