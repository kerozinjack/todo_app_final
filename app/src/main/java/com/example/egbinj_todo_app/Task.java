package com.example.egbinj_todo_app;

public class Task {
    String title;
    String priority;
    boolean done;
    boolean counted;

    public Task(String title, String priority, boolean done, boolean counted) {
        this.title = title;
        this.priority=priority;
        this.done = done;
        this.counted=false;
    }
}
