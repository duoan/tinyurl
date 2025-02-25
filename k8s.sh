#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to check if command was successful
check_status() {
    if [ $? -eq 0 ]; then
        print_message "${GREEN}" "✔ Success: $1"
    else
        print_message "${RED}" "✘ Failed: $1"
        exit 1
    fi
}

# Function to deploy resources
deploy() {
    print_message "${YELLOW}" "Starting deployment process..."
    
    # Build Docker image
    print_message "${YELLOW}" "Building Docker image..."
    ./gradlew bootBuildImage
    check_status "Docker image build"
    
    # List of deployment files
    local deployments=(
        "kubernetes/tinyurl-postgres-deployment.yaml"
        "kubernetes/tinyurl-redis-deployment.yaml"
        "kubernetes/tinyurl-prometheus-deployment.yaml"
        "kubernetes/tinyurl-app-deployment.yaml"
        "kubernetes/tinyurl-network-policy.yaml"
    )
    
    # Apply each deployment
    for deployment in "${deployments[@]}"; do
        print_message "${YELLOW}" "Applying $deployment..."
        kubectl apply -f "$deployment"
        check_status "Applied $deployment"
    done
    
    print_message "${GREEN}" "Deployment completed successfully! 🎉"
}

# Function to delete resources
delete() {
    print_message "${YELLOW}" "Starting deletion process..."
    
    # List of deployment files
    local deployments=(
        "kubernetes/tinyurl-network-policy.yaml"
        "kubernetes/tinyurl-app-deployment.yaml"
        "kubernetes/tinyurl-prometheus-deployment.yaml"
        "kubernetes/tinyurl-redis-deployment.yaml"
        "kubernetes/tinyurl-postgres-deployment.yaml"
    )
    
    # Delete each deployment
    for deployment in "${deployments[@]}"; do
        print_message "${YELLOW}" "Deleting $deployment..."
        kubectl delete -f "$deployment"
        check_status "Deleted $deployment"
    done
    
    print_message "${GREEN}" "Deletion completed successfully! 🧹"
}

# Main script
case "$1" in
    "deploy")
        deploy
        ;;
    "delete")
        delete
        ;;
    *)
        print_message "${RED}" "Usage: $0 {deploy|delete}"
        exit 1
        ;;
esac
