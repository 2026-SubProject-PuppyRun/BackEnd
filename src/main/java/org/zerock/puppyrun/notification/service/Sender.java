package org.zerock.puppyrun.notification.service;

import java.util.List;

import org.zerock.puppyrun.notification.entity.NotificationType;
import org.zerock.puppyrun.notification.service.DTO.PushTask;
import org.zerock.puppyrun.notification.repository.DTO.EnabledNotifications;

public interface Sender {
    public List<PushTask> setPushTasks(NotificationType type, List<EnabledNotifications> memberSettings);

}
