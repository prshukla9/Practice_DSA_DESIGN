package com.opentorelocate.practice.streams;

public class EmployeeStreamClass {

    int id;
    String name;
    String department;
    double salary;

    public EmployeeStreamClass(int id, String name, String department, double salary){

        this.id = id;
        this.name = name;
        this.department = department;
        this.salary = salary;
    }

    @Override
    public String toString() {
        return id + "id: " + name + "Name: " + department + " " + salary;
    }
}
