package com.hyperativa.javaEspecialista.domain.ports.out;

/**
 * Port to access security context information.
 * Allows the domain to retrieve the current authenticated user without
 * depending on frameworks.
 */
public interface SecurityPort {

    /**
     * @return The username/subject of the currently authenticated user, or "system"
     *         if not authenticated.
     */
    /**
     * @return The username/subject of the currently authenticated user, or "system"
     *         if not authenticated.
     */
    String getCurrentUser();

    /**
     * @return The IP address of the current request, or null if not available.
     */
    String getCurrentIp();
}
