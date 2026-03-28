@ECHO OFF
SETLOCAL EnableDelayedExpansion

SET BASE_DIR=%~dp0
SET WRAPPER_DIR=%BASE_DIR%\.mvn\wrapper
SET JAR=%WRAPPER_DIR%\maven-wrapper.jar
SET PROPS=%WRAPPER_DIR%\maven-wrapper.properties

IF NOT EXIST "%JAR%" (
  IF NOT EXIST "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
  IF NOT EXIST "%PROPS%" (
    > "%PROPS%" (
      ECHO distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip
      ECHO wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar
    )
  )
  FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%PROPS%") DO (
    IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '!WRAPPER_URL!' -OutFile '%JAR%'" || GOTO :FALLBACK
)

IF NOT EXIST "%JAR%" GOTO :FALLBACK
java -jar "%JAR%" -Dmaven.multiModuleProjectDirectory="%BASE_DIR%" %*
EXIT /B %ERRORLEVEL%

:FALLBACK
REM Maven Wrapper download can fail in restricted environments; fall back to system Maven if available.
mvn %*
EXIT /B %ERRORLEVEL%
