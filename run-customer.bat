@echo off
echo ==========================================
echo    Aurelia Suites - Customer Portal
echo ==========================================
echo.

set JAVAFX_PATH=C:\Program Files\Java\javafx-sdk-25.0.1\lib

echo [1/2] Checking if project is compiled...
call mvn compile -q

echo [2/2] Launching Customer Portal...
echo.

call mvn exec:java -Dexec.mainClass="com.hotel.main.CustomerApp" -q

pause
