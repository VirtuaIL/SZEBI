package org.example.interfaces;

import java.util.List;
import org.example.DTO.Alert;

public interface IAlertData {

    void saveAlert(Alert alert);

    List<Alert> getAlertsByDeviceId(int deviceId);
}
