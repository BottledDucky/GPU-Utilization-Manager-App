package logic;

import java.util.ArrayList;

import model.Student;
import model.Administrator;
import model.AccessRequest;

public class AccountService {

    private ArrayList<Student> students;
    private ArrayList<AccessRequest> requests;
    private Administrator admin;

    public AccountService()
    {
        students = new ArrayList<Student>();
        requests = new ArrayList<AccessRequest>();

        admin = new Administrator("", hashPassword("gpu2026"));
    }

    
    public Student getStudent(String uniId)
    {
        for (int i = 0; i < students.size(); i++)
        {
            if (students.get(i).getUniId().equals(uniId))
            {
                return students.get(i);
            }
        }

        return null;
    }
    
    private AccessRequest findRequest(String uniId)
    {
        for (int i = 0; i < requests.size(); i++)
        {
            if (requests.get(i).getStudent().getUniId().equals(uniId))
            {
                return requests.get(i);
            }
        }

        return null;
    }
    
    public ArrayList<AccessRequest> getPendingRequests()
    {
        ArrayList<AccessRequest> pending = new ArrayList<AccessRequest>();

        for (int i = 0; i < requests.size(); i++)
        {
            if (requests.get(i).getStatus().equals("PENDING"))
            {
                pending.add(requests.get(i));
            }
        }

        return pending;
    }
    
    public ArrayList<Student> getStudents()
    {
        return new ArrayList<Student>(students);
    }
    
    public ArrayList<AccessRequest> getRequests()
    {
        return new ArrayList<AccessRequest>(requests);
    }
}