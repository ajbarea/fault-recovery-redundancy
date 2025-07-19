#!/bin/bash

# User Reset Script for Fault Recovery Redundancy System
# This script wipes all users from the backend database

set -e  # Exit on any error

# Configuration
BASE_URL="http://localhost:8080"
SIMULATION_URL="${BASE_URL}/simulation"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}🧹 Fault Recovery User Reset Script${NC}"
echo "=================================================="

# Function to check if the service is running
check_service() {
    echo -e "${YELLOW}📡 Checking if service is running...${NC}"
    
    if curl -s -f "${BASE_URL}/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Service is running${NC}"
        return 0
    else
        echo -e "${RED}❌ Service is not running or not accessible at ${BASE_URL}${NC}"
        echo "Please ensure the application is running with:"
        echo "  cd app && mvn spring-boot:run"
        echo "Or using Docker:"
        echo "  docker compose up -d"
        return 1
    fi
}

# Function to delete all users
delete_users() {
    echo -e "${YELLOW}🗑️  Deleting all users...${NC}"
    
    response=$(curl -s -w "%{http_code}" -X DELETE "${SIMULATION_URL}/users" -o /tmp/user_reset_response.json)
    http_code="${response: -3}"
    
    if [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✅ Successfully deleted all users${NC}"
        if [ -f /tmp/user_reset_response.json ]; then
            cat /tmp/user_reset_response.json | grep -o '"message":"[^"]*"' | sed 's/"message":"\([^"]*\)"/\1/' 2>/dev/null || echo "All users have been deleted."
        fi
        rm -f /tmp/user_reset_response.json
        return 0
    else
        echo -e "${RED}❌ Failed to delete users (HTTP $http_code)${NC}"
        if [ -f /tmp/user_reset_response.json ]; then
            echo "Response:"
            cat /tmp/user_reset_response.json
        fi
        rm -f /tmp/user_reset_response.json
        return 1
    fi
}

# Function to verify deletion
verify_deletion() {
    echo -e "${YELLOW}🔍 Verifying user deletion...${NC}"
    
    # Try to register a test user to verify the database is clean
    test_data='{"username":"test_verification","password":"testpass123","confirmPassword":"testpass123","email":"test@verification.com"}'
    
    response=$(curl -s -w "%{http_code}" -X POST "${BASE_URL}/api/auth/register" \
        -H "Content-Type: application/json" \
        -d "$test_data" \
        -o /tmp/verification_response.json)
    http_code="${response: -3}"
    
    if [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✅ Verification successful - database is clean${NC}"
        
        # Clean up the test user
        curl -s -X DELETE "${SIMULATION_URL}/users" > /dev/null 2>&1
        rm -f /tmp/verification_response.json
        return 0
    else
        echo -e "${YELLOW}⚠️  Could not verify deletion (HTTP $http_code)${NC}"
        if [ -f /tmp/verification_response.json ]; then
            cat /tmp/verification_response.json
        fi
        rm -f /tmp/verification_response.json
        return 1
    fi
}

# Main execution
main() {
    echo
    
    # Check if service is running
    if ! check_service; then
        exit 1
    fi
    
    echo
    
    # Confirm deletion
    echo -e "${YELLOW}⚠️  This will delete ALL users from the database!${NC}"
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}🚫 Operation cancelled${NC}"
        exit 0
    fi
    
    echo
    
    # Delete users
    if delete_users; then
        echo
        verify_deletion
        echo
        echo -e "${GREEN}🎉 User reset completed successfully!${NC}"
        echo "You can now register new users without conflicts."
    else
        echo
        echo -e "${RED}💥 User reset failed!${NC}"
        exit 1
    fi
}

# Run the script
main "$@"
