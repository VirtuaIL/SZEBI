package org.example;

import org.example.interfaces.IAcquisitionData;
import org.example.DTO.Urzadzenie;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;

public class ForecastScheduler {
    
    private final ForecastServiceAPI forecastService;
    private final IAcquisitionData acquisitionData;
    private final ConfigManager config;
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    
    public ForecastScheduler(ForecastServiceAPI forecastService, IAcquisitionData acquisitionData) {
        this.forecastService = forecastService;
        this.acquisitionData = acquisitionData;
        this.config = ConfigManager.getInstance();
    }
    
    public void start() {
        if (isRunning) {
            System.out.println("[SCHEDULER] Scheduler już działa.");
            return;
        }
        
        if (!config.isEnableAutoRetraining()) {
            System.out.println("[SCHEDULER] Automatyczny retrening jest wyłączony w konfiguracji.");
            return;
        }
        
        scheduler = Executors.newScheduledThreadPool(1);
        int intervalHours = config.getRetrainingIntervalHours();
        
        System.out.println("[SCHEDULER] Uruchamianie automatycznego retreningu (co " + intervalHours + " godzin)");
        
        scheduler.scheduleAtFixedRate(
            this::performScheduledRetraining,
            0,
            intervalHours,
            TimeUnit.HOURS
        );
        
        isRunning = true;
    }
    
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            isRunning = false;
            System.out.println("[SCHEDULER] Automatyczny retrening zatrzymany.");
        }
    }
    
    private void performScheduledRetraining() {
        System.out.println("\n[SCHEDULER] === AUTOMATYCZNY RETRENING ===");
        
        try {
            if (acquisitionData == null) {
                System.out.println("[SCHEDULER] Brak dostępu do danych akwizycji.");
                return;
            }
            
            List<Urzadzenie> activeDevices = acquisitionData.getActiveDevices();
            
            if (activeDevices == null || activeDevices.isEmpty()) {
                System.out.println("[SCHEDULER] Brak aktywnych urządzeń do retreningu.");
                return;
            }
            
            System.out.println("[SCHEDULER] Retrening dla " + activeDevices.size() + " urządzeń...");
            
            for (Urzadzenie device : activeDevices) {
                try {
                    forecastService.retrainModel(device.getId());
                } catch (Exception e) {
                    System.err.println("[SCHEDULER] Błąd retreningu dla urządzenia " + device.getId() + ": " + e.getMessage());
                }
            }
            
            System.out.println("[SCHEDULER] === ZAKOŃCZONO AUTOMATYCZNY RETRENING ===\n");
        } catch (Exception e) {
            System.err.println("[SCHEDULER] Błąd podczas automatycznego retreningu: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean isRunning() {
        return isRunning;
    }
}
