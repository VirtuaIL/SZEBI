package org.example.interfaces;

public interface IOptimizationData {

    /**
     * Zapisuje pojedynczą operację sterowania (np. zmianę temperatury).
     * 
     * @param deviceId  ID urządzenia
     * @param operation Nazwa operacji (np. "set_temp")
     * @param value     Nowa wartość (np. 21.5)
     * @param source    Źródło decyzji (np. "AUTO", "MANUAL", "USER_PREF")
     */
    void logOperation(int deviceId, String operation, double value, String source);

}
