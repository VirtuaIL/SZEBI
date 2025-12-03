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

```bash`
docker-compose up -d


Struktura i Rozwój Projektu
Projekt jest zorganizowany w architekturze wielomodułowej. Poniższa instrukcja opisuje, jak poprawnie dodać nowy, niezależny moduł do systemu.
1. Stwórz Nowy Moduł Mavena
W panelu projektu IntelliJ, kliknij prawym przyciskiem myszy na folder /modules.
Wybierz New -> Module.
W oknie "New Module" skonfiguruj swój moduł:
Name: Wpisz nazwę modułu (np. new-feature-module).
Location: Upewnij się, że ścieżka prowadzi do .../modules/new-feature-module.
Language: Java
Build system: Maven
JDK: Wybierz wersję 21 (lub aktualną wersję projektu).
Advanced Settings:
GroupId: org.example (użyj tego samego groupId, co w innych modułach).
ArtifactId: new-feature-module.
Kliknij "Create".
2. Dodaj Zależności w pom.xml
Twój nowy moduł będzie prawdopodobnie potrzebował dostępu do interfejsów i klas DTO z modułu bazy danych.
Otwórz plik pom.xml w folderze swojego nowego modułu.
Dodaj blok <dependencies> z zależnością do database-api:
code
Xml
<dependencies>
    <!-- Zależność do modułu z interfejsami bazy danych -->
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>database-api</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- Dodaj tutaj inne potrzebne zależności -->
</dependencies>
3. Odśwież Projekt Maven
Po zapisaniu pliku pom.xml, kliknij ikonę odświeżania ("Reload All Maven Projects") w panelu Maven.
4. Zintegruj Moduł z Aplikacją Główną
Aby przetestować współpracę Twojego nowego modułu z resztą systemu, musisz dodać go jako zależność do modułu application-runner.
Otwórz plik pom.xml w module /modules/application-runner/.
W jego bloku <dependencies>, dodaj nową zależność do swojego modułu:
code
Xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>new-feature-module</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
Po odświeżeniu Mavena, w klasie Main.java w module application-runner możesz tworzyć instancje klas ze swojego nowego modułu i integrować je z resztą aplikacji.
