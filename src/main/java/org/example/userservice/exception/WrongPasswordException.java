package org.example.userservice.exception;

public class WrongPasswordException extends Exception{

    public WrongPasswordException(String message) {
        super(message);
    }
}
