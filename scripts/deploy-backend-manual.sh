#!/usr/bin/env bash
set -euo pipefail

# Manual deploy for AWS Academy / Learner Lab
# Requires local AWS CLI auth already active (temporary credentials).
#
# Usage:
#   ./scripts/deploy-backend-manual.sh /path/to/backend-app.jar /path/to/smarttrip-key.pem

JAR_PATH="${1:-}"
KEY_PATH="${2:-}"

AWS_REGION="${AWS_REGION:-us-east-1}"
BACKEND_ASG_NAME="${BACKEND_ASG_NAME:-smarttrip-backend-asg}"
SSH_USER="${SSH_USER:-ec2-user}"
BACKEND_APP_DIR="${BACKEND_APP_DIR:-/opt/smarttrip/backend}"
BACKEND_APP_NAME="${BACKEND_APP_NAME:-backend-app.jar}"

if [[ -z "${JAR_PATH}" || -z "${KEY_PATH}" ]]; then
  echo "Usage: $0 <jar-path> <ssh-key-path>"
  exit 1
fi

if [[ ! -f "${JAR_PATH}" ]]; then
  echo "JAR not found: ${JAR_PATH}"
  exit 1
fi

if [[ ! -f "${KEY_PATH}" ]]; then
  echo "SSH key not found: ${KEY_PATH}"
  exit 1
fi

echo "Resolving backend instances from ASG: ${BACKEND_ASG_NAME}"
INSTANCE_IDS=$(aws autoscaling describe-auto-scaling-groups \
  --region "${AWS_REGION}" \
  --auto-scaling-group-names "${BACKEND_ASG_NAME}" \
  --query "AutoScalingGroups[0].Instances[?LifecycleState=='InService'].InstanceId" \
  --output text)

if [[ -z "${INSTANCE_IDS}" ]]; then
  echo "No InService instances found in ASG ${BACKEND_ASG_NAME}"
  exit 1
fi

IPS=$(aws ec2 describe-instances \
  --region "${AWS_REGION}" \
  --instance-ids ${INSTANCE_IDS} \
  --query "Reservations[].Instances[?State.Name=='running'].PublicIpAddress" \
  --output text | tr '\t' '\n' | sed '/^$/d')

if [[ -z "${IPS}" ]]; then
  echo "No public IPs found for backend instances"
  exit 1
fi

chmod 600 "${KEY_PATH}"
ssh-keyscan -H ${IPS} >> ~/.ssh/known_hosts 2>/dev/null || true

for IP in ${IPS}; do
  echo "Deploying backend artifact to ${IP}"
  scp -i "${KEY_PATH}" -o StrictHostKeyChecking=yes "${JAR_PATH}" "${SSH_USER}@${IP}:/tmp/${BACKEND_APP_NAME}"

  ssh -i "${KEY_PATH}" -o StrictHostKeyChecking=yes "${SSH_USER}@${IP}" << EOF
set -e
mkdir -p "${BACKEND_APP_DIR}"
mv "/tmp/${BACKEND_APP_NAME}" "${BACKEND_APP_DIR}/${BACKEND_APP_NAME}"
pkill -f "${BACKEND_APP_NAME}" || true
cd "${BACKEND_APP_DIR}"
nohup java -jar "${BACKEND_APP_NAME}" > backend.log 2>&1 &
EOF
done

echo "Backend deployment completed."
