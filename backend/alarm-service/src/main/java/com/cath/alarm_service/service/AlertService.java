package com.cath.alarm_service.service;
import com.cath.alarm_service.model.Alert;
import com.cath.alarm_service.config.AlertWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AlertService {
    @Autowired
    private AlertWebSocketHandler alertWebSocketHandler;

    public void triggerAlert(Alert alert) {
        alertWebSocketHandler.sendAlert(alert);
    }
}