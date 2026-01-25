# Opis Zaktualizowanego Diagramu UML GUI

## Główne Zmiany w Stosunku do Oryginalnego Diagramu

### 1. **Struktura Główna**
- **Usunięto:** `MainLayout` jako pojedynczy komponent zarządzający wszystkimi widokami
- **Dodano:** `App` jako główny komponent routingu oraz osobne panele dla każdej roli:
  - `AdminPanel`
  - `EngineerPanel`
  - `UserPanel`
  - `LoginPanel`

### 2. **Autentykacja**
- **Usunięto:** `AuthService` jako osobna klasa serwisowa
- **Zmieniono:** Autentykacja jest obsługiwana bezpośrednio w `LoginPanel` i `App` poprzez:
  - Funkcję `handleLogin()` w `App`
  - Zarządzanie stanem przez `localStorage` i React state

### 3. **Komunikacja z API**
- **Usunięto:** `APIGatewayService` jako klasa serwisowa
- **Dodano:** `ApiUtils` jako moduł z funkcjami pomocniczymi:
  - `getApiBaseUrl()` - automatyczne wykrywanie adresu API
  - `apiRequest()` - uniwersalna funkcja do żądań HTTP
  - `handleApiError()` - obsługa błędów API
  - `ApiError` - klasa błędu dla błędów API

### 4. **System Powiadomień**
- **Zmieniono:** `NotificationManager` → `ToastContainer`
- **Różnice:** 
  - Używa wzorca observer/listener zamiast bezpośrednich wywołań
  - Eksportuje funkcję `showToast()` używana globalnie
  - Renderuje komponenty `Toast` dynamicznie

### 5. **Nowe Komponenty**
Dodano komponenty, których nie było w oryginalnym diagramie:
- **`Navigation`** - menu nawigacyjne z różnymi opcjami dla różnych ról
- **`Header`** - nagłówek z informacjami o użytkowniku
- **`Monitoring`** - widok monitorowania urządzeń z wykresami historii
- **`DeviceManagement`** - zarządzanie urządzeniami (tylko admin)
- **`UserManagement`** - zarządzanie użytkownikami (tylko admin)
- **`Settings`** - ustawienia globalne (tylko admin)
- **`ErrorBoundary`** - obsługa błędów React

### 6. **Komponenty Wykresów**
- **Usunięto:** `ChartComponent` jako osobny komponent
- **Zmieniono:** Wykresy są bezpośrednio osadzone w komponentach używając biblioteki Recharts:
  - `Dashboard` używa `LineChart` i `BarChart`
  - `Reports` używa `LineChart` i `ScatterChart`
  - `Monitoring` używa `LineChart`

### 7. **Widoki (Views)**
Zaktualizowano istniejące widoki zgodnie z implementacją:
- **`Dashboard`** - dodano obsługę różnych zakresów czasowych (dzień/tydzień/miesiąc)
- **`ControlPanel`** - dodano obsługę statusu OZE i różnych typów urządzeń
- **`AlertsCenter`** - dodano sortowanie, filtrowanie i akcje potwierdzania/rozwiązywania
- **`Reports`** - dodano wykrywanie anomalii i eksport raportów

## Struktura Relacji

### Kompozycja (Composition)
- `App` komponuje wszystkie panele i `ToastContainer`
- Każdy panel komponuje `Navigation`, `Header` i odpowiednie widoki
- `ErrorBoundary` opakowuje całą aplikację

### Agregacja (Aggregation)
- Widoki używają komponentów wykresów z biblioteki Recharts

### Zależności (Dependencies)
- Wszystkie widoki zależą od `ApiUtils` do komunikacji z backendem
- Większość widoków używa `ToastContainer` do wyświetlania powiadomień
- `ApiUtils` może rzucać `ApiError`

## Różnice Architektoniczne

### Oryginalny Diagram (Teoretyczny)
- Centralizowana architektura z `MainLayout`
- Serwisy jako klasy (`AuthService`, `APIGatewayService`)
- Osobny komponent wykresów

### Aktualna Implementacja (React)
- Rozproszona architektura z panelami dla ról
- Funkcje pomocnicze zamiast klas serwisowych
- Wykresy osadzone bezpośrednio w komponentach
- Wykorzystanie React hooks i funkcjonalnych komponentów

## Uwagi Techniczne

1. **React Functional Components:** Wszystkie komponenty są funkcjonalne, nie klasowe
2. **State Management:** Używa React hooks (`useState`, `useEffect`) zamiast klasowych komponentów
3. **API Communication:** Bezpośrednie wywołania `fetch` i funkcje pomocnicze zamiast klasy serwisowej
4. **Routing:** Routing odbywa się przez warunkowe renderowanie w `App`, nie przez router zewnętrzny
5. **Authentication:** Stan autentykacji przechowywany w `localStorage` i React state

## Plik Diagramu

Diagram UML został zapisany w formacie PlantUML w pliku `GUI_UML_DIAGRAM.puml`.

Aby wyświetlić diagram:
1. Zainstaluj PlantUML plugin w swoim IDE (IntelliJ IDEA, VS Code)
2. Otwórz plik `GUI_UML_DIAGRAM.puml`
3. Diagram zostanie automatycznie wyrenderowany

Alternatywnie możesz użyć narzędzi online:
- http://www.plantuml.com/plantuml/uml/
- https://plantuml-editor.kkeisuke.com/
