package org.fusesource.fabric.example.dosgi.impl;


import org.fusesource.fabric.example.dosgi.Service;

public class ServiceImpl implements Service {

    @Override
    public String messageFrom(String input) {
        return "Message from distributed service to : " + input;
    }
}
