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
```
## Rozwój Projektu: Jak Dodać Nowy Moduł Backendowy

Projekt jest zorganizowany w architekturze wielomodułowej przy użyciu Mavena. Poniższa instrukcja opisuje, jak poprawnie dodać nowy, niezależny moduł (np. dla nowego członka zespołu).

### Krok 1: Stwórz Nowy Moduł Mavena

1.  Otwórz (`modules`) w IntelliJ IDEA.
2.  W panelu projektu, kliknij prawym przyciskiem myszy na folder **`/modules`**.
3.  Wybierz **New -> Module**.
4.  W oknie "New Module" skonfiguruj swój moduł:
    - **Name:** Wpisz nazwę swojego modułu, np. `new-feature-module`.
    - **Location:** Upewnij się, że ścieżka prowadzi do `.../SZEBI/modules/new-feature-module`.
    - **Language:** `Java`
    - **Build system:** `Maven`
    - **JDK:** Wybierz wersję `21` (lub aktualną wersję projektu).
    - **Advanced Settings:**
        - `GroupId`: `org.example` (użyj tego samego, co w innych modułach).
        - `ArtifactId`: `new-feature-module`.
5.  Kliknij **"Create"**.

### Krok 2: Dodaj Zależności w `pom.xml`

Twój nowy moduł będzie prawdopodobnie potrzebował dostępu do interfejsów i klas DTO z modułu bazy danych.

1.  Otwórz plik `pom.xml` w folderze swojego nowego modułu (np. `/modules/new-feature-module/pom.xml`).
2.  Dodaj blok `<dependencies>` z zależnością do `database-api`:

    ```xml
    <dependencies>
        <!-- Zależność do modułu z interfejsami bazy danych -->
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>database-api</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        
    </dependencies>
    ```

### Krok 3: Odśwież Projekt Maven

Po zapisaniu pliku `pom.xml`, kliknij ikonę odświeżania ("Reload All Maven Projects") w panelu Maven po prawej stronie IntelliJ.

### Krok 4: Zacznij Pisać Kod

Możesz teraz tworzyć nowe pakiety i klasy w folderze `/src/main/java/` swojego modułu. Będziesz miał pełen dostęp do wszystkich interfejsów (np. `IAlertData`) i klas DTO (np. `Alert`) z modułu `database-api`.

Pamiętaj o zasadzie **programowania do interfejsu** i wstrzykiwania zależności przez konstruktor.

### Krok 5: Zintegruj Moduł z Aplikacją Główną

Aby przetestować współpracę Twojego nowego modułu z resztą systemu, musisz dodać go jako zależność do modułu `application-runner`.

1.  Otwórz plik `pom.xml` w module `/modules/application-runner/`.
2.  W jego bloku `<dependencies>`, dodaj nową zależność do swojego modułu:

    ```xml
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>new-feature-module</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    ```

3.  Teraz w klasie `Main.java` w module `application-runner` możesz tworzyć instancje klas ze swojego nowego modułu i integrować je z resztą aplikacji.

Po odświeżeniu Mavena, w klasie Main.java w module application-runner możesz tworzyć instancje klas ze swojego nowego modułu i integrować je z resztą aplikacji.


WAŻNE!!
Jak nie wiesz jak używać interfejsów z bazki to patrz do modułu acquisition do pliku DataCollector, powinien być git
