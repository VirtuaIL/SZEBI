# Projekt SZEBI

Repozytorium zawierające kod źródłowy oraz infrastrukturę dla systemu SZEBI.

## Wymagania Wstępne

- Docker Desktop
- JDK 21 lub nowszy
- Apache Maven
- IntelliJ IDEA

## Uruchomienie Środowiska Deweloperskiego

Proces uruchomienia całego środowiska składa się z dwóch głównych kroków.

### 1. Uruchomienie Infrastruktury (Bazy Danych)

Przejdź do głównego folderu projektu w terminalu i uruchom kontenery Docker w tle. Spowoduje to uruchomienie serwerów PostgreSQL i MongoDB oraz automatyczne załadowanie schematu i danych startowych.

```bash
docker-compose up -d
