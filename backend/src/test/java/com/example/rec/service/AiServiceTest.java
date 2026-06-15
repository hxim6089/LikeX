package com.example.rec.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void returnsConfigurationMessageWithoutCallingApiWhenKeyIsMissing() {
        AiService service = new AiService(restTemplate);
        ReflectionTestUtils.setField(service, "apiKey", " ");

        Map<String, Object> result = service.chat("hello", List.of());

        assertEquals("AI service is unavailable because DEEPSEEK_API_KEY is not configured.",
                result.get("reply"));
        verifyNoInteractions(restTemplate);
    }
}
