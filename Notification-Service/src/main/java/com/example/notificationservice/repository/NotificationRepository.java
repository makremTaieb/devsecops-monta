package com.example.notificationservice.repository;
import com.example.notificationservice.entities.Notification;
import com.example.notificationservice.entities.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByProjectId(Long projectId);
    List<Notification> findByPipelineExecutionId(Long pipelineExecutionId);
    List<Notification> findByStatus(NotificationStatus status);
    List<Notification> findBySourceService(String sourceService);
}
