#!/usr/bin/env bash
set -e

# Custom ASCII Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

# Check Maven
if ! command -v mvn >/dev/null 2>&1; then
    printf "%bError: Maven is not installed%b\n" "$RED" "$NC"
    exit 1
fi

# Check pom.xml exists
if [ ! -f "pom.xml" ]; then
    printf "%bError: pom.xml not found. Run this from the project root.%b\n" "$RED" "$NC"
    exit 1
fi

# Start application
printf "%bStarting application...%b\n" "$GREEN" "$NC"
mvn spring-boot:run
