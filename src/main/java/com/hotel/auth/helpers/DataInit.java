package com.hotel.auth.helpers;

import com.hotel.auth.domain.model.Role;
import com.hotel.auth.domain.model.ServiceClient;
import com.hotel.auth.domain.repository.RoleRepository;
import com.hotel.auth.domain.repository.ServiceClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInit implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInit.class);

    private final RoleRepository roleRepository;
    private final ServiceClientRepository serviceClientRepository;

    @Value("${internal.clients.hotel-service.client-id:}")
    private String hotelClientId;

    @Value("${internal.clients.hotel-service.client-secret:}")
    private String hotelClientSecret;

    @Value("${internal.clients.hotel-service.scope:service:hotel}")
    private String hotelClientScope;

    @Value("${internal.clients.reserva-service.client-id:}")
    private String reservaClientId;

    @Value("${internal.clients.reserva-service.client-secret:}")
    private String reservaClientSecret;

    @Value("${internal.clients.reserva-service.scope:service:reserva}")
    private String reservaClientScope;

    @Value("${internal.clients.notificacion-service.client-id:}")
    private String notificacionClientId;

    @Value("${internal.clients.notificacion-service.client-secret:}")
    private String notificacionClientSecret;

    @Value("${internal.clients.notificacion-service.scope:service:notificacion}")
    private String notificacionClientScope;

    public DataInit(RoleRepository roleRepository, ServiceClientRepository serviceClientRepository) {
        this.roleRepository = roleRepository;
        this.serviceClientRepository = serviceClientRepository;
    }

    @Override
    public void run(String... args) {
        initRoles();
        initServiceClients();
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            LOGGER.info("[DATA-INIT] Creando roles iniciales...");
            
            Role userRole = new Role();
            userRole.setRolename("USER");
            roleRepository.save(userRole);
            LOGGER.info("[DATA-INIT] Rol USER creado");

            Role adminRole = new Role();
            adminRole.setRolename("ADMIN");
            roleRepository.save(adminRole);
            LOGGER.info("[DATA-INIT] Rol ADMIN creado");

            LOGGER.info("[DATA-INIT] Roles iniciales creados exitosamente");
        } else {
            LOGGER.info("[DATA-INIT] Los roles ya existen, omitiendo inicializacion");
        }
    }

    private void initServiceClients() {
        initServiceClient(hotelClientId, hotelClientSecret, hotelClientScope, "HOTEL");
        initServiceClient(reservaClientId, reservaClientSecret, reservaClientScope, "RESERVA");
        initServiceClient(notificacionClientId, notificacionClientSecret, notificacionClientScope, "NOTIFICACION");
    }

    private void initServiceClient(String clientId, String clientSecret, String scope, String label) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            LOGGER.warn("[DATA-INIT] Service client {} no configurado. Omitiendo.", label);
            return;
        }

        ServiceClient existing = serviceClientRepository.findByClientId(clientId).orElse(null);
        if (existing == null) {
            ServiceClient client = ServiceClient.builder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .scope(scope)
                    .enabled(true)
                    .build();
            serviceClientRepository.save(client);
            LOGGER.info("[DATA-INIT] Service client {} creado", label);
            return;
        }

        boolean updated = false;
        if (!clientSecret.equals(existing.getClientSecret())) {
            existing.setClientSecret(clientSecret);
            updated = true;
        }
        if (scope != null && !scope.equals(existing.getScope())) {
            existing.setScope(scope);
            updated = true;
        }
        if (Boolean.FALSE.equals(existing.getEnabled())) {
            existing.setEnabled(true);
            updated = true;
        }
        if (updated) {
            serviceClientRepository.save(existing);
            LOGGER.info("[DATA-INIT] Service client {} actualizado", label);
        } else {
            LOGGER.info("[DATA-INIT] Service client {} ya existe", label);
        }
    }
}
