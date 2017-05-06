package com.example.leanhkhoi.android_recorder_app;

/**
 * Created by Le Anh Khoi on 4/17/2017.
 */

public class RecordFile {
    String name;
    int size;

    public RecordFile(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
