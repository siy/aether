package org.pragmatica.net;

import org.pragmatica.lang.Cause;

import java.nio.file.Path;

/**
 * Error types for TLS operations.
 */
public sealed interface TlsError extends Cause {
    /**
     * Failed to load certificate from file.
     */
    record CertificateLoadFailed(Path path, Throwable cause) implements TlsError {
        @Override
        public String message() {
            return "Failed to load certificate from " + path + ": " + cause.getMessage();
        }
    }

    /**
     * Failed to generate self-signed certificate.
     */
    record SelfSignedGenerationFailed(Throwable cause) implements TlsError {
        @Override
        public String message() {
            return "Failed to generate self-signed certificate: " + cause.getMessage();
        }
    }
}
