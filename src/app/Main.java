package app;

import java.io.*;
import java.util.*;

import javax.swing.*;

import logic.AccountService;
import logic.Scheduler;
import logic.SimulationClock;

import model.AccessRequest;
import model.GPU;
import model.Job;
import model.Student;

import storage.DataStore;
import storage.SavedState;

import ui.LoginFrame;

public class Main {

    private static AccountService accountService;
    private static Scheduler scheduler;
    private static DataStore store;

    public static void main(String[] args)
    {
        start();
    }

    private static void start()
    {
        store = new DataStore(new File("data/gpu-state.txt"));
        accountService = new AccountService();

        try
        {
            SavedState saved = store.load().orElse(null);
            SimulationClock clock = new SimulationClock(null);

            if (saved != null)
            {
                ArrayList<Student> students = saved.restoreStudentList();
                ArrayList<AccessRequest> requests = saved.restoreRequests(students);

                accountService.restoreData(students, requests);

                if (saved.getClockNow() != null)
                {
                    clock = saved.restoreClock();
                }
            }

            scheduler = new Scheduler(accountService, clock);

            if (saved != null)
            {
                scheduler.restoreData(new ArrayList<Job>(saved.restoreJobs()), new ArrayList<String>(saved.getQueueJobIds()), saved.getNextJobNumber());
            }
            
            new LoginFrame(accountService, scheduler);
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(null, "The application could not start.\n" + e.getMessage() + "\nAny existing save file has been kept.", "Startup error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void saveData()
    {
        SavedState state = new SavedState();
        state.setClock(scheduler.getClock());

        ArrayList<Student> students = accountService.getStudents();
        ArrayList<AccessRequest> requests = accountService.getRequests();
        ArrayList<Job> jobs = scheduler.getJobs();
        ArrayList<Job> queue = scheduler.getQueue();
        ArrayList<GPU> gpus = scheduler.getGpus();

        for (int i = 0; i < students.size(); i++)
        {
            state.addStudent(students.get(i));
        }

        for (int i = 0; i < requests.size(); i++)
        {
            state.addRequest(requests.get(i));
        }

        for (int i = 0; i < jobs.size(); i++)
        {
            state.addJob(jobs.get(i));
        }

        for (int i = 0; i < queue.size(); i++)
        {
            state.enqueue(queue.get(i).getJobId());
        }

        for (int i = 0; i < gpus.size(); i++)
        {
            if (!gpus.get(i).isFree())
            {
                state.assignGpu(gpus.get(i).getGpuId(), gpus.get(i).getCurrentJob().getJobId());
            }
        }

        state.setNextJobNumber(scheduler.getNextJobNumber());

        try
        {
            store.save(state);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not save data: " + e.getMessage(), e);
        }
    }

}