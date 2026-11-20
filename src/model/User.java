package model;

public abstract class User {

    private String username;
    private String name;
    private String email;
    private String passHash;

    public User(String username, String name, String email, String passHash)
    {
        this.username = username;
        this.name = name;
        this.email = email;
        this.passHash = passHash;
    }

    public String getUsername()
    {
        return username;
    }

    public String getName()
    {
        return name;
    }

    public String getEmail()
    {
        return email;
    }

    public String getPassHash()
    {
        return passHash;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

}