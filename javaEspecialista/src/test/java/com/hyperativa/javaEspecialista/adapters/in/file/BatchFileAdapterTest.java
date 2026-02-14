package com.hyperativa.javaEspecialista.adapters.in.file;

import com.hyperativa.javaEspecialista.adapters.in.web.dto.BatchResponse;
import com.hyperativa.javaEspecialista.domain.ports.in.CardInputPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchFileAdapterTest {

    @Mock
    private CardInputPort cardInputPort;

    @InjectMocks
    private BatchFileAdapter batchFileAdapter;

    @Test
    void processFile_ShouldProcessValidLinesAndSkipHeaderFooter() throws IOException {
        // Arrange
        String content = "DESAFIO HEADER\n" +
                "DATA   1234567890123452   REST\n" + // Valid card 1
                "DATA   1234567890123452   REST\n" + // Valid card 2
                "LOTE FOOTER\n" +
                "SHORT\n" +
                "\n";

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain",
                content.getBytes(StandardCharsets.UTF_8));

        // Act
        BatchResponse response = batchFileAdapter.processFile(file);

        // Assert
        assertNotNull(response);
        assertEquals(6, response.totalLinesProcessed()); // 6 lines in total
        assertEquals(2, response.successCount());
        assertEquals(0, response.failureCount());
        assertEquals(0, response.errors().size());
        verify(cardInputPort, times(2)).registerCard("1234567890123452");
    }

    @Test
    void processFile_WhenCardRegistrationFails_ShouldRecordError() throws IOException {
        // Arrange
        String content = "DATA   1234567890123452   REST\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain",
                content.getBytes(StandardCharsets.UTF_8));

        when(cardInputPort.registerCard(anyString()))
                .thenThrow(new com.hyperativa.javaEspecialista.domain.exception.CardValidationException("Luhn failed"));

        // Act
        BatchResponse response = batchFileAdapter.processFile(file);

        // Assert
        assertEquals(1, response.totalLinesProcessed());
        assertEquals(0, response.successCount());
        assertEquals(1, response.failureCount());
        assertEquals(1, response.errors().size());
        assertEquals("Luhn failed", response.errors().get(0).reason());
        assertEquals("****3452", response.errors().get(0).cardNumber());
    }

    @Test
    void processFile_WhenEmptyCardNumber_ShouldRecordError() throws IOException {
        // Arrange
        String content = "DATA                      REST\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain",
                content.getBytes(StandardCharsets.UTF_8));

        // Act
        BatchResponse response = batchFileAdapter.processFile(file);

        // Assert
        assertEquals(1, response.totalLinesProcessed());
        assertEquals(0, response.successCount());
        assertEquals(1, response.failureCount());
        assertEquals(1, response.errors().size());
        assertEquals("Empty card number", response.errors().get(0).reason());
    }

    @Test
    void processFile_WhenIOException_ShouldThrowRuntimeException() throws IOException {
        // Arrange
        MultipartFileMock file = mock(MultipartFileMock.class);
        when(file.getInputStream()).thenThrow(new IOException("Read error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> batchFileAdapter.processFile(file));
    }

    // Workaround for Mockito and MultipartFile.getInputStream()
    interface MultipartFileMock extends org.springframework.web.multipart.MultipartFile {
    }
}
