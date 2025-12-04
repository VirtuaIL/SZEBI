# Moduł Akwizycji Danych - System SZEBI

Moduł odpowiedzialny za komunikację z warstwą sprzętową, walidację danych pomiarowych oraz ich dystrybucję do Bazy Danych i Modułu Analizy.

## Główne Funkcjonalności
1.  **Cykliczna Akwizycja:** Pobieranie danych z czujników co 1 minutę.
2.  **Walidacja:** Weryfikacja odczytów względem zdefiniowanych norm (min/max).
3.  **Integracja:**
    * Pobieranie konfiguracji urządzeń z bazy danych przy starcie systemu.
    * Wysyłanie danych pomiarowych do bazy (trwały zapis).
    * Wysyłanie danych i błędów do Modułu Analizy w czasie rzeczywistym (Push).
4.  **Symulacja zużycia energii:** Każdy odczyt zawiera dodatkowo informację o poborze mocy przez urządzenie.
---

## Dla Zespołu Analizy i Raportowania (Integracja)

Moduł Akwizycji udostępnia mechanizm **Real-Time Push**, który pozwala Modułowi Analizy otrzymywać dane pomiarowe oraz powiadomienia o awariach natychmiast po ich wystąpieniu, bez konieczności odpytywania bazy danych.

Integracja odbywa się poprzez implementację interfejsu `org.example.IAnalysisService`.

### 1. Kontrakt Interfejsu
Aby odebrać dane, należy dostarczyć implementację interfejsu `org.example.IAnalysisService`.

### 2. Jak podłączyć implementację?
W pliku startowym (`runner/Main.java`) należy wstrzyknąć klasę implementującą ten interfejs w następujący sposób:
```java
// Main.java

// Zamiast Mocka:
// IAnalysisService analysisService = new MockAnalysisService();

// Użyjcie Waszej implementacji:
IAnalysisService analysisService = new RealAnalysisReceiver();

// Wstrzyknięcie do API i Serwisu
ErrorReporter errorReporter = new ErrorReporter(analysisService);
CollectionService service = new CollectionService(..., analysisService);
AcquisitionAPI api = new AcquisitionAPI(..., analysisService);
```
## Dokumentacja API (Dla innych modułów)

Głównym punktem dostępu do modułu jest klasa `org.example.AcquisitionAPI`.

### 1. Pobieranie listy dostępnych urządzeń
Zwraca listę identyfikatorów i nazw urządzeń, które są aktualnie monitorowane przez system.

* **Metoda:** `List<String> getAvailableDevices()`
* **Przykład użycia:**
    ```java
    List<String> devices = acquisitionApi.getAvailableDevices();
    // Wynik: ["[1] Czujnik Temp (temperatura_C)", "[2] Lampa (jasnosc_procent)"]
    ```

### 2. Wymuszenie odczytu (Na żądanie)
Wykonuje natychmiastowy odczyt fizyczny z konkretnego czujnika, pomijając harmonogram cykliczny. [cite_start]Odczyt jest od razu zapisywany w bazie[cite: 22].

* **Metoda:** `Double requestSensorRead(String deviceId)`
* **Parametry:** `deviceId` - ID urządzenia (String).
* **Zwraca:** Wartość odczytu (`Double`) lub `null` w przypadku błędu.
* **Przykład użycia:**
    ```java
    Double temp = acquisitionApi.requestSensorRead("1");
    if (temp != null) {
        System.out.println("Aktualna temperatura: " + temp);
    }
    ```

### 3. Wymuszenie pełnego odświeżenia danych
Uruchamia procedurę pobrania danych ze wszystkich czujników naraz.

* **Metoda:** `void requestAllData()`

---

## Instrukcja dla Deweloperów (Debugowanie)

Domyślnie system skonfigurowany jest na pracę produkcyjną (cykl co 1 minutę, brak logów testowych). Aby przejść w tryb deweloperski:

### Jak zmienić częstotliwość odczytów?
W pliku `src/main/java/org/example/CollectionService.java` w metodzie `runPeriodicCollectionTask()` zmień parametry harmonogramu:

**Produkcja (Obecnie):**
```java
scheduler.scheduleAtFixedRate(..., 0, 1, TimeUnit.MINUTES);
```
**Tryb testowy (Szybki):**
```java
scheduler.scheduleAtFixedRate(..., 0, 5, TimeUnit.SECONDS);
```
**Jak testować manualnie?**
W pliku `runner/Main.java` możesz dopisać poniższy blok kodu na końcu metody `main`, aby zasymulować interakcję z GUI:
```java
// Blok testowy
System.out.println("--- DEBUG ---");
System.out.println("Urządzenia: " + api.getAvailableDevices());
api.requestSensorRead("1"); // Wymuszenie odczytu dla ID 1
```
