package model;

public class AccessRequest {

    private Student student;
    private String status;

    public AccessRequest(Student student)
    {
        this.student = student;
        this.status = "PENDING";
    }

    public Student getStudent()
    {
        return student;
    }

    public String getStatus()
    {
        return status;
    }

    public void approve()
    {
        if (!status.equals("PENDING"))
        {
            throw new IllegalStateException("Request already reviewed.");
        }

        status = "APPROVED";
    }

    public void reject()
    {
        if (!status.equals("PENDING"))
        {
            throw new IllegalStateException("Request already reviewed.");
        }

        status = "REJECTED";
    }

}