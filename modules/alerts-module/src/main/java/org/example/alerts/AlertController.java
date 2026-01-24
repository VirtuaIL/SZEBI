package org.example.alerts;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.example.alerts.dto.ZgloszenieDTO;
import org.example.DTO.Alert;

import java.util.List;

public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    public void zglosAnomalie(Context ctx) {
        ZgloszenieDTO zgloszenie = ctx.bodyAsClass(ZgloszenieDTO.class);
        alertService.zglosAnomalie(zgloszenie);
        ctx.status(HttpStatus.CREATED);
    }

    public void pobierzAktywneAlarmy(Context ctx) {
        List<Alert> alerty = alertService.pobierzAktywneAlarmy();
        ctx.json(alerty);
    }

    public void potwierdzAlarm(Context ctx) {
        // Pobieramy ID ze ścieżki (path parameter)
        int id = Integer.parseInt(ctx.pathParam("id"));
        alertService.potwierdzAlarm(id);
        ctx.status(HttpStatus.OK);
    }

    public void rozwiazAlarm(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        alertService.rozwiazAlarm(id);
        ctx.status(HttpStatus.OK);
    }
}