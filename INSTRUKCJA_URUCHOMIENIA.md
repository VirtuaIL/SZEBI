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

---

## 🔨 Krok 2: Zbuduj Projekt Maven (tylko pierwszy raz lub po zmianach)

Z głównego katalogu projektu:

```bash
mvn clean install -DskipTests
```

To zbuduje wszystkie moduły Java i zainstaluje je w lokalnym repozytorium Maven.

---

## 📦 Krok 3: Zainstaluj Zależności Frontendu (tylko pierwszy raz)

Przejdź do katalogu frontendu:

```bash
cd modules/gui/web
npm install
```

To zainstaluje wszystkie pakiety npm potrzebne dla React/Vite, w tym `concurrently` (używane do uruchamiania obu serwisów jednocześnie).

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

Z katalogu `modules/gui/web`:

```bash
npm run dev:all
```

To uruchomi oba serwisy jednocześnie w jednym oknie (wymaga zainstalowanego `concurrently`).

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