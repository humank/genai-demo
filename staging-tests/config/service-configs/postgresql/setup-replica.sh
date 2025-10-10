#!/bin/bash
set -e

# Wait for primary to be ready
until pg_isready -h postgres-primary -p 5432 -U postgres; do
  echo "Waiting for primary database to be ready..."
  sleep 2
done

# Stop PostgreSQL if running
pg_ctl stop -D "$PGDATA" -m fast || true

# Remove existing data directory
rm -rf "$PGDATA"/*

# Create base backup from primary
PGPASSWORD=repl_password pg_basebackup -h postgres-primary -D "$PGDATA" -U replicator -v -P -W

# Create recovery configuration
cat > "$PGDATA/postgresql.auto.conf" << EOF
primary_conninfo = 'host=postgres-primary port=5432 user=replicator password=repl_password'
primary_slot_name = 'replica_slot'
EOF

# Create standby signal file
touch "$PGDATA/standby.signal"

# Set proper permissions
chmod 700 "$PGDATA"
chown -R postgres:postgres "$PGDATA"

echo "Replica setup completed"