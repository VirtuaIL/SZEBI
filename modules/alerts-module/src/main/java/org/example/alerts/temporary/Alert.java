package org.example.alerts.temporary;

import java.util.Date;

public class Alert {
    private String tresc;
    private AlertSeverity priorytet;
    private int deviceId;
    private String zrodlo;
    private Date dataWystapienia;
    private AlertStatus status;

    // Gettery i Settery (możesz wygenerować Alt+Insert)
    public void setTresc(String tresc) { this.tresc = tresc; }
    public String getTresc() { return tresc; }

    public void setPriorytet(AlertSeverity priorytet) { this.priorytet = priorytet; }
    public AlertSeverity getPriorytet() { return priorytet; }

    public void setDeviceId(int deviceId) { this.deviceId = deviceId; }
    public int getDeviceId() { return deviceId; }

    public void setZrodlo(String zrodlo) { this.zrodlo = zrodlo; }
    public String getZrodlo() { return zrodlo; }

    public void setDataWystapienia(Date date) { this.dataWystapienia = date; }
    public void setStatus(AlertStatus status) { this.status = status; }
}