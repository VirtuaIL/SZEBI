package org.example.alerts.dto;

import org.example.alerts.temporary.AlertSeverity; // Import tymczasowego Enuma

public class ZgloszenieDTO {
    private String tresc;
    private int deviceId;
    private AlertSeverity priorytet;
    private String zrodlo;

    // Konstruktor
    public ZgloszenieDTO(String tresc, int deviceId, AlertSeverity priorytet, String zrodlo) {
        this.tresc = tresc;
        this.deviceId = deviceId;
        this.priorytet = priorytet;
        this.zrodlo = zrodlo;
    }

    // Gettery
    public String getTresc() { return tresc; }
    public int getDeviceId() { return deviceId; }
    public AlertSeverity getPriorytet() { return priorytet; }
    public String getZrodlo() { return zrodlo; }
}