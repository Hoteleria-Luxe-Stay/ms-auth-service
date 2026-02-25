package com.hotel.auth.domain.repository;

import com.hotel.auth.domain.model.ServiceClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceClientRepository extends JpaRepository<ServiceClient, Long> {

    Optional<ServiceClient> findByClientIdAndEnabledTrue(String clientId);

    Optional<ServiceClient> findByClientId(String clientId);
}
