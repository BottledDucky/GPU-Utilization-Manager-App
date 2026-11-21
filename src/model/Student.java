package model;

public class Student extends User {

    private String phoneNum;
    private TimeAllowance timeAllowance;

    public Student(String username, String name, String email, String passHash, String phoneNum, TimeAllowance timeAllowance)
    {
        super(username, name, email, passHash);

        this.phoneNum = phoneNum;
        this.timeAllowance = timeAllowance;
    }

    public String getUniId()
    {
        return getUsername();
    }

    public String getPhoneNum()
    {
        return phoneNum;
    }

    public TimeAllowance getTimeAllowance()
    {
        return timeAllowance;
    }

    public void setPhoneNum(String phoneNum)
    {
        this.phoneNum = phoneNum;
    }

    public void setTimeAllowance(TimeAllowance timeAllowance)
    {
        if (timeAllowance == null)
        {
            throw new IllegalArgumentException("Time allowance cannot be null.");
        }

        if (this.timeAllowance != null)
        {
            throw new IllegalStateException("Time allowance already assigned.");
        }

        this.timeAllowance = timeAllowance;
    }

}