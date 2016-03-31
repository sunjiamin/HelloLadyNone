package com.sun.hellolady.demo;

/**
 * Created by Jiamin.Sun on 1/25/2016.
 */
public class Person {
    private  String age;
    private String name;

    public Person(String name, String age){
        this.age=age;
        this.name=name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
