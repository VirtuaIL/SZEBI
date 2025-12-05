# Moduł Analizy i Raportowania - System SZEBI

# Uruchamianie projektu - dla Kopana

```bash
cd SZEBI/modules
mvn install
mvn package
mvn compile
mvn dependency:copy-dependencies -DoutputDirectory=target/libs

cd application-runner
java -cp 'target/application-runner-1.0-SNAPSHOT.jar:target/libs/*' org.example.runner.Main

cd analysis-report
java -cp target/analysis-report-1.0-SNAPSHOT.jar org.example.App
```
