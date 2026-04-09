package com.dagdockersim.simulation.infrastructure.persistence.session.repository;

import com.dagdockersim.simulation.infrastructure.persistence.session.entity.DeviceSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceSessionRepository extends JpaRepository<DeviceSessionEntity, Long> {
    List<DeviceSessionEntity> findAllByOrderByTerminalIdAscDeviceNameAsc();
}
