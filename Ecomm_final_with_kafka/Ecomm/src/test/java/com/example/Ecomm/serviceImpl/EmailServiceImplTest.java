package com.example.Ecomm.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmailServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Inject @Value fields manually
        ReflectionTestUtils.setField(emailService, "mailtrapApiUrl", "http://fake-mailtrap-url");
        ReflectionTestUtils.setField(emailService, "mailtrapApiToken", "fake-token");
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Test Sender");

        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");
        when(restTemplate.postForEntity(
                any(String.class),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("OK"));
    }

    @Test
    void testSendEmail() {
        assertDoesNotThrow(() ->
                emailService.sendEmail(
                        "user@test.com",
                        "Test Subject",
                        "Test Body"
                )
        );

        verify(restTemplate, times(1))
                .postForEntity(any(String.class), any(), eq(String.class));
    }

    @Test
    void testSend2faCode() {
        assertDoesNotThrow(() ->
                emailService.send2faCode(
                        "user@test.com",
                        "123456",
                        5
                )
        );

        verify(restTemplate, times(1))
                .postForEntity(any(String.class), any(), eq(String.class));
    }
}
