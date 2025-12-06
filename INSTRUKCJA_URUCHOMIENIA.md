# 🚀 Instrukcja Uruchomienia Aplikacji SZEBI

Prosty poradnik krok po kroku, jak uruchomić aplikację SZEBI (backend + frontend).

## 📋 Wymagania

Przed uruchomieniem upewnij się, że masz zainstalowane:

- ✅ **Java JDK 25** (lub nowszy)
- ✅ **Apache Maven** (dostępny w PATH)
- ✅ **Node.js** (wersja 18 lub nowsza) - zawiera npm
- ✅ **Docker Desktop** (dla baz danych)

## 🔧 Krok 1: Uruchomienie Baz Danych (Docker)

Otwórz terminal w głównym katalogu projektu i uruchom:

```bash
docker-compose up -d
```

To uruchomi kontenery PostgreSQL i MongoDB w tle.

**Sprawdź czy działa:**
```bash
docker ps
```
Powinieneś zobaczyć działające kontenery `postgres` i `mongo`.

---

## 🔨 Krok 2: Zbuduj Projekt Maven (tylko pierwszy raz lub po zmianach)

Z głównego katalogu projektu:

```bash
mvn clean install -DskipTests
```

To zbuduje wszystkie moduły Java i zainstaluje je w lokalnym repozytorium Maven.

**Czas:** ~30-60 sekund

**Kiedy powtarzać:**
- Pierwszy raz
- Po zmianach w kodzie Java
- Po dodaniu nowych zależności Maven

---

## 📦 Krok 3: Zainstaluj Zależności Frontendu (tylko pierwszy raz)

Przejdź do katalogu frontendu:

```bash
cd modules/gui/web
npm install
```

To zainstaluje wszystkie pakiety npm potrzebne dla React/Vite, w tym `concurrently` (używane do uruchamiania obu serwisów jednocześnie).

**Czas:** ~1-2 minuty

**Kiedy powtarzać:**
- Pierwszy raz (OBOWIĄZKOWE!)
- Po zmianach w `package.json`
- Po aktualizacji repozytorium (jeśli ktoś dodał nowe zależności)
- Jeśli widzisz błąd `'concurrently' is not recognized` - uruchom `npm install` ponownie

---

## 🎯 Krok 4: Uruchom Aplikację

Masz **dwie opcje** - uruchomienie w **dwóch osobnych terminalach** lub **w jednym terminalu**.

### Opcja A: Dwa Osobne Terminale (Zalecane dla początkujących)

#### Terminal 1 - Backend:
```bash
cd modules/application-runner
mvn exec:java
```

#### Terminal 2 - Frontend:
```bash
cd modules/gui/web
npm run dev
```

### Opcja B: Jeden Terminal (Dla zaawansowanych)

**WAŻNE:** Przed użyciem tej opcji upewnij się, że zainstalowałeś wszystkie zależności npm (patrz Krok 3).

Z katalogu `modules/gui/web`:

```bash
npm run dev:all
```

To uruchomi oba serwisy jednocześnie w jednym oknie (wymaga zainstalowanego `concurrently`).

**Jeśli widzisz błąd: `'concurrently' is not recognized`:**
```bash
# Z katalogu modules/gui/web
npm install
```

To zainstaluje wszystkie zależności, w tym `concurrently`.

---

## ✅ Sprawdź Czy Wszystko Działa

Po uruchomieniu powinieneś zobaczyć:

### Backend:
- ✅ `[INFO] REST API uruchomione na porcie 8080`
- ✅ `[INFO] Endpoint logowania: http://localhost:8080/api/login`

**Backend dostępny na:** http://localhost:8080

### Frontend:
- ✅ `Local:   http://localhost:5173/`
- ✅ `Network: http://192.168.x.x:5173/`

**Frontend dostępny na:** http://localhost:5173

---

## 🛑 Jak Zatrzymać

### Terminal z Backendem:
Naciśnij `Ctrl+C`

### Terminal z Frontendem:
Naciśnij `Ctrl+C`

### Jeśli używasz `npm run dev:all`:
Naciśnij `Ctrl+C` raz - zatrzyma oba serwisy.

---

## 🐛 Rozwiązywanie Problemów

### Problem: "Maven nie jest rozpoznawany jako polecenie"
**Rozwiązanie:** 
- Sprawdź czy Maven jest w PATH
- Uruchom: `mvn --version` - powinno pokazać wersję
- Jeśli nie działa, dodaj Maven do zmiennej środowiskowej PATH

### Problem: "npm nie jest rozpoznawany jako polecenie"
**Rozwiązanie:**
- Sprawdź czy Node.js jest zainstalowany: `node --version`
- Zainstaluj Node.js z https://nodejs.org/
- Po instalacji zrestartuj terminal

### Problem: "Could not find artifact org.example:analysis-report"
**Rozwiązanie:**
- Uruchom ponownie: `mvn clean install -DskipTests` z głównego katalogu

### Problem: "Port 8080 już w użyciu"
**Rozwiązanie:**
- Zatrzymaj inne aplikacje używające portu 8080
- Lub zmień port w `Main.java` (linia 107)

### Problem: "Port 5173 już w użyciu"
**Rozwiązanie:**
- Vite automatycznie użyje następnego dostępnego portu (5174, 5175, itd.)
- Sprawdź w terminalu jaki port został użyty

### Problem: "Brak połączenia z serwerem" w przeglądarce
**Rozwiązanie:**
- Upewnij się, że backend jest uruchomiony (sprawdź terminal)
- Sprawdź czy backend działa: otwórz http://localhost:8080/api/login w przeglądarce
- Sprawdź czy nie ma błędów w terminalu backendu

### Problem: "'concurrently' is not recognized"
**Rozwiązanie:**
- Zainstaluj zależności npm: `cd modules/gui/web && npm install`
- To zainstaluje `concurrently` i wszystkie inne wymagane pakiety
- Po instalacji spróbuj ponownie: `npm run dev:all`

---

## 📝 Szybka Ścieżka (Po Pierwszej Instalacji)

Jeśli już wszystko masz zainstalowane i zbudowane:

1. **Uruchom Docker:**
   ```bash
   docker-compose up -d
   ```

2. **Uruchom Backend** (Terminal 1):
   ```bash
   cd modules/application-runner
   mvn exec:java
   ```

3. **Uruchom Frontend** (Terminal 2):
   ```bash
   cd modules/gui/web
   npm run dev
   ```

**Gotowe!** 🎉

---

## 💡 Wskazówki

- **Pierwszy raz:** Wykonaj wszystkie kroki po kolei
- **Kolejne uruchomienia:** Możesz pominąć budowanie Maven i instalację npm (jeśli nic się nie zmieniło)
- **Po zmianach w kodzie Java:** Zbuduj ponownie: `mvn clean install -DskipTests`
- **Po zmianach w kodzie React:** Frontend odświeży się automatycznie (Hot Module Replacement)
- **Zatrzymanie Docker:** `docker-compose down`

---

## 📞 Potrzebujesz Pomocy?

Jeśli masz problemy:
1. Sprawdź sekcję "Rozwiązywanie Problemów" powyżej
2. Sprawdź logi w terminalach (backend i frontend)
3. Sprawdź czy wszystkie wymagania są spełnione
4. Upewnij się, że Docker działa: `docker ps`

---

**Powodzenia! 🚀**

