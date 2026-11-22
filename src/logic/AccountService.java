package logic;

import java.util.ArrayList;

import model.Student;
import model.Administrator;
import model.AccessRequest;
import model.TimeAllowance;

public class AccountService {

    private ArrayList<Student> students;
    private ArrayList<AccessRequest> requests;
    private Administrator admin;

    public AccountService()
    {
        students = new ArrayList<Student>();
        requests = new ArrayList<AccessRequest>();
        admin = new Administrator("", "gpu2026");
    }

    public Student getStudent(String uniId)
    {
        if (uniId == null)
        {
            return null;
        }

        uniId = uniId.trim();

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
        if (uniId == null)
        {
            return null;
        }

        uniId = uniId.trim();

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

    public ArrayList<Student> getApprovedStudents()
    {
        ArrayList<Student> approved = new ArrayList<Student>();

        for (int i = 0; i < requests.size(); i++)
        {
            if (requests.get(i).getStatus().equals("APPROVED"))
            {
                approved.add(requests.get(i).getStudent());
            }
        }

        return approved;
    }

    public AccessRequest registerStudent(String uniId, String name, String email, String phoneNum, String password)
    {
        if (uniId == null || uniId.trim().isEmpty() || name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty() || phoneNum == null || phoneNum.trim().isEmpty() || password == null || password.trim().isEmpty())
        {
            throw new IllegalArgumentException("All fields are required.");
        }

        uniId = ValidationService.universityId(uniId);
        name = name.trim();
        email = ValidationService.email(email);
        phoneNum = ValidationService.phone(phoneNum);

        if (getStudent(uniId) != null)
        {
            throw new IllegalArgumentException("University ID already registered.");
        }

        Student student = new Student(uniId, name, email, password, phoneNum, null);

        AccessRequest request = new AccessRequest(student);

        students.add(student);
        requests.add(request);

        return request;
    }

    public Student loginStudent(String uniId, String password)
    {
        Student student = getStudent(uniId);

        if (student == null)
        {
            throw new IllegalArgumentException("Student not found.");
        }

        if (!student.checkPassword(password))
        {
            throw new IllegalArgumentException("Incorrect password.");
        }

        AccessRequest request = findRequest(uniId);

        if (request == null)
        {
            throw new IllegalStateException("Registration request not found.");
        }

        if (request.getStatus().equals("PENDING"))
        {
            throw new IllegalStateException("Account is pending approval.");
        }

        if (request.getStatus().equals("REJECTED"))
        {
            throw new IllegalStateException("Registration was rejected.");
        }

        if (!request.getStatus().equals("APPROVED"))
        {
            throw new IllegalStateException("Account is not approved.");
        }

        if (student.getTimeAllowance() == null)
        {
            throw new IllegalStateException("Approved account has no GPU allowance.");
        }

        return student;
    }

    public Administrator loginAdmin(String username, String password)
    {
        if (admin.getUsername().equals(username) && admin.checkPassword(password))
        {
            return admin;
        }

        throw new IllegalArgumentException("Incorrect administrator credentials.");
    }

    public void approveRequest(String uniId, int initialMinutes)
    {
        AccessRequest request = findRequest(uniId);

        if (request == null)
        {
            throw new IllegalArgumentException("Registration request not found.");
        }

        if (!request.getStatus().equals("PENDING"))
        {
            throw new IllegalStateException("Request already reviewed.");
        }

        if (initialMinutes <= 0)
        {
            throw new IllegalArgumentException("Initial minutes must be positive.");
        }

        Student student = request.getStudent();

        if (student.getTimeAllowance() != null)
        {
            throw new IllegalStateException("Student already has an allowance.");
        }

        TimeAllowance allowance = new TimeAllowance(initialMinutes);

        student.setTimeAllowance(allowance);

        request.approve();
    }

    public void rejectRequest(String uniId)
    {
        AccessRequest request = findRequest(uniId);

        if (request == null)
        {
            throw new IllegalArgumentException("Registration request not found.");
        }

        request.reject();
    }

    public boolean isApproved(String uniId)
    {
        AccessRequest request = findRequest(uniId);
        return request != null && request.getStatus().equals("APPROVED");
    }

    public void restoreData(ArrayList<Student> savedStudents, ArrayList<AccessRequest> savedRequests)
    {
        if (savedStudents == null || savedRequests == null || savedStudents.size() != savedRequests.size())
        {
            throw new IllegalArgumentException("Incomplete saved account data.");
        }

        for (int i = 0; i < savedStudents.size(); i++)
        {
            Student student = savedStudents.get(i);

            if (student == null || student.getUniId() == null || student.getUniId().trim().isEmpty())
            {
                throw new IllegalArgumentException("Invalid saved student.");
            }

            for (int j = 0; j < i; j++)
            {
                if (savedStudents.get(j).getUniId().equals(student.getUniId()))
                {
                    throw new IllegalArgumentException("Duplicate saved university ID.");
                }
            }

            int matches = 0;

            for (int j = 0; j < savedRequests.size(); j++)
            {
                AccessRequest request = savedRequests.get(j);

                if (request == null || request.getStudent() == null)
                {
                    throw new IllegalArgumentException("Invalid saved registration request.");
                }

                if (request.getStudent() == student)
                {
                    matches++;
                    String status = request.getStatus();

                    if (!"PENDING".equals(status) && !"APPROVED".equals(status) && !"REJECTED".equals(status))
                    {
                        throw new IllegalArgumentException("Invalid saved request status.");
                    }

                    if ("APPROVED".equals(status) != (student.getTimeAllowance() != null))
                    {
                        throw new IllegalArgumentException("Saved approval and allowance do not match.");
                    }
                }
            }

            if (matches != 1)
            {
                throw new IllegalArgumentException("Each saved request must use its matching student object.");
            }
        }

        students = new ArrayList<Student>(savedStudents);
        requests = new ArrayList<AccessRequest>(savedRequests);
    }

}
