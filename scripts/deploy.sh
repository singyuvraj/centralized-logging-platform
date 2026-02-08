#!/bin/bash

# Deployment script for EC2
# Usage: ./scripts/deploy.sh <environment> <image-tag>

set -e

ENVIRONMENT=${1:-dev}
IMAGE_TAG=${2:-latest}
DOCKER_IMAGE=${DOCKER_IMAGE:-ghcr.io/valantech/suljhaoo-backend-service}

# Determine profile based on environment (can be overridden by SPRING_PROFILES_ACTIVE env var)
if [ -z "$SPRING_PROFILES_ACTIVE" ]; then
  case $ENVIRONMENT in
    prod|production)
      SPRING_PROFILE=prod
      ;;
    stage|staging)
      SPRING_PROFILE=stage
      ;;
    qa)
      SPRING_PROFILE=qa
      ;;
    *)
      SPRING_PROFILE=dev
      ;;
  esac
else
  SPRING_PROFILE=$SPRING_PROFILES_ACTIVE
fi

echo "Deploying $IMAGE_NAME:$IMAGE_TAG"
echo "  Environment: $ENVIRONMENT"
echo "  Spring Profile: $SPRING_PROFILE"
echo "  Parameter Store Environment: $PARAMETER_STORE_ENV"
echo "Deploying $DOCKER_IMAGE:$IMAGE_TAG to $ENVIRONMENT environment with profile $SPRING_PROFILE"

# Login to container registry (if needed). Set GHCR_USER if using a different registry user.
if [ -n "$DOCKER_GHCR_TOKEN" ]; then
  echo "$DOCKER_GHCR_TOKEN" | docker login ghcr.io -u "${GHCR_USER:-tapasrwth}" --password-stdin
fi

# Pull latest image
docker pull $DOCKER_IMAGE:$IMAGE_TAG || echo "Image not found, will build locally"

# Stop and remove old container
docker stop suljhaoo-backend-service 2>/dev/null || true
docker rm suljhaoo-backend-service 2>/dev/null || true

# Run new container
docker run -d \
  --name suljhaoo-backend-service \
  --restart unless-stopped \
  -p 8443:8080 \
  -e SPRING_PROFILES_ACTIVE=$SPRING_PROFILE \
  -e AWS_PARAMETERSTORE_ENVIRONMENT=$PARAMETER_STORE_ENV \
  -e AWS_REGION=${AWS_REGION:-ap-south-1} \
  ${AWS_ACCESS_KEY_ID:+-e AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID} \
  ${AWS_SECRET_ACCESS_KEY:+-e AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY} \
  $DOCKER_IMAGE:$IMAGE_TAG

# Wait for health check
echo "Waiting for application to be healthy..."
sleep 10

# Check if container is running
if docker ps | grep -q suljhaoo-backend; then
  echo "Container is running."
  docker logs --tail 50 suljhaoo-backend
else
  echo "Container did not start."
  docker logs suljhaoo-backend
  exit 1
fi

# Clean up old images
docker image prune -f

