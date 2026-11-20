package model;

public class Administrator extends User {

    public Administrator(String passHash)
    {
        super("admin", "Administrator", "mohamed.nagy@ku.ac.ae", passHash);
    }

}