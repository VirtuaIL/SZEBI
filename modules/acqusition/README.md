# Moduł akwizycji danych


## Jak korzystać?

Poprzez poniższe metody HTTP

## SPIS METOD

### Uruchomienie okresowego czytania danych
   ```http request
   /api/acquisition/startPeriodicCollectionTask
   ```
typ requestu: GET

format danych do przesłania: BRAK

odpowiedź:
```json
{"success":"true"}
```
lub
```json
{
  "success":"false",
  "error": "Błąd podczas uruchamiania zadania"
}
```

### Zlecenie pojedyńczego odczytu z urządzenia o podanym ID
   ```http request
   /api/devices/{id}/requestSingleRead
   ```

typ requestu: POST

format danych do przesłania: tylko {id} w URL

odpowiedź:
```json
{
  "success":"true",
  "message":"pomyślnie zlecono odczyt"
}
```
lub
```json
{
  "success":"false",
  "error": "Błąd podczas próby odczytu"
}
```

### Zlecenie pojedyńczego odczytu z wszystkich urządzeń
   ```http request
   /api/acquisition/requestAllRead
   ```

typ requestu: POST

format danych do przesłania: BRAK

odpowiedź:
```json
{
  "success":"true",
  "message":"pomyślnie zlecono odczyt na wszystkich urządzeniach"
}
```
lub
```json
{
  "success":"false",
  "error": "Błąd podczas próby odczytu"
}
```


### Utworzenie nowego urządzenia i dodanie go do modułu oraz bazy danych
   ```http request
   /api/acquisition/createDevice
   ```

typ requestu: PUT

format danych do przesłania:

```json
{
  "deviceName": "grzejnik",
  "roomID": 1,
  "modelID": 1,
  "minValue": 18,
  "maxValue": 28,
  "metricLabel": "temperatura_F",
  "powerUsage": 155
}
```

odpowiedź:
```json
{
  "success":"true",
  "message":"pomyślnie utworzono urzadzenie"
}
```
lub
```json
{
  "success":"false",
  "error": "Błąd podczas dodawania urządzenia"
}
```