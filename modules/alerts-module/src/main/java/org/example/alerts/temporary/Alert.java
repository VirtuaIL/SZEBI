package org.example.alerts.temporary;

import java.util.Date;

public class Alert {
    private int id;
    private String tresc;
    private AlertSeverity priorytet;
    private int deviceId;
    private int roomId;
    private String zrodlo;
    private Date dataWystapienia;
    private AlertStatus status;

    public void setId(int id) { this.id = id; }
    public int getId() { return id; }

    public void setTresc(String tresc) { this.tresc = tresc; }
    public String getTresc() { return tresc; }

    public void setPriorytet(AlertSeverity priorytet) { this.priorytet = priorytet; }
    public AlertSeverity getPriorytet() { return priorytet; }

    public void setDeviceId(int deviceId) { this.deviceId = deviceId; }
    public int getDeviceId() { return deviceId; }

    public void setRoomId(int roomId) { this.roomId = roomId; }
    public int getRoomId() { return roomId; }

    public void setZrodlo(String zrodlo) { this.zrodlo = zrodlo; }
    public String getZrodlo() { return zrodlo; }

    public void setDataWystapienia(Date date) { this.dataWystapienia = date; }
    public Date getDataWystapienia() { return dataWystapienia; }

    public void setStatus(AlertStatus status) { this.status = status; }
    public AlertStatus getStatus() { return status; }
}