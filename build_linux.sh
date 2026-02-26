#!/bin/bash

echo "================================"
echo "Compiling LanPortScanner..."
echo "================================"
echo ""

# Compile with UTF-8 encoding
javac -encoding UTF-8 LanPortScanner.java

if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Compilation failed!"
    exit 1
fi

# Create JAR file
echo "Creating JAR file..."
jar cfe LanPortScanner.jar LanPortScanner LanPortScanner.class

if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Failed to create JAR!"
    exit 1
fi

echo ""
echo "================================"
echo "Build Success!"
echo "================================"
echo "JAR file created: LanPortScanner.jar"
echo ""
echo "Usage examples:"
echo "  java -jar LanPortScanner.jar tcp 192.168.1.1 192.168.1.254 80"
echo "  java -jar LanPortScanner.jar http 192.168.10.1 192.168.10.254 8080 3"
echo "================================"
echo ""
