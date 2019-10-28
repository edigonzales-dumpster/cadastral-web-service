[![Build Status](https://travis-ci.org/edigonzales/cadastral-info-service.svg?branch=master)](https://travis-ci.org/edigonzales/cadastral-info-service)

# cadastral-info-service

## Beschreibung
Proof-of-Concept für einen Service, der grundstücksbezogene Informationen aus der amtlichen Vermessung liefert. Das Konzept ist angelehnt an den ÖREB-Kataster Web Service.

Der Aufruf `http://localhost:8080/extract/${EGRID}` liefert einen XML-Auszug. Das XML wird durch ein [Schema](src/main/xsd/CadastralExtract.xsd) beschrieben.

Flughafen Grenchen:
```
http://localhost:8080/extract/CH870672603279
```


## Betriebsdokumentation
Bei jedem Git-Push wird mittels Travis das Docker-Image neu gebuildet und als sogis/cadastral-info-service mit den Tags `latest` und "Travis-Buildnummer" auf Docker Hub abgelegt. Auf der AGI-Testumgebung wird viertelstündlich das `latest`-Image deployed.

### Konfiguration
Die Datenbankverbindungsparameter (ohne Benutzer und Passwort) werden über Spring Boot Profile gesteuert. Für jede Umgebung gibt es eine application-[dev|test|int|prod].properties-Datei. Diese spezielle, zur "normalen" Properties-Datei zusätzliche Datei kann mit der speziellen Spring-Boot-Umgebungsvariable SPRING_PROFILES_ACTIVE=[dev|test|int|prod] gesteuert werden. Zum jetzigen Zeitpunkt werden diese Properties-Dateien in das Image gebrannt.

Es wird die ÖREB-Datenbank verwendet, da diese Schema- und Tabellenstruktur erwartet wird. 

**TODO**: Es fehlen verschiedene AV-Daten in der ÖREB-DB (in der AGI-GDI). -> Use Edit-DB instead.

Zusätzlich müssen sensible Konfigurationen über ENV-Variablen gesetzt werden:

- DBUSER
- DBPWD
- DBSCHEMA (optional)
- DBURL (optional)

### Persistenz
Es wird kein Volume o.ä. benötigt.

### Docker
```
docker run --restart always -p 8080:8080 \
-e "SPRING_PROFILES_ACTIVE=dev" \
-e "DBUSER=gretl" \
-e "DBPWD=gretl" \
sogis/cadastral-info-service
```

## Entwicklerdokumentation
Lokale Datenbank mit Daten (z.B. oereb-db-data):
```
docker run --rm --name oereb-db-data -p 54321:5432 --hostname primary \
-e PG_DATABASE=oereb -e PG_LOCALE=de_CH.UTF-8 -e PG_PRIMARY_PORT=5432 -e PG_MODE=primary \
-e PG_USER=admin -e PG_PASSWORD=admin \
-e PG_PRIMARY_USER=repl -e PG_PRIMARY_PASSWORD=repl \
-e PG_ROOT_PASSWORD=secret \
-e PG_WRITE_USER=gretl -e PG_WRITE_PASSWORD=gretl \
-e PG_READ_USER=ogc_server -e PG_READ_PASSWORD=ogc_server \
-e PGDATA=/tmp/primary \
sogis/oereb-db-data:latest
```

Falls das Docker-Image des Services zusammen mit der Datenbank verwendet wird, muss ein gemeinsames Netzwerk erstellt werden:

```
docker-compose up
```

