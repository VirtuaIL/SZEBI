all: build run

build:
	cd modules && mvn clean compile package dependency:copy-dependencies -DoutputDirectory=target/libs

run:
	cd modules/application-runner && java -cp 'target/application-runner-1.0-SNAPSHOT.jar:target/libs/*' org.example.runner.Main

run-analysis:
	cd modules/analysis-report && java -cp 'target/analysis-report-1.0-SNAPSHOT.jar:target/libs/*' org.example.App

# Uruchomienie backendu i frontendu jednocześnie
dev:
	@echo "Uruchamianie backendu i frontendu..."
	@cd modules/gui/web && npm run dev:all

# Tylko backend
dev-backend:
	cd modules/application-runner && mvn exec:java

# Tylko frontend
dev-frontend:
	cd modules/gui/web && npm run dev

clean:
	cd modules && mvn clean

test:
	cd modules && mvn test

.PHONY: all build run run-analysis dev dev-backend dev-frontend clean test
