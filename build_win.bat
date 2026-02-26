@echo off
echo Compiling LanPortScanner...
echo.

javac -encoding UTF-8 LanPortScanner.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Creating JAR file...
jar cfe LanPortScanner.jar LanPortScanner LanPortScanner.class

if %errorlevel% neq 0 (
    echo Failed to create JAR!
    pause
    exit /b 1
)

echo.
echo ================================
echo Build Success!
echo ================================
echo JAR file created: LanPortScanner.jar
echo.
echo Usage:
echo   java -jar LanPortScanner.jar tcp 192.168.1.1 192.168.1.254 80
echo   java -jar LanPortScanner.jar http 192.168.10.1 192.168.10.254 8080 3
echo ================================
echo.

pause
