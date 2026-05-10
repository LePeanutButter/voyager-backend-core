#!/usr/bin/env bash
#
# EC2 manual deploy for AWS Academy Learner Lab (no registry push).
# Installs Docker if missing, optionally loads the image from a .tar produced in CI,
# ensures the RDS PostgreSQL database exists (no Postgres in-container), and runs
# the app container under systemd (foreground docker run so restarts work).
#
# Usage (as root or with sudo):
#   sudo ./ec2-deploy-backend.sh [/path/to/voyager-backend-image.tar]
#
# If the tar path is omitted, looks for voyager-backend-image.tar in VOYAGER_INSTALL_ROOT
# or next to this script. If no tar is found, expects the image already present locally
# (e.g. docker build on the instance).
#
# Required in /opt/voyager-backend/environment (or VOYAGER_ENV_FILE):
#   DB_HOST, DB_USERNAME, DB_PASSWORD — for psql bootstrap (CREATE DATABASE)
#   DB_URL, JWT_SECRET — for the Spring Boot container (via --env-file)
#
# Optional:
#   DB_PORT, DB_NAME, DB_ADMIN_DATABASE, PGSSLMODE
#   VOYAGER_IMAGE        image reference after load, default voyager-backend:latest
#   VOYAGER_INSTALL_ROOT default /opt/voyager-backend
#   VOYAGER_ENV_FILE     default $INSTALL_ROOT/environment
#   VOYAGER_SERVICE_NAME systemd unit name, default voyager-backend
#
# CORS: see application.yml / previous docs (CORS_* variables in the same env file).

set -euo pipefail

readonly INSTALL_ROOT="${VOYAGER_INSTALL_ROOT:-$(pwd)}"
readonly SERVICE_NAME="${VOYAGER_SERVICE_NAME:-voyager-backend}"
readonly CONTAINER_NAME="${VOYAGER_CONTAINER_NAME:-voyager-backend}"
readonly ENV_FILE="${VOYAGER_ENV_FILE:-$INSTALL_ROOT/environment}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_TAR_CLI="${1:-}"

log() { echo "[$(date -Iseconds)] $*"; }
die() { echo "ERROR: $*" >&2; exit 1; }

require_root() {
  if [[ "$(id -u)" -ne 0 ]]; then
    die "Run as root (sudo)."
  fi
}

detect_os() {
  if [[ -f /etc/os-release ]]; then
    # shellcheck source=/dev/null
    . /etc/os-release
    echo "${ID:-unknown} ${VERSION_ID:-}"
  else
    echo "unknown"
  fi
}

ensure_docker_daemon() {
  if systemctl is-system-running &>/dev/null; then
    systemctl enable docker &>/dev/null || true
    systemctl start docker || service docker start || die "Could not start Docker daemon."
  else
    service docker start 2>/dev/null || true
  fi
  docker info >/dev/null 2>&1 || die "Docker daemon is not running."
}

install_docker() {
  if command -v docker >/dev/null 2>&1; then
    ensure_docker_daemon
    log "Docker already installed: $(docker --version)"
    return 0
  fi
  log "Installing Docker..."
  local os
  os="$(detect_os)"
  case "$os" in
    amzn\ 2*)
      yum install -y docker
      ;;
    amzn\ 2023*|fedora*|rocky*|almalinux*)
      dnf install -y docker
      ;;
    ubuntu*|debian*)
      apt-get update -y
      apt-get install -y docker.io
      ;;
    *)
      die "Unsupported OS for automatic Docker install: $os. Install Docker manually."
      ;;
  esac
  ensure_docker_daemon
  command -v docker >/dev/null 2>&1 || die "Docker CLI not available after install."
}

install_psql_client() {
  if command -v psql >/dev/null 2>&1; then
    return 0
  fi
  log "Installing PostgreSQL client (psql)..."
  local os
  os="$(detect_os)"
  case "$os" in
    amzn\ 2*)
      amazon-linux-extras install -y postgresql14 >/dev/null 2>&1 || true
      yum install -y postgresql
      ;;
    amzn\ 2023*|fedora*)
      dnf install -y postgresql15
      ;;
    ubuntu*|debian*)
      apt-get update -y
      apt-get install -y postgresql-client
      ;;
    *)
      die "Unsupported OS for automatic psql install: $os."
      ;;
  esac
  command -v psql >/dev/null 2>&1 || die "psql not available after install attempt."
}

load_environment() {
  mkdir -p "$INSTALL_ROOT"
  
  # Check if environment file exists in INSTALL_ROOT, otherwise copy from script directory
  if [[ ! -f "$ENV_FILE" && -f "$SCRIPT_DIR/environment" ]]; then
    log "Copying environment file from script directory to $ENV_FILE"
    cp "$SCRIPT_DIR/environment" "$ENV_FILE"
    chmod 0600 "$ENV_FILE"
  fi
  
  if [[ -f "$ENV_FILE" ]]; then
    log "Loading $ENV_FILE"
    set +u
    # shellcheck source=/dev/null
    set -a && source "$ENV_FILE" && set +a
    set -u
  fi
}

resolve_image_tar() {
  local tar_path="$IMAGE_TAR_CLI"
  if [[ -z "$tar_path" ]]; then
    if [[ -f "$INSTALL_ROOT/voyager-backend-image.tar" ]]; then
      tar_path="$INSTALL_ROOT/voyager-backend-image.tar"
    elif [[ -f "$SCRIPT_DIR/voyager-backend-image.tar" ]]; then
      tar_path="$SCRIPT_DIR/voyager-backend-image.tar"
    fi
  fi
  echo "${tar_path:-}"
}

load_image_if_needed() {
  local tar_path
  tar_path="$(resolve_image_tar)"
  if [[ -n "$tar_path" ]]; then
    [[ -f "$tar_path" ]] || die "Image tar not found: $tar_path"
    log "docker load -i $tar_path"
    docker load -i "$tar_path"
    install -d -m 0755 "$INSTALL_ROOT"
    install -m 0644 "$tar_path" "$INSTALL_ROOT/voyager-backend-image.tar" 2>/dev/null || true
  fi
}

ensure_database_exists() {
  [[ -n "${DB_HOST:-}" ]] || die "DB_HOST is not set."
  [[ -n "${DB_USERNAME:-}" ]] || die "DB_USERNAME is not set."
  [[ -n "${DB_PASSWORD:-}" ]] || die "DB_PASSWORD is not set."
  local port="${DB_PORT:-5432}"
  local dbname="${DB_NAME:-tourism_platform}"
  local admin_db="${DB_ADMIN_DATABASE:-postgres}"
  export PGPASSWORD="$DB_PASSWORD"
  export PGSSLMODE="${PGSSLMODE:-require}"

  log "Checking PostgreSQL database '$dbname' on $DB_HOST:$port ..."
  local exists
  exists="$(psql -h "$DB_HOST" -p "$port" -U "$DB_USERNAME" -d "$admin_db" -tAc \
    "SELECT 1 FROM pg_database WHERE datname = '$dbname'" || true)"
  if [[ "$(echo "$exists" | tr -d '[:space:]')" == "1" ]]; then
    log "Database '$dbname' already exists."
    return 0
  fi
  log "Creating database '$dbname' (Flyway applies schema when the app starts)."
  psql -h "$DB_HOST" -p "$port" -U "$DB_USERNAME" -d "$admin_db" -v ON_ERROR_STOP=1 \
    -c "CREATE DATABASE \"$dbname\";"
  unset PGPASSWORD
}

write_systemd_unit() {
  local image_ref="${VOYAGER_IMAGE:-voyager-backend:latest}"
  cat >"/etc/systemd/system/${SERVICE_NAME}.service" <<EOF
[Unit]
Description=Voyager backend (Docker)
After=docker.service network-online.target
Requires=docker.service
Wants=network-online.target

[Service]
Type=simple
TimeoutStartSec=0
Restart=always
RestartSec=15
WorkingDirectory=$INSTALL_ROOT
# Recreate container on each start so image updates take effect after deploy.
ExecStartPre=-/usr/bin/docker stop $CONTAINER_NAME
ExecStartPre=-/usr/bin/docker rm $CONTAINER_NAME
ExecStart=/usr/bin/docker run --name $CONTAINER_NAME \\
  --env-file $ENV_FILE \\
  -p 0.0.0.0:8080:8080 \\
  -p 0.0.0.0:8081:8081 \\
  $image_ref
ExecStop=/usr/bin/docker stop $CONTAINER_NAME
ExecStopPost=-/usr/bin/docker rm $CONTAINER_NAME

[Install]
WantedBy=multi-user.target
EOF
}

create_template_environment() {
  log "Creating example $ENV_FILE — edit with real values, then re-run this script."
  install -d -m 0755 "$INSTALL_ROOT"
  cat >"$ENV_FILE" <<'EOF'
SPRING_PROFILES_ACTIVE=prod
VOYAGER_IMAGE=voyager-backend:latest
DB_HOST=your-rds.region.rds.amazonaws.com
DB_PORT=5432
DB_NAME=tourism_platform
DB_USERNAME=your_master_user
DB_PASSWORD=your_password
DB_URL=jdbc:postgresql://your-rds.region.rds.amazonaws.com:5432/tourism_platform?sslmode=require
JWT_SECRET=change-me-min-32-chars-random
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GOOGLE_REDIRECT_URI=https://your-domain.com/api/v1/auth/google/callback
EOF
  chmod 0600 "$ENV_FILE"
}

assert_environment_ready() {
  if grep -qE '(your-rds\.region\.rds\.amazonaws\.com|your_master_user|your_password|your-google-client-id|your-google-client-secret|your-domain\.com)' "$ENV_FILE" 2>/dev/null; then
    die "Edit $ENV_FILE and replace placeholder values before continuing."
  fi
  if grep -qF 'change-me-min-32-chars-random' "$ENV_FILE" 2>/dev/null; then
    die "Set a strong JWT_SECRET in $ENV_FILE before continuing."
  fi
}

assert_image_present() {
  local image="${VOYAGER_IMAGE:-voyager-backend:latest}"
  docker image inspect "$image" >/dev/null 2>&1 || die "Docker image '$image' not found. Run with path to voyager-backend-image.tar from CI or build the image on this host."
}

main() {
  require_root
  install -d -m 0755 "$INSTALL_ROOT"

  if [[ ! -f "$ENV_FILE" ]]; then
    create_template_environment
    die "Template created at $ENV_FILE. Edit it and run this script again."
  fi

  load_environment
  assert_environment_ready

  install_docker
  install_psql_client

  ensure_database_exists
  load_image_if_needed
  assert_image_present

  write_systemd_unit
  systemctl daemon-reload
  systemctl enable "$SERVICE_NAME"
  systemctl restart "$SERVICE_NAME"
  log "Service $SERVICE_NAME started. Check: systemctl status $SERVICE_NAME  |  docker logs -f $CONTAINER_NAME"
}

main "$@"
