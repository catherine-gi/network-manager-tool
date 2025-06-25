package com.cath.heartbeat_monitor.model;
import com.network.heartbeat.model.Alert;
import com.network.heartbeat.repository.AlertRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {
    private final AlertRepository alertRepository;

    @GetMapping
    public List<Alert> getActiveAlerts() {
        return alertRepository.findByNotifiedFalse();
    }

    @GetMapping("/history")
    public List<Alert> getAlertHistory() {
        return alertRepository.findAll();
    }

    @PostMapping("/{id}/acknowledge")
    public Alert acknowledgeAlert(@PathVariable String id) {
        return alertRepository.findById(id)
                .map(alert -> {
                    alert.setNotified(true);
                    return alertRepository.save(alert);
                })
                .orElseThrow(() -> new RuntimeException("Alert not found"));
    }
}