$projectFile = Join-Path $PSScriptRoot '..\wastina_trust\pom.xml'
& mvn -f $projectFile spring-boot:run '-Dspring-boot.run.profiles=local'
exit $LASTEXITCODE
