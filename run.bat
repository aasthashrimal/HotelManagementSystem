@echo off
echo ==========================================
echo    Aurelia Suites - Management System
echo ==========================================
echo.

set JAVAFX_PATH=C:\Program Files\Java\javafx-sdk-25.0.1\lib

echo [1/3] Compiling project with Maven...
call mvn clean package -q

if errorlevel 1 (
    echo.
    echo Build FAILED. Check errors above.
    pause
    exit /b 1
)

echo [2/3] Build successful!
echo [3/3] Launching Hotel Management System...
echo.

java --module-path "%JAVAFX_PATH%" ^
     --add-modules javafx.controls,javafx.fxml ^
     -jar target\HotelManagementSystem-1.0-SNAPSHOT.jar

pause
