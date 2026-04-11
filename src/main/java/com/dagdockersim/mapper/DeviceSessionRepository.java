package com.dagdockersim.mapper;

import com.dagdockersim.model.entity.DeviceSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceSessionRepository extends JpaRepository<DeviceSessionEntity, Long> {
    List<DeviceSessionEntity> findAllByOrderByTerminalIdAscDeviceNameAsc();
}
