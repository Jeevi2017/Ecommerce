package com.example.Ecomm.service;

public interface EmailService {
    
    void sendEmail(String to, String subject, String body);

    
    void send2faCode(String to, String code, int validityMinutes); 
}
