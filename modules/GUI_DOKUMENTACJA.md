# 📱 Dokumentacja GUI - System SZEBI

## 📋 Spis Treści

1. [Architektura GUI](#architektura-gui)
2. [System Autoryzacji i Ról](#system-autoryzacji-i-ról)
3. [Główne Komponenty i Funkcjonalności](#główne-komponenty-i-funkcjonalności)
4. [Komunikacja z Backendem](#komunikacja-z-backendem)
5. [System Powiadomień](#system-powiadomień)
6. [Obsługa Błędów](#obsługa-błędów)
7. [Responsywność i UX](#responsywność-i-ux)
8. [Szczegóły Implementacji Responsywności](#-szczegóły-implementacji-responsywności)
9. [Zarządzanie Stanem](#zarządzanie-stanem)
10. [Używane Biblioteki](#używane-biblioteki)
11. [Uwagi Implementacyjne](#uwagi-implementacyjne)

---

## 🏗️ Architektura GUI

GUI jest aplikacją **React (v19)** zbudowaną z użyciem **Vite**. Struktura projektu:

```
modules/gui/web/
├── src/
│   ├── App.jsx              # Główny komponent aplikacji
│   ├── main.jsx             # Punkt wejścia
│   ├── Panels/              # Panele dla różnych ról użytkowników
│   │   ├── LoginPanel.jsx   # Panel logowania
│   │   ├── AdminPanel.jsx   # Panel administratora
│   │   ├── EngineerPanel.jsx # Panel inżyniera
│   │   └── UserPanel.jsx    # Panel najemcy
│   ├── components/          # Komponenty wielokrotnego użytku
│   │   ├── Dashboard.jsx    # Dashboard z wykresami
│   │   ├── AlertsCenter.jsx # Centrum alarmów
│   │   ├── Monitoring.jsx   # Monitoring urządzeń
│   │   ├── ControlPanel.jsx # Panel sterowania
│   │   ├── Reports.jsx      # Generator raportów
│   │   ├── Navigation.jsx   # Nawigacja boczna
│   │   ├── Header.jsx       # Nagłówek aplikacji
│   │   └── ToastContainer.jsx # System powiadomień
│   └── utils/
│       └── api.js           # Narzędzia do komunikacji z API
```

### Technologie

- **React 19.2.0** - Framework UI
- **Vite 7.2.4** - Build tool i dev server
- **Recharts 3.5.1** - Biblioteka do wykresów
- **ESLint** - Linting kodu

---

## 🔐 System Autoryzacji i Ról

### 1. Proces Logowania

**Komponent:** `LoginPanel.jsx`

- Formularz logowania z polami email i hasło
- Wysyła żądanie POST do `/api/login`
- Backend weryfikuje dane i zwraca rolę użytkownika
- Dane użytkownika zapisywane w `localStorage`:
  - `user_role` - rola użytkownika
  - `user_data` - pełne dane użytkownika (JSON)

### 2. Trzy Poziomy Dostępu

| Rola | Dostęp | Opis |
|------|--------|------|
| **Administrator** (`admin`) | Pełny dostęp | Wszystkie funkcje + zarządzanie urządzeniami, użytkownikami i ustawieniami |
| **Inżynier** (`engineer`) | Rozszerzony | Monitoring, alarmy, raporty, sterowanie |
| **Najemca** (`user`) | Ograniczony | Dashboard, podstawowe sterowanie, raporty |

### 3. Routing Oparty na Roli

**Komponent:** `App.jsx`

- Sprawdza `userRole` z `localStorage` przy starcie
- Renderuje odpowiedni panel w zależności od roli:
  - `AdminPanel` - dla administratorów
  - `EngineerPanel` - dla inżynierów
  - `UserPanel` - dla najemców
- Jeśli brak roli → wyświetla `LoginPanel`

---

## 🎯 Główne Komponenty i Funkcjonalności

### 1. Dashboard (`Dashboard.jsx`)

**Funkcjonalności:**
- 📊 Wykresy zużycia energii (biblioteka Recharts)
- 📈 Porównanie rzeczywistego vs prognozowanego zużycia
- 🔍 Wykrywanie i wizualizacja anomalii
- 📊 Statystyki:
  - Całkowite zużycie (kWh)
  - Średnie zużycie (kWh)
  - Liczba wykrytych anomalii
- 💰 Dla najemcy: kalkulator kosztów energii
- ⏱️ Automatyczne odświeżanie co 60 sekund

**Widoki czasowe:**
- Dzień (24 godziny)
- Tydzień (7 dni)
- Miesiąc (30 dni)

### 2. Centrum Alarmów (`AlertsCenter.jsx`)

**Funkcjonalności:**
- 📋 Lista aktywnych i rozwiązanych alarmów
- 🔄 Sortowanie według:
  - Data (rosnąco/malejąco)
  - Urządzenie (alfabetycznie)
  - Priorytet (CRITICAL → WARNING → INFO)
- 🚨 Priorytety alarmów:
  - `CRITICAL` - krytyczne (czerwony)
  - `WARNING` - ostrzeżenia (pomarańczowy)
  - `INFO` - informacyjne (niebieski)
- 📊 Statusy alarmów:
  - `NOWY` - nowy alarm
  - `POTWIERDZONY` - potwierdzony przez użytkownika
  - `WYSLANY` - wysłany powiadomienie
  - `ROZWIAZANY` - rozwiązany
- ✅ Dla inżyniera/admina:
  - Przycisk "Potwierdź" (dla alarmów NOWY/WYSLANY)
  - Przycisk "Rozwiąż" (dla wszystkich aktywnych)
- 🔔 Automatyczne powiadomienia toast dla nowych alarmów
- ⏱️ Odświeżanie co 30 sekund

**Statystyki:**
- Liczba alarmów krytycznych
- Liczba ostrzeżeń
- Liczba aktywnych alarmów

### 3. Monitoring (`Monitoring.jsx`)

**Funkcjonalności:**
- 📋 Tabela wszystkich urządzeń z aktualnymi odczytami
- 🔍 Szczegóły urządzenia po kliknięciu:
  - Typ, lokalizacja, status
  - Aktualny odczyt z możliwością ręcznego odświeżenia
  - Ostatnia aktualizacja
- 📈 Wykres historii odczytów (ostatnie 24h)
- ⚡ Automatyczne odświeżanie:
  - Lista urządzeń: co 30 sekund
  - Wartości urządzeń: co 15 sekund
  - Wybrane urządzenie: co 10 sekund
- 📊 Agregacja danych (gdy >50 odczytów, grupowanie co 15-30 min)

**Statusy urządzeń:**
- `DZIAŁA` - urządzenie działa poprawnie (zielony)
- `BŁĄD` - urządzenie ma problemy (czerwony)

### 4. Panel Sterowania (`ControlPanel.jsx`)

**Funkcjonalności:**
- ☀️ Status OZE:
  - Produkcja energii z OZE (W)
  - Pobór z sieci (W)
  - Wizualizacja proporcji (pasek)
- 🎛️ Sterowanie urządzeniami:
  - **Klimatyzacja**: ustawianie temperatury docelowej (16-30°C)
  - **Oświetlenie**: regulacja jasności (0-100%)
  - **Wentylacja**: poziom nawiewu (0-max poziomów)
- 🔘 Włączanie/wyłączanie urządzeń (toggle switch)

**⚠️ Uwaga:** Obecnie używa danych mockowych (TODO: integracja z API)

### 5. Generator Raportów (`Reports.jsx`)

**Funkcjonalności:**
- 🔍 Filtry:
  - Typ raportu (zużycie energii, historia alarmów, stan urządzeń, koszty)
  - Medium (prąd, temperatura, wilgotność, wszystkie)
  - Zakres dat (od-do)
  - Strefa (cały budynek, piętra, pokoje)
- 📊 Wizualizacja z wykrytymi anomaliami
- 📄 Eksport (PDF, JSON, XML) - **do implementacji**

### 6. Nawigacja (`Navigation.jsx`)

**Funkcjonalności:**
- 📱 Menu boczne z ikonami
- 🔐 Różne opcje w zależności od roli:
  - **Admin**: Wszystkie opcje + Urządzenia, Użytkownicy, Ustawienia
  - **Inżynier**: Dashboard, Sterowanie, Monitoring, Alarmy, Raporty
  - **Najemca**: Dashboard, Sterowanie, Raporty
- 📱 Responsywne (hamburger menu na mobile)
- ✨ Wyróżnienie aktywnej sekcji

**Menu items:**
- 📊 Dashboard
- 🎛️ Sterowanie
- 📡 Monitoring
- 🚨 Alarmy
- 📈 Raporty
- 🔧 Urządzenia (tylko admin)
- 👥 Użytkownicy (tylko admin)
- ⚙️ Ustawienia (tylko admin)

---

## 🌐 Komunikacja z Backendem

### System API (`utils/api.js`)

**Funkcjonalności:**
- 🔍 Automatyczne wykrywanie adresu API (ten sam host, port 8080)
- 🌍 Działa zarówno na localhost jak i w sieci lokalnej
- 📡 Funkcja `apiRequest()`:
  - Obsługa błędów HTTP
  - Parsowanie JSON
  - Obsługa błędów sieciowych
- 🛡️ Klasa `ApiError` do obsługi błędów

### Endpointy używane przez GUI

| Metoda | Endpoint | Opis |
|--------|----------|------|
| `POST` | `/api/login` | Logowanie użytkownika |
| `GET` | `/api/alerts` | Pobranie listy alarmów |
| `POST` | `/api/alerts/{id}/acknowledge` | Potwierdzenie alarmu |
| `POST` | `/api/alerts/{id}/resolve` | Rozwiązanie alarmu |
| `GET` | `/api/devices` | Lista wszystkich urządzeń |
| `POST` | `/api/devices/{id}/read` | Odczyt aktualnej wartości urządzenia |
| `GET` | `/api/devices/{id}/readings` | Historia odczytów urządzenia |

### Przykład użycia API

```javascript
import { apiRequest, handleApiError } from '../utils/api';
import { showToast } from './ToastContainer';

try {
  const alerts = await apiRequest('/alerts');
  setAlerts(alerts);
} catch (error) {
  handleApiError(error, showToast);
}
```

---

## 🔔 System Powiadomień (Toast)

**Komponenty:**
- `ToastContainer.jsx` - globalny kontener powiadomień
- `Toast.jsx` - pojedyncze powiadomienie

**Funkcjonalności:**
- 🌐 Funkcja globalna `showToast(message, type, duration)`
- 🎨 Typy powiadomień:
  - `success` - sukces (zielony)
  - `error` - błąd (czerwony)
  - `warning` - ostrzeżenie (pomarańczowy)
  - `info` - informacja (niebieski)
- ⏱️ Automatyczne zamykanie po określonym czasie
- 📍 Pozycjonowanie w prawym górnym rogu

**Użycie:**
```javascript
import { showToast } from './ToastContainer';

showToast('Operacja zakończona sukcesem', 'success', 3000);
showToast('Wystąpił błąd', 'error', 5000);
```

**Zastosowania:**
- Powiadomienia o nowych alarmach
- Potwierdzenia akcji (potwierdzenie alarmu, rozwiązanie)
- Błędy API
- Informacje o aktualizacji danych

---

## ⚠️ Obsługa Błędów

### Error Boundary (`ErrorBoundary.jsx`)

**Funkcjonalności:**
- 🛡️ React Error Boundary przechwytujący błędy renderowania
- 💬 Wyświetla przyjazny komunikat błędu
- 🔄 Przycisk do odświeżenia strony
- 📝 Logowanie błędów do konsoli

### Obsługa Błędów API

**Funkcja:** `handleApiError(error, showToast)`

- 🔍 Wykrywa typ błędu (sieć, HTTP, inne)
- 💬 Wyświetla odpowiedni komunikat użytkownikowi
- 🔔 Używa systemu toast do powiadomień
- 📊 Obsługuje różne kody statusu HTTP

**Przykłady błędów:**
- `0` - Brak połączenia z serwerem
- `400` - Błędne żądanie
- `401` - Brak autoryzacji
- `404` - Nie znaleziono
- `500` - Błąd serwera

---

## 📱 Responsywność i UX

### Design Responsywny

- 📱 **Mobile-first** - działa na urządzeniach mobilnych
- 💻 **Desktop** - pełna funkcjonalność na większych ekranach
- 🍔 **Hamburger menu** - menu boczne na mobile
- 🎯 **Overlay** - ciemne tło przy otwartym menu na mobile
- ✨ **Smooth transitions** - płynne przejścia między widokami

### Doświadczenie Użytkownika

- ⏳ **Loading states** - wskaźniki ładowania dla asynchronicznych operacji
- 🎨 **Wizualne wskaźniki** - kolory, badge'e, ikony dla statusów
- 🔄 **Automatyczne odświeżanie** - dane aktualizowane w tle
- 💬 **Komunikaty błędów** - przyjazne komunikaty zamiast technicznych błędów
- ✅ **Potwierdzenia akcji** - powiadomienia o udanych operacjach

---

## 📱 Szczegóły Implementacji Responsywności

### Strategia Implementacji

Aplikacja została zaprojektowana jako **desktop-first**, a następnie dostosowana do urządzeń mobilnych poprzez **media queries** z breakpointem `768px`. Wszystkie komponenty mają dedykowane style mobilne.

### Breakpoint

```css
@media (max-width: 768px) {
  /* Style mobilne */
}
```

**Uzasadnienie:** 768px to standardowy breakpoint dla tabletów i telefonów w trybie landscape/portrait.

---

### 1. 🍔 Nawigacja Boczna (Navigation)

#### Desktop (domyślnie):
- **Pozycja:** Fixed, zawsze widoczna po lewej stronie
- **Szerokość:** 250px
- **Z-index:** 1001 (nad innymi elementami)

#### Mobile (max-width: 768px):
- **Ukrycie domyślne:** `transform: translateX(-100%)` - menu schowane poza ekranem
- **Pokazanie:** Klasa `.nav-open` → `transform: translateX(0)` - slide-in animacja
- **Szerokość:** 280px (szersze dla lepszej czytelności)
- **Przycisk zamknięcia:** Widoczny tylko na mobile (`.nav-close-btn`)
- **Overlay:** Ciemne tło (`rgba(0,0,0,0.5)`) przy otwartym menu

**Implementacja:**
```css
/* Desktop - zawsze widoczne */
.navigation {
  width: 250px;
  position: fixed;
  left: 0;
  transition: transform 0.3s ease;
}

/* Mobile - ukryte domyślnie */
@media (max-width: 768px) {
  .navigation {
    transform: translateX(-100%);
    width: 280px;
  }
  
  .navigation.nav-open {
    transform: translateX(0);
  }
  
  .nav-close-btn {
    display: block; /* Widoczne tylko na mobile */
  }
  
  .nav-overlay {
    display: block; /* Overlay tylko na mobile */
  }
}
```

**Logika JavaScript:**
```javascript
// W komponencie Navigation
const [menuOpen, setMenuOpen] = useState(false);

// Automatyczne zamykanie po kliknięciu w element menu
const handleItemClick = (itemId) => {
  onViewChange(itemId);
  if (window.innerWidth <= 768 && onToggle) {
    onToggle(); // Zamknij menu
  }
};
```

---

### 2. 📱 Header (Nagłówek)

#### Desktop:
- **Pozycja:** Fixed, `left: 250px` (obok menu)
- **Hamburger menu:** Ukryte (`display: none`)

#### Mobile:
- **Pozycja:** `left: 0` (pełna szerokość)
- **Hamburger menu:** Widoczne (`display: block`)
- **Zmniejszone fonty:** Mniejsze rozmiary tekstu dla lepszego dopasowania
- **Mniejsze przyciski:** Zmniejszony padding przycisku wylogowania

**Implementacja:**
```css
/* Desktop */
.app-header {
  left: 250px;
}

.menu-toggle-btn {
  display: none; /* Ukryte na desktop */
}

/* Mobile */
@media (max-width: 768px) {
  .app-header {
    left: 0; /* Pełna szerokość */
  }
  
  .menu-toggle-btn {
    display: block; /* Widoczne na mobile */
  }
  
  .user-name {
    font-size: 13px; /* Zmniejszony z 14px */
  }
  
  .header-logout-btn {
    padding: 6px 12px; /* Zmniejszony z 8px 16px */
    font-size: 12px; /* Zmniejszony z 14px */
  }
}
```

---

### 3. 📊 Dashboard

#### Desktop:
- **Grid layout:** `grid-template-columns: repeat(auto-fit, minmax(400px, 1fr))`
- **Wykres:** `grid-column: span 2` (zajmuje 2 kolumny)
- **Statystyki:** 3 kolumny (`grid-template-columns: repeat(3, 1fr)`)

#### Mobile:
- **Single column:** Wszystkie karty w jednej kolumnie
- **Wykres:** Pełna szerokość (`grid-column: span 1`)
- **Statystyki:** Pionowo (`grid-template-columns: 1fr`)
- **Header:** Kolumna zamiast rzędu (`flex-direction: column`)
- **Przyciski czasu:** Pełna szerokość z `flex-wrap`

**Implementacja:**
```css
/* Desktop */
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
}

.chart-card {
  grid-column: span 2; /* Wykres zajmuje 2 kolumny */
}

.stats-grid {
  grid-template-columns: repeat(3, 1fr);
}

/* Mobile */
@media (max-width: 768px) {
  .dashboard-grid {
    grid-template-columns: 1fr; /* Single column */
    gap: 15px;
  }
  
  .chart-card {
    grid-column: span 1; /* Pełna szerokość */
  }
  
  .stats-grid {
    grid-template-columns: 1fr; /* Pionowo */
  }
  
  .dashboard-header {
    flex-direction: column; /* Kolumna zamiast rzędu */
    align-items: flex-start;
  }
  
  .time-range-selector button {
    flex: 1; /* Pełna szerokość przycisków */
    min-width: 80px;
  }
}
```

---

### 4. 🚨 Centrum Alarmów (AlertsCenter)

#### Desktop:
- **Statystyki:** Grid z auto-fit (`repeat(auto-fit, minmax(150px, 1fr))`)
- **Header alarmu:** Flex row (obok siebie)
- **Przyciski akcji:** Flex row (obok siebie)

#### Mobile:
- **Statystyki:** Single column (`grid-template-columns: 1fr`)
- **Header alarmu:** Flex column (pionowo)
- **Przyciski akcji:** Pełna szerokość, jeden pod drugim (`flex-direction: column`)
- **Kontrolki sortowania:** Wrap dla lepszego dopasowania

**Implementacja:**
```css
/* Desktop */
.alerts-stats {
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.alert-header {
  display: flex;
  justify-content: space-between; /* Obok siebie */
}

.alert-actions {
  display: flex;
  gap: 10px; /* Poziomo */
}

/* Mobile */
@media (max-width: 768px) {
  .alerts-stats {
    grid-template-columns: 1fr; /* Pionowo */
  }
  
  .alert-header {
    flex-direction: column; /* Pionowo */
    gap: 10px;
  }
  
  .alert-actions {
    flex-direction: column; /* Przyciski jeden pod drugim */
  }
  
  .btn-acknowledge,
  .btn-resolve {
    width: 100%; /* Pełna szerokość */
  }
  
  .sort-controls {
    flex-wrap: wrap; /* Zawijanie przy małej przestrzeni */
  }
}
```

---

### 5. 📡 Monitoring

#### Desktop:
- **Tabela:** Pełna szerokość, wszystkie kolumny widoczne
- **Szczegóły urządzenia:** Grid z auto-fit (`repeat(auto-fit, minmax(200px, 1fr))`)

#### Mobile:
- **Tabela:** Horizontal scroll (`overflow-x: scroll`)
- **Minimalna szerokość:** `min-width: 800px` (zapewnia czytelność)
- **Zmniejszone paddingi:** `padding: 8px` zamiast `12px`
- **Mniejsze fonty:** `font-size: 12px`
- **Szczegóły:** Single column (`grid-template-columns: 1fr`)

**Implementacja:**
```css
/* Desktop */
.devices-table-container {
  overflow-x: auto; /* Scroll tylko gdy potrzeba */
}

.devices-table th,
.devices-table td {
  padding: 12px;
}

.details-content {
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}

/* Mobile */
@media (max-width: 768px) {
  .devices-table-container {
    overflow-x: scroll; /* Wymuszony scroll poziomy */
  }
  
  .devices-table {
    min-width: 800px; /* Minimalna szerokość dla czytelności */
  }
  
  .devices-table th,
  .devices-table td {
    padding: 8px; /* Zmniejszone */
    font-size: 12px; /* Mniejsze fonty */
  }
  
  .details-content {
    grid-template-columns: 1fr; /* Single column */
  }
}
```

---

### 6. 🎛️ Panel Sterowania

#### Desktop:
- **Grid urządzeń:** Auto-fit z minmax
- **Kontrolki:** Standardowe rozmiary

#### Mobile:
- **Single column:** Wszystkie urządzenia pionowo
- **Pełna szerokość:** Wszystkie kontrolki na pełną szerokość
- **Zmniejszone paddingi:** Mniejsze odstępy

---

### 7. 🔐 Panel Logowania

#### Desktop:
- **Szerokość:** 300px (stała)
- **Padding:** 40px

#### Mobile:
- **Szerokość:** 90% z max-width 300px
- **Padding:** 30px 20px (mniejszy)
- **Responsywność:** Automatyczne dopasowanie do ekranu

**Implementacja:**
```css
/* Desktop */
.login-box {
  width: 300px;
  padding: 40px;
}

/* Mobile */
@media (max-width: 768px) {
  .login-box {
    width: 90%;
    max-width: 300px;
    padding: 30px 20px;
  }
}
```

---

### 8. 📄 Content Area (Główna Treść)

#### Desktop:
- **Margin-left:** 250px (miejsce na menu)
- **Padding:** 20px

#### Mobile:
- **Margin-left:** 0 (brak menu bocznego)
- **Padding:** 15px (mniejszy)

**Implementacja:**
```css
/* Desktop */
.content {
  margin-left: 250px; /* Miejsce na menu */
  padding: 20px;
}

/* Mobile */
@media (max-width: 768px) {
  .content {
    margin-left: 0; /* Pełna szerokość */
    padding: 15px; /* Mniejszy padding */
  }
}
```

---

### 📋 Podsumowanie Zmian dla Mobile

| Element | Desktop | Mobile |
|---------|---------|--------|
| **Menu boczne** | Zawsze widoczne (250px) | Ukryte, slide-in (280px) |
| **Header** | `left: 250px` | `left: 0` (pełna szerokość) |
| **Hamburger menu** | Ukryte | Widoczne |
| **Content margin** | `margin-left: 250px` | `margin-left: 0` |
| **Grid layouts** | Multi-column | Single column |
| **Tabele** | Pełna szerokość | Horizontal scroll |
| **Przyciski** | Standardowe | Pełna szerokość |
| **Fonty** | Standardowe | Zmniejszone o 1-2px |
| **Padding** | 20px | 15px |
| **Gap** | 20px | 15px |

---

### 🎯 Kluczowe Techniki Responsywności

1. **CSS Media Queries**
   - Wszystkie komponenty mają `@media (max-width: 768px)`
   - Breakpoint: 768px (standardowy dla mobile/tablet)

2. **Flexbox i Grid**
   - Desktop: Multi-column layouts
   - Mobile: Single column (`flex-direction: column`, `grid-template-columns: 1fr`)

3. **Transform dla Animacji**
   - Menu boczne: `transform: translateX()` zamiast `display: none`
   - Płynne animacje z `transition: transform 0.3s ease`

4. **Overlay Pattern**
   - Ciemne tło przy otwartym menu na mobile
   - Zamykanie menu po kliknięciu w overlay

5. **Conditional Rendering w JavaScript**
   - Automatyczne zamykanie menu po kliknięciu w element
   - Sprawdzanie szerokości ekranu: `window.innerWidth <= 768`

6. **Horizontal Scroll dla Tabel**
   - `overflow-x: scroll` z `min-width` dla czytelności
   - Zachowanie struktury tabeli na mobile

7. **Progressive Enhancement**
   - Desktop jako podstawowa wersja
   - Mobile jako ulepszenie z dodatkowymi funkcjami (hamburger menu)

---

### ✅ Zalety Takiego Podejścia

- ✅ **Zachowana funkcjonalność** - wszystkie funkcje dostępne na mobile
- ✅ **Płynne animacje** - transform zamiast show/hide
- ✅ **Czytelność** - odpowiednie rozmiary fontów i paddingów
- ✅ **Użyteczność** - pełna szerokość przycisków na mobile
- ✅ **Wydajność** - CSS-only rozwiązania (bez JavaScript dla layoutu)
- ✅ **Łatwość utrzymania** - każdy komponent ma własne style mobilne

---

## 💾 Zarządzanie Stanem

### localStorage

**Przechowywane dane:**
- `user_role` - rola użytkownika (string)
- `user_data` - dane użytkownika (JSON):
  ```json
  {
    "userId": 1,
    "email": "admin@szebi.com",
    "imie": "Jan",
    "nazwisko": "Kowalski",
    "role": "admin"
  }
  ```

**Zastosowanie:**
- Trwałe przechowywanie sesji użytkownika
- Automatyczne logowanie po odświeżeniu strony
- Przekazywanie danych między komponentami

### React Hooks

**Używane hooki:**
- `useState` - lokalny stan komponentów
- `useEffect` - efekty uboczne (odświeżanie danych, subskrypcje)
- `useRef` - referencje do wartości (unikanie problemów z zależnościami)
- `useCallback` - memoizacja funkcji

**Przykład:**
```javascript
const [alerts, setAlerts] = useState([]);

useEffect(() => {
  loadAlerts();
  const interval = setInterval(loadAlerts, 30000);
  return () => clearInterval(interval);
}, []);
```

---

## 📚 Używane Biblioteki

| Biblioteka | Wersja | Zastosowanie |
|------------|--------|--------------|
| **react** | 19.2.0 | Framework UI |
| **react-dom** | 19.2.0 | Renderowanie React |
| **recharts** | 3.5.1 | Wykresy i wizualizacje |
| **vite** | 7.2.4 | Build tool i dev server |
| **concurrently** | 9.1.0 | Uruchamianie wielu procesów |
| **eslint** | 9.39.1 | Linting kodu |

### Dev Dependencies

- `@vitejs/plugin-react` - Plugin React dla Vite
- `@types/react` - Typy TypeScript dla React
- `eslint-plugin-react-hooks` - Reguły ESLint dla React hooks

---

## ⚠️ Uwagi Implementacyjne

### 1. Dane Mockowe

Niektóre komponenty używają obecnie danych mockowych (oznaczone jako TODO w kodzie):

- ✅ **Dashboard.jsx** - mock dane energii (TODO: endpoint API)
- ✅ **ControlPanel.jsx** - mock urządzenia (TODO: endpoint API)
- ✅ **Reports.jsx** - mock raporty (TODO: endpoint API)

### 2. Automatyczne Wykrywanie API

- Frontend automatycznie wykrywa hostname przeglądarki
- Używa portu **8080** dla API backendu
- Działa zarówno na `localhost` jak i w sieci lokalnej (np. `192.168.1.100`)

**Implementacja:**
```javascript
const getApiBaseUrl = () => {
  const hostname = window.location.hostname;
  const protocol = window.location.protocol;
  return `${protocol}//${hostname}:8080/api`;
};
```

### 3. Optymalizacja Wydajności

**Agregacja danych:**
- Gdy historia odczytów > 50 punktów, dane są agregowane co 15-30 minut
- Zmniejsza liczbę punktów na wykresie, poprawiając wydajność

**Równoległe odczyty:**
- Wartości urządzeń odczytywane równolegle (Promise.all)
- Szybsze ładowanie danych

**Użycie useRef:**
- Unikanie problemów z zależnościami w useEffect
- Przechowywanie aktualnych wartości bez powodowania re-renderów

### 4. Interwały Odświeżania

| Komponent | Interwał | Opis |
|-----------|----------|------|
| Dashboard | 60 sekund | Odświeżanie danych energii |
| AlertsCenter | 30 sekund | Odświeżanie listy alarmów |
| Monitoring (lista) | 30 sekund | Odświeżanie listy urządzeń |
| Monitoring (wartości) | 15 sekund | Odświeżanie wartości urządzeń |
| Monitoring (wybrane) | 10 sekund | Odświeżanie wybranego urządzenia |

### 5. Formatowanie Danych

**Temperatura:** `XX.X°C`
**Procenty:** `XX%`
**Energia:** `XX.XX kWh` lub `XXXX W`
**Data/Czas:** Format polski (`pl-PL`)

---

## 🚀 Uruchomienie

### Wymagania

- Node.js 18+ (zawiera npm)
- Backend uruchomiony na porcie 8080

### Instalacja

```bash
cd modules/gui/web
npm install
```

### Uruchomienie Development

```bash
npm run dev
```

Aplikacja będzie dostępna na: `http://localhost:5173`

### Uruchomienie z Backendem

```bash
npm run dev:all
```

Uruchamia zarówno frontend jak i backend jednocześnie.

---

## 📝 Podsumowanie

GUI Systemu SZEBI to nowoczesna aplikacja React z pełnym systemem autoryzacji, różnymi poziomami dostępu i kompleksowymi funkcjami monitoringu i zarządzania energią. Aplikacja jest responsywna, zoptymalizowana pod kątem wydajności i gotowa do użycia z podstawowymi funkcjami zaimplementowanymi i połączonymi z backendem.

**Status implementacji:**
- ✅ Logowanie i autoryzacja
- ✅ Dashboard z wykresami
- ✅ Centrum alarmów z pełną funkcjonalnością
- ✅ Monitoring urządzeń z historią
- ⚠️ Panel sterowania (częściowo - mock data)
- ⚠️ Generator raportów (częściowo - mock data)
- ⚠️ Zarządzanie urządzeniami (do implementacji)
- ⚠️ Zarządzanie użytkownikami (do implementacji)

---

**Ostatnia aktualizacja:** 2025-01-05
