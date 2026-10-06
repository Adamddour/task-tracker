# Task Tracker API

Eine REST-API zur Verwaltung von Aufgaben mit Java und Spring Boot. Aufgaben lassen sich anlegen, auflisten, als erledigt markieren und löschen. Die Speicherung erfolgt über Spring Data JPA in einer lokalen H2-Dateidatenbank, sodass die Daten nach einem Neustart erhalten bleiben.

Das Lernprojekt verbindet HTTP-Endpunkte, Eingabeprüfung, relationale Datenspeicherung und automatisierte Tests. Es besitzt keine grafische Benutzeroberfläche; die Bedienung erfolgt beispielsweise über PowerShell.

## Funktionen

- Aufgaben mit Titel und optionaler Beschreibung anlegen.
- Alle Aufgaben nach aufsteigender ID auflisten.
- Aufgaben als erledigt markieren.
- Aufgaben anhand ihrer ID löschen.
- Fehlende oder leere Titel mit HTTP 400 ablehnen; Leerzeichen am Anfang und Ende gültiger Titel entfernen.
- Unbekannte IDs beim Abschließen und Löschen mit HTTP 404 beantworten.
- Aufgaben und Erledigt-Status dauerhaft speichern.

## Technologien und Voraussetzungen

| Bestandteil | Aktueller Stand |
| --- | --- |
| Java | JDK 27, entsprechend `pom.xml` |
| Spring Boot | `4.2.0-SNAPSHOT` |
| Web | Spring Web MVC |
| Datenzugriff | Spring Data JPA und Hibernate |
| Datenbank | H2 im Dateimodus |
| Build | Maven über den mitgelieferten Maven Wrapper |
| Tests | JUnit Jupiter, Spring Boot Test und MockMvc |

Benötigt werden ein JDK 27 und eine Internetverbindung zum Herunterladen der Abhängigkeiten. Maven und ein separater Datenbankserver müssen nicht zusätzlich installiert werden.

**Versionshinweis:** Das Projekt verwendet derzeit eine Spring-Boot-Entwicklungsversion. Snapshot-Abhängigkeiten können sich ändern. Die Umstellung auf eine stabile Version ist noch offen.

## Lokal starten

Die folgenden Befehle werden in PowerShell im Projektordner ausgeführt, in dem die `pom.xml` liegt.

Prüfe zunächst die Java-Konfiguration:

```powershell
java -version
$env:JAVA_HOME
```

`JAVA_HOME` muss auf den JDK-Ordner zeigen, nicht auf dessen `bin`-Unterordner. Falls die Variable fehlt, kann sie für das aktuelle Terminal gesetzt werden. Passe den Beispielpfad bei Bedarf an:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-27"
```

Starte die Anwendung:

```powershell
.\mvnw.cmd spring-boot:run
```

Nach der Meldung `Started TaskTrackerApplication` ist die API standardmäßig unter `http://localhost:8080/tasks` erreichbar. Das Terminal bleibt während des Betriebs geöffnet. Mit `Strg+C` wird die Anwendung beendet.

Alternativ in IntelliJ IDEA:

1. Den Projektordner mit der `pom.xml` öffnen.
2. JDK 27 als Projekt-SDK auswählen.
3. Im Maven-Fenster **Sync Project** ausführen und das Laden der Abhängigkeiten abwarten.
4. Die `main`-Methode in `TaskTrackerApplication` starten.

Nur eine Instanz gleichzeitig mit derselben lokalen Datenbank starten. Die folgenden API-Beispiele können in einem zweiten PowerShell-Terminal ausgeführt werden.

## API

| Methode | Pfad | Wirkung | Erfolg |
| --- | --- | --- | --- |
| GET | `/tasks` | Alle Aufgaben auflisten | 200, JSON-Liste |
| POST | `/tasks` | Aufgabe anlegen | 201, angelegte Aufgabe |
| PATCH | `/tasks/{id}/complete` | Aufgabe als erledigt markieren | 200, aktualisierte Aufgabe |
| DELETE | `/tasks/{id}` | Aufgabe löschen | 204, kein Antwortinhalt |

Eine Aufgabe wird beispielsweise so dargestellt:

```json
{
  "id": 1,
  "title": "Bewerbung vorbereiten",
  "description": "Lebenslauf aktualisieren",
  "completed": false
}
```

Die Datenbank vergibt die ID. Neue Aufgaben sind zunächst unerledigt. Wiederholtes Abschließen lässt `completed` auf `true`. Nach dem Löschen liefert ein erneuter Löschversuch für dieselbe ID HTTP 404. Der genaue Fehlertext im Antwortinhalt ist nicht als festes API-Format definiert.

### Aufgabe anlegen

```powershell
$body = @{
    title = "Bewerbung vorbereiten"
    description = "Lebenslauf aktualisieren und Java-Projekt verlinken"
} | ConvertTo-Json

$task = Invoke-RestMethod `
    -Uri "http://localhost:8080/tasks" `
    -Method Post `
    -ContentType "application/json; charset=utf-8" `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($body))

$task
```

Die Antwort wird in `$task` gespeichert. Die folgenden Beispiele verwenden die tatsächlich zurückgegebene ID und werden im selben Terminal ausgeführt.

### Aufgaben auflisten

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/tasks" -Method Get
```

Die Liste lässt sich auch im Browser unter [localhost:8080/tasks](http://localhost:8080/tasks) ansehen. Ohne Aufgaben lautet die Antwort `[]`.

### Aufgabe abschließen

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/tasks/$($task.id)/complete" `
    -Method Patch
```

Die zurückgegebene Aufgabe enthält anschließend `completed: true`.

### Aufgabe löschen

```powershell
$response = Invoke-WebRequest `
    -Uri "http://localhost:8080/tasks/$($task.id)" `
    -Method Delete `
    -UseBasicParsing

$response.StatusCode
```

Erwarteter Status: `204`. Die Aufgabe ist anschließend nicht mehr in der Liste enthalten.

## Datenspeicherung

Die Verbindung wird in `src/main/resources/application.properties` konfiguriert:

```properties
spring.datasource.url=jdbc:h2:file:./data/tasktracker;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
```

Der Pfad `./data/` ist relativ zum Arbeitsverzeichnis der Anwendung. Beim Start aus dem Projektordner liegt dort unter anderem die Datei `tasktracker.mv.db`. Für dieselben Daten muss die Anwendung bei jedem Start dasselbe Arbeitsverzeichnis verwenden. Der Ordner `data/` wird durch `.gitignore` von der Versionsverwaltung ausgeschlossen.

Hibernate erstellt beziehungsweise aktualisiert die Tabelle `tasks` anhand der Entity. `ddl-auto=update` dient hier der lokalen Entwicklung; versionierte Datenbankmigrationen sind noch nicht eingerichtet.

Zum manuellen Prüfen der dauerhaften Speicherung:

1. Eine neue Aufgabe anlegen und ihre ID notieren.
2. Die Anwendung stoppen und aus demselben Arbeitsverzeichnis erneut starten.
3. `GET /tasks` aufrufen. Die Aufgabe muss weiterhin vorhanden sein.

## Aufbau

Die Java-Dateien liegen unter `src/main/java/de/adamkaddour/task_tracker/`.

| Datei | Aufgabe |
| --- | --- |
| `TaskTrackerApplication.java` | Einstiegspunkt der Spring-Boot-Anwendung |
| `TaskController.java` | HTTP-Endpunkte, Titelprüfung und Umwandlung der Antworten |
| `CreateTaskRequest.java` | Record für den JSON-Inhalt beim Anlegen |
| `Task.java` | Record für die JSON-Antwort einer Aufgabe |
| `TaskEntity.java` | JPA-Entity für die Tabelle `tasks` mit automatisch erzeugter ID |
| `TaskRepository.java` | Datenzugriff über `JpaRepository<TaskEntity, Long>` |

Der Controller erhält das Repository über seinen Konstruktor. Spring stellt die Implementierung des Repository-Interfaces bereit. Die Entity beschreibt die Speicherung; der separate `Task`-Record hält das Antwortformat der API unabhängig davon.

Abschließen und Löschen laufen innerhalb einer Transaktion. Beim Abschließen erkennt JPA die Änderung an der geladenen Entity und speichert sie beim erfolgreichen Abschluss der Transaktion.

## Tests und Build

Alle Tests ausführen:

```powershell
.\mvnw.cmd test
```

Die Tests liegen unter `src/test/java/de/adamkaddour/task_tracker/`:

- `TaskTrackerApplicationTests` prüft den Start des Spring-Kontexts mit einer separaten In-Memory-Datenbank.
- `TaskPersistenceTests` prüft die Endpunkte über MockMvc mit einem echten JPA-Repository und einer temporären H2-Dateidatenbank. Mehrere nacheinander geöffnete und geschlossene Anwendungskontexte prüfen, ob Aufgaben, ihr Erledigt-Status und Löschungen dauerhaft gespeichert werden. Außerdem werden Titelprüfung, unbekannte IDs und der Erhalt anderer Aufgaben beim Löschen geprüft.

Die Tests verwenden nicht die persönliche Datenbank unter `data/`. MockMvc prüft die Verarbeitung der Anfragen ohne einen echten HTTP-Server; der Neustarttest öffnet mehrere Anwendungskontexte innerhalb desselben Testprozesses.

Ein ausführbares JAR inklusive Tests bauen:

```powershell
.\mvnw.cmd package
```

Anschließend aus dem Projektordner starten:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar target/task-tracker-0.0.1-SNAPSHOT.jar
```

## Häufige Startprobleme

- **`JAVA_HOME` fehlt:** Im Terminal den JDK-Pfad wie oben setzen. Mit `.\mvnw.cmd -v` lässt sich die von Maven verwendete Java-Version prüfen.
- **Spring-Pakete werden in IntelliJ nicht gefunden:** Die Web-Abhängigkeit in der `pom.xml` prüfen und im Maven-Fenster **Sync Project** ausführen. Mit `.\mvnw.cmd compile` lässt sich der Build unabhängig von IntelliJ prüfen.
- **Port 8080 ist belegt:** Eine bereits laufende Instanz stoppen, bevor eine weitere gestartet wird.
- **Die Datenbank ist gesperrt:** Prüfen, ob eine andere Instanz dieselbe H2-Datei geöffnet hat.

## Umfang und nächste Schritte

Dies ist ein lokal betriebenes Lernprojekt ohne Anmeldung, Benutzertrennung oder grafische Oberfläche. Titel und Beschreibung vorhandener Aufgaben lassen sich bisher nicht bearbeiten; erledigte Aufgaben können nicht wieder geöffnet werden. Die lokale Datenbankkonfiguration ist für Entwicklung gedacht.

Mögliche nächste Schritte sind die Umstellung auf eine stabile Spring-Boot-Version, das Bearbeiten von Aufgaben und ein Wechsel zu PostgreSQL mit versionierten Datenbankmigrationen.
