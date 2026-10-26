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

# Handle --reset-db flag or auto-init if database is missing/empty
RESET_DB=false
SKIP_TESTS=false
for arg in "$@"; do
    case $arg in
        --reset-db|-r)
            RESET_DB=true
            ;;
        --skip-tests|-s)
            SKIP_TESTS=true
            ;;
    esac
done

DB_TABLE_COUNT=$(mysql -u root -p1234 -N -s -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='aura_cafe_erp';" 2>/dev/null || echo "0")

if [ "$RESET_DB" = true ]; then
    echo "🔄 Resetting and re-seeding database aura_cafe_erp from database/aura_cafe.sql..."
    mysql -u root -p1234 < database/aura_cafe.sql
    echo "✅ Database aura_cafe_erp re-seeded successfully!"
elif [ "$DB_TABLE_COUNT" -eq 0 ]; then
    echo "⚠️  Database aura_cafe_erp is missing or empty. Auto-initializing from database/aura_cafe.sql..."
    mysql -u root -p1234 < database/aura_cafe.sql
    echo "✅ Initialized database aura_cafe_erp successfully!"
fi

# Automatically free port 8080 if occupied by old process
OLD_PID=$(lsof -ti :8080 2>/dev/null || true)
if [ -n "$OLD_PID" ]; then
    echo "Releasing port 8080 (PID: $OLD_PID)..."
    kill -9 $OLD_PID 2>/dev/null || true
    sleep 1
fi

# Clean previous Cargo configuration directory to prevent ContainerException on restart
rm -rf target/cargo/configurations

MVN_OPTS=""
if [ "$SKIP_TESTS" = true ]; then
    MVN_OPTS="-DskipTests"
    echo "⚡ Fast start enabled: skipping unit tests"
fi

echo "=========================================================="
echo " Starting SWP391 Cafe Management System (Tomcat 10.1)..."
echo " Access URL: http://localhost:8080/SWP391_CafeManagementSystem/"
echo " Options:"
echo "   ./run.sh              -> Start normally"
echo "   ./run.sh --reset-db   -> Re-seed database from aura_cafe.sql"
echo "   ./run.sh --skip-tests -> Skip unit tests for faster startup"
echo " Press Ctrl + C to stop the server."
echo "=========================================================="

mvn package $MVN_OPTS cargo:run
