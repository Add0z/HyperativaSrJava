package com.hyperativa.javaEspecialista.domain.exception;

/**
 * Thrown when batch file processing fails (I/O errors, format issues).
 */
public class FileProcessingException extends RuntimeException {

    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
