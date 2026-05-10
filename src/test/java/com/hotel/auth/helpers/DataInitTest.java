package com.hotel.auth.helpers;

import com.hotel.auth.domain.model.Role;
import com.hotel.auth.domain.model.ServiceClient;
import com.hotel.auth.domain.repository.RoleRepository;
import com.hotel.auth.domain.repository.ServiceClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitTest {

    @Mock private RoleRepository roleRepository;
    @Mock private ServiceClientRepository serviceClientRepository;

    @InjectMocks
    private DataInit dataInit;

    @BeforeEach
    void setUp() {
        // Default: no service client config — overridden per test as needed
        ReflectionTestUtils.setField(dataInit, "hotelClientId", "");
        ReflectionTestUtils.setField(dataInit, "hotelClientSecret", "");
        ReflectionTestUtils.setField(dataInit, "hotelClientScope", "service:hotel");
        ReflectionTestUtils.setField(dataInit, "reservaClientId", "");
        ReflectionTestUtils.setField(dataInit, "reservaClientSecret", "");
        ReflectionTestUtils.setField(dataInit, "reservaClientScope", "service:reserva");
        ReflectionTestUtils.setField(dataInit, "notificacionClientId", "");
        ReflectionTestUtils.setField(dataInit, "notificacionClientSecret", "");
        ReflectionTestUtils.setField(dataInit, "notificacionClientScope", "service:notificacion");
    }

    // ==================== initRoles ====================

    @Test
    void runCreatesUserAndAdminRolesWhenRepositoryEmpty() {
        when(roleRepository.count()).thenReturn(0L);

        dataInit.run();

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Role::getRolename)
                .containsExactlyInAnyOrder("USER", "ADMIN");
    }

    @Test
    void runSkipsRoleCreationWhenAlreadyExist() {
        when(roleRepository.count()).thenReturn(2L);

        dataInit.run();

        verify(roleRepository, never()).save(any());
    }

    // ==================== initServiceClients ====================

    @Test
    void runSkipsServiceClientWhenClientIdBlank() {
        when(roleRepository.count()).thenReturn(2L);

        dataInit.run();

        verify(serviceClientRepository, never()).save(any());
        verify(serviceClientRepository, never()).findByClientId(anyString());
    }

    @Test
    void runCreatesServiceClientWhenNotInRepository() {
        when(roleRepository.count()).thenReturn(2L);
        ReflectionTestUtils.setField(dataInit, "hotelClientId", "hotel-svc");
        ReflectionTestUtils.setField(dataInit, "hotelClientSecret", "secret-x");
        when(serviceClientRepository.findByClientId("hotel-svc")).thenReturn(Optional.empty());

        dataInit.run();

        ArgumentCaptor<ServiceClient> captor = ArgumentCaptor.forClass(ServiceClient.class);
        verify(serviceClientRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getClientId()).isEqualTo("hotel-svc");
        assertThat(captor.getValue().getClientSecret()).isEqualTo("secret-x");
        assertThat(captor.getValue().getEnabled()).isTrue();
    }

    @Test
    void runUpdatesServiceClientWhenSecretChanges() {
        when(roleRepository.count()).thenReturn(2L);
        ReflectionTestUtils.setField(dataInit, "hotelClientId", "hotel-svc");
        ReflectionTestUtils.setField(dataInit, "hotelClientSecret", "new-secret");

        ServiceClient existing = ServiceClient.builder()
                .id(1L)
                .clientId("hotel-svc")
                .clientSecret("old-secret")
                .scope("service:hotel")
                .enabled(true)
                .build();
        when(serviceClientRepository.findByClientId("hotel-svc")).thenReturn(Optional.of(existing));

        dataInit.run();

        verify(serviceClientRepository, times(1)).save(existing);
        assertThat(existing.getClientSecret()).isEqualTo("new-secret");
    }

    @Test
    void runUpdatesServiceClientWhenScopeChanges() {
        when(roleRepository.count()).thenReturn(2L);
        ReflectionTestUtils.setField(dataInit, "hotelClientId", "hotel-svc");
        ReflectionTestUtils.setField(dataInit, "hotelClientSecret", "secret-x");
        ReflectionTestUtils.setField(dataInit, "hotelClientScope", "service:hotel:new");

        ServiceClient existing = ServiceClient.builder()
                .id(1L)
                .clientId("hotel-svc")
                .clientSecret("secret-x")
                .scope("service:hotel:old")
                .enabled(true)
                .build();
        when(serviceClientRepository.findByClientId("hotel-svc")).thenReturn(Optional.of(existing));

        dataInit.run();

        verify(serviceClientRepository, times(1)).save(existing);
        assertThat(existing.getScope()).isEqualTo("service:hotel:new");
    }

    @Test
    void runReEnablesServiceClientWhenDisabled() {
        when(roleRepository.count()).thenReturn(2L);
        ReflectionTestUtils.setField(dataInit, "hotelClientId", "hotel-svc");
        ReflectionTestUtils.setField(dataInit, "hotelClientSecret", "secret-x");

        ServiceClient existing = ServiceClient.builder()
                .id(1L)
                .clientId("hotel-svc")
                .clientSecret("secret-x")
                .scope("service:hotel")
                .enabled(false)
                .build();
        when(serviceClientRepository.findByClientId("hotel-svc")).thenReturn(Optional.of(existing));

        dataInit.run();

        verify(serviceClientRepository, times(1)).save(existing);
        assertThat(existing.getEnabled()).isTrue();
    }

    @Test
    void runDoesNotSaveWhenServiceClientExistsAndUnchanged() {
        when(roleRepository.count()).thenReturn(2L);
        ReflectionTestUtils.setField(dataInit, "hotelClientId", "hotel-svc");
        ReflectionTestUtils.setField(dataInit, "hotelClientSecret", "secret-x");

        ServiceClient existing = ServiceClient.builder()
                .id(1L)
                .clientId("hotel-svc")
                .clientSecret("secret-x")
                .scope("service:hotel")
                .enabled(true)
                .build();
        when(serviceClientRepository.findByClientId("hotel-svc")).thenReturn(Optional.of(existing));

        dataInit.run();

        verify(serviceClientRepository, never()).save(any());
    }
}
