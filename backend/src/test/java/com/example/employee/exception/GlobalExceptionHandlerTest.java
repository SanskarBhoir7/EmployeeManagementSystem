package com.example.employee.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void reportsMultipartLimitAsPayloadTooLarge() {
        var response = new GlobalExceptionHandler().handleUploadTooLarge(new MaxUploadSizeExceededException(5L));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals(413, response.getBody().getStatus());
    }
}
