package com.hotel.auth.helpers;

import com.hotel.auth.domain.model.Role;
import com.hotel.auth.domain.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInit implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataInit.class);

    private final RoleRepository roleRepository;

    public DataInit(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        initRoles();
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
}
