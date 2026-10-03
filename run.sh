#!/bin/bash
set -e

# Load Homebrew Java 17 and Maven if not already loaded
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"

# Ensure MySQL service is running
if ! mysqladmin ping -u root -p1234 >/dev/null 2>&1; then
    echo "Starting MySQL service via Homebrew..."
    brew services start mysql
    sleep 2
fi

# Automatically free port 8080 if occupied by old process
OLD_PID=$(lsof -ti :8080 2>/dev/null || true)
if [ -n "$OLD_PID" ]; then
    echo "Releasing port 8080 (PID: $OLD_PID)..."
    kill -9 $OLD_PID 2>/dev/null || true
    sleep 1
fi

echo "=========================================================="
echo " Starting SWP391 Cafe Management System (Tomcat 10.1)..."
echo " Access URL: http://localhost:8080/SWP391_CafeManagementSystem/"
echo " Press Ctrl + C to stop the server."
echo "=========================================================="

mvn package cargo:run
