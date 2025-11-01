#!/bin/bash
set -e 

# Custom ASCII Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
IMAGE_NAME="opty-socket"
IMAGE_TAG="${IMAGE_TAG:-${1:-latest}}"
DOCKERFILE_PATH="./data/Dockerfile"

# Build Start Message
echo -e "${GREEN}--- Building opty-socket Docker image ---${NC}"
echo ""

# Check if Dockerfile exists
if [ ! -f "$DOCKERFILE_PATH" ]; then
    echo -e "${RED}Error: Dockerfile not found at $DOCKERFILE_PATH${NC}"
    exit 1
fi

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running${NC}"
    exit 1
fi

# Display build configuration
echo -e "${YELLOW}Image name:${NC} $IMAGE_NAME:$IMAGE_TAG"
echo -e "${YELLOW}Dockerfile:${NC} $DOCKERFILE_PATH"
echo ""

# Build the Docker image
echo -e "${GREEN}Starting Docker build...${NC}"
docker build \
    -f "$DOCKERFILE_PATH" \
    -t "$IMAGE_NAME:$IMAGE_TAG" \
    --platform linux/amd64 \
    .

# Check if build was successful
if [ $? -eq 0 ]; then

    # Success Message
    echo ""
    echo -e "${GREEN}--- Build completed successfully! ---${NC}"
    echo ""
    echo -e "${YELLOW}Image details:${NC}"
    docker images | grep "$IMAGE_NAME" | grep "$IMAGE_TAG"
    echo ""
    exit 0

# Error in Docker build
else

    # Failure Message
    echo ""
    echo -e "${RED}--- Build failed! ---${NC}"
    echo ""
    exit 1

fi
