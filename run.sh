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

echo "=========================================================="
echo " Starting SWP391 Cafe Management System (Tomcat 10.1)..."
echo " Access URL: http://localhost:8080/SWP391_CafeManagementSystem/"
echo " Press Ctrl + C to stop the server."
echo "=========================================================="

mvn package cargo:run
