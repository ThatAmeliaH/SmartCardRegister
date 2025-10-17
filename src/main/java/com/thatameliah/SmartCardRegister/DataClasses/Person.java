package com.thatameliah.SmartCardRegister.DataClasses;

/*
    This class is a temporary replacement of "DataClasses/KotlinPerson.kt"
    Once Kotlin is configured on my college PC, this file will be deleted.
*/
public class Person {
    private String forename;
    private String surname;
    private String id;
    
    public Person(String forename, String surname, String id) {
        this.forename = forename;
        this.surname = surname;
        this.id = id;
    }

    public String getForename() { return forename; }

    public String getSurname() { return surname; }

    public String getId() { return id; }

    public void setForename(String forename) { this.forename = forename; }
    
    public void setSurname(String surname) { this.surname = surname; }

    public void setId(String id) { this.id = id; }
}
