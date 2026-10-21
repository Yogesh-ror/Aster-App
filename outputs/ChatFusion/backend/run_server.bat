@echo off
set "ENV_FILE=C:\Users\yoges\Documents\Codex\2026-10-07\as-you-are-a-proffesional-software\outputs\ChatFusion\backend\.env"

if exist "%ENV_FILE%" (
    for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
        if not "%%A"=="" if not "%%B"=="" set "%%A=%%B"
    )
)

set "JAVA_HOME=C:\Users\yoges\.jdks\jbr-21.0.11"
"C:\Users\yoges\.jdks\jbr-21.0.11\bin\java.exe" -cp "C:\Users\yoges\Documents\Codex\2026-10-07\as-you-are-a-proffesional-software\outputs\ChatFusion\backend\build\install\backend\lib\*" com.yogesh.chatfusion.ApplicationKt
