package tests;
import model.TimeAllowance;
import java.time.*;

public class AllowanceTest {
    public static void main(String[] args) {
        TimeAllowance allowance=new TimeAllowance();
        if(allowance.getAllocated()!=100 || allowance.getUsed()!=0 || allowance.getReserved()!=0 || allowance.getAvailable()!=100) throw new AssertionError("Default allowance is incorrect.");
        allowance.reserve("J1",15);
        if(allowance.getReserved()!=15 || allowance.getAvailable()!=85) throw new AssertionError("Reservation is incorrect.");
        try {
            allowance.reserve("J1",15);
            throw new AssertionError("Duplicate reservation accepted.");
        }
        catch(IllegalStateException e) {
        }
        // cancels the job without charging 
        allowance.release("J1");
        if(allowance.getUsed()!=0 || allowance.getReserved()!=0 || allowance.getAvailable()!=100) throw new AssertionError("Queued cancellation is incorrect.");
        // cancels a runnign job after 7m
        allowance.reserve("J2",30);
        allowance.charge("J2",7);
        if(allowance.getUsed()!=7 || allowance.getReserved()!=0 || allowance.getAvailable()!=93) throw new AssertionError("Running cancellation charge is incorrect.");
        // full 15m job run
        allowance.reserve("J3",15);
        allowance.complete("J3");

        if(allowance.getUsed()!=22 || allowance.getReserved()!=0 || allowance.getAvailable()!=78) throw new AssertionError("Completion incorrect.");
        // granting extra minutes
        LocalDateTime time=LocalDateTime.of(2026,11,16,10,20);
        allowance.grantTime(20,"admin","Extra research time",time);

        if(allowance.getAllocated()!=120 || allowance.getAvailable()!=98) throw new AssertionError("Time grant incorrect.");
        if(allowance.getGrants().size()!=1) throw new AssertionError("Time grant has not been recorded.");
        if(allowance.getGrants().get(0).getMinutes()!=20) throw new AssertionError("Recorded grant minutes are incorrect.");
        if(!allowance.getGrants().get(0).getAdminUsername().equals("admin")) throw new AssertionError("Grant administrator incorrect.");
        if(!allowance.getGrants().get(0).getReason().equals("Extra research time")) throw new AssertionError("Grant reason incorrect.");
        if(!allowance.getGrants().get(0).getSimulatedTime().equals(time)) throw new AssertionError("Grant timestamp incorrect.");
        TimeAllowance exact=new TimeAllowance(15);
        exact.reserve("J4",15);
        if(exact.getAvailable()!=0 || exact.getReserved()!=15) throw new AssertionError("Exact allowance reservation incorrect.");

        // rejects a job if the user doesnt have enough minutes for the job
        try {
            exact.reserve("J5",5);
            throw new AssertionError("Insufficient allowance.");
        }
        catch(IllegalStateException e) {}
        exact.complete("J4");
        if(exact.getUsed()!=15 || exact.getReserved()!=0 || exact.getAvailable()!=0) throw new AssertionError("Completion at zero available minutes incorrect.");
        try {
            allowance.charge("J2",7);
            throw new AssertionError("Double charge.");
        }
        catch(IllegalStateException e) {}
        try {
            TimeAllowance invalid=new TimeAllowance(-1);
            throw new AssertionError("Negative initial allowance.");
        }
        catch(IllegalArgumentException e) {}
        try {
            allowance.reserve("J6",0);
            throw new AssertionError("Zero reservation.");
        }
        catch(IllegalArgumentException e) {}
        try {
            allowance.reserve("J7",-5);
            throw new AssertionError("Negative reservation.");
        }
        catch(IllegalArgumentException e) {}
        allowance.reserve("J8",15);

        try {
            allowance.charge("J8",-1);
            throw new AssertionError("Negative runtime.");
        }
        catch(IllegalArgumentException e) {}
        try {
            allowance.charge("J8",16);
            throw new AssertionError("Excessive runtime.");
        }
        catch(IllegalArgumentException e) {}
        if(allowance.getUsed()!=22 || allowance.getReserved()!=15 || allowance.getAvailable()!=83) throw new AssertionError("Invalid charge changed from balance.");
        allowance.release("J8");
        try {
            allowance.grantTime(0,"admin","Extra time",time);
            throw new AssertionError("Zero grant accepted.");
        }
        catch(IllegalArgumentException e) {}
        try {
            allowance.grantTime(-5,"admin","Extra research time",time);
            throw new AssertionError("Negative grant accepted.");
        }
        catch(IllegalArgumentException e) {}
        try {
            allowance.grantTime(10,"admin","",time);
            throw new AssertionError("Grant without a reason accepted.");
        }
        catch(IllegalArgumentException e) {}
        if(allowance.getAllocated()!=120 || allowance.getUsed()!=22 || allowance.getReserved()!=0 || allowance.getAvailable()!=98 || allowance.getGrants().size()!=1) throw new AssertionError("Invalid operations changed the allowance.");
        System.out.println("passed AllowanceTest");
    }
}
