package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Value("${mailtrap.api.url}")
    private String mailtrapApiUrl;

    @Value("${mailtrap.api.token}")
    private String mailtrapApiToken;

    @Value("${mailtrap.from.email}")
    private String fromEmail;

    @Value("${mailtrap.from.name}")
    private String fromName;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public EmailServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + mailtrapApiToken);

        try {
            String jsonPayload = objectMapper.writeValueAsString(
                    Map.of(
                            "from", Map.of("email", fromEmail, "name", fromName),
                            "to", Collections.singletonList(Map.of("email", to)),
                            "subject", subject,
                            "text", body
                    )
            );

            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
            restTemplate.postForEntity(mailtrapApiUrl, request, String.class);

            logger.info("Email sent successfully via Mailtrap API to={} subject={}", to, subject);

        } catch (HttpClientErrorException e) {
            logger.error(
                    "Mailtrap API HTTP error while sending email to={} status={} response={}",
                    to,
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
            throw new IllegalStateException("Failed to send email due to Mailtrap API error", e);

        } catch (Exception e) {
            logger.error(
                    "Unexpected error while sending email via Mailtrap API to={}",
                    to,
                    e
            );
            throw new IllegalStateException("Unexpected error while sending email", e);
        }
    }

    @Override
    public void send2faCode(String to, String code, int validityMinutes) {

        String subject = "Your 2FA Verification Code for Ecomm App";
        String body = "Hello,\n\n"
                + "Your two-factor authentication code is: " + code + "\n\n"
                + "This code is valid for " + validityMinutes + " minutes. Do not share it with anyone.\n\n"
                + "If you did not request this code, please ignore this email.\n\n"
                + "Thank you,\n"
                + "Ecomm App Team";

        sendEmail(to, subject, body);
    }
}
