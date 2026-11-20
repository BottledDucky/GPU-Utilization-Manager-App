package model;

public class Administrator extends User {

    public Administrator(String email, String passHash)
    {
        super("admin", "Administrator", email, passHash);
    }

}
