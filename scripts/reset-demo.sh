#!/usr/bin/env bash
# Reset only the local demo database after backing up its current contents.
set -euo pipefail
task_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
task_volume=cats-dev_dev-data
task_compose=(docker compose --project-name cats-dev --file "$task_root/compose.yml")
if [[ $# -gt 1 || ( $# -eq 1 && $1 != --yes ) ]]; then
    echo 'Usage: bash scripts/reset-demo.sh [--yes]' >&2
    exit 1
fi
echo 'This clears cats-dev and restores the fixed October 2026 demo data.'
echo 'Stop Spring Boot first. Automated tests and production are not reset.'
if command -v curl >/dev/null && curl -fsS --max-time 2 "http://127.0.0.1:${CATS_HTTP_PORT:-8081}/health" >/dev/null 2>&1; then
    echo 'Stop Spring Boot, then run this script again.' >&2
    exit 1
fi
if [[ ${1:-} != --yes ]]; then
    read -r -p 'Type RESET to continue: ' task_answer
    if [[ $task_answer != RESET ]]; then echo 'Cancelled.'; exit 0; fi
fi

# Pin the project and confirm its configured database/volume before removing data.
task_config=$("${task_compose[@]}" config)
grep -Eq '^name: cats-dev$' <<< "$task_config"
grep -Eq 'MYSQL_DATABASE: cats-dev$' <<< "$task_config"
grep -Eq 'name: cats-dev_dev-data$' <<< "$task_config"
test -f "$task_root/src/main/resources/db/schema.sql"
test -f "$task_root/src/main/resources/db/demo-data.sql"
if docker volume ls --format '{{.Name}}' | grep -Fxq "$task_volume"; then
    [[ $(docker volume inspect "$task_volume" --format '{{index .Labels "com.docker.compose.project"}}') == cats-dev ]]
    [[ $(docker volume inspect "$task_volume" --format '{{index .Labels "com.docker.compose.volume"}}') == dev-data ]]
    "${task_compose[@]}" up -d --wait
    mkdir -p "$task_root/backups"
    task_backup="$task_root/backups/cats-dev-$(date +%Y%m%d-%H%M%S)-$$.sql"
    "${task_compose[@]}" exec -T db sh -c 'MYSQL_PWD=$MYSQL_PASSWORD exec mysqldump --user=$MYSQL_USER --single-transaction --no-tablespaces --hex-blob --set-gtid-purged=OFF --result-file=/tmp/cats-demo-backup.sql $MYSQL_DATABASE'
    "${task_compose[@]}" cp db:/tmp/cats-demo-backup.sql "$task_backup"
    test -s "$task_backup"
    echo "Backup: $task_backup"
    "${task_compose[@]}" down
    docker volume rm "$task_volume"
fi
"${task_compose[@]}" up -d --wait
# Check fixtures and confirm this is a fresh schema rather than a migrated database.
task_counts=$("${task_compose[@]}" exec -T db sh -c 'MYSQL_PWD=$MYSQL_PASSWORD exec mysql --user=$MYSQL_USER --database=$MYSQL_DATABASE --batch --skip-column-names' <<'SQL'
SELECT (SELECT COUNT(*) FROM users), (SELECT COUNT(*) FROM training_entitlement), (SELECT COUNT(*) FROM course_detail), (SELECT COUNT(*) FROM course_batch), (SELECT COUNT(*) FROM course_application), (SELECT COUNT(*) FROM course_fee_application), (SELECT COUNT(*) FROM excluded_days), (SELECT COUNT(*) FROM course_application WHERE status='APPROVED'), (SELECT COUNT(*) FROM course_application WHERE course_id=102 AND staff_id=4 AND status='APPROVED' AND course_end_date='2026-10-09' AND experience_comments IS NULL), (SELECT COUNT(*) FROM course_fee_application WHERE course_application_id=102), (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='flyway_schema_history');
SQL
)
if [[ $(echo "$task_counts" | tr -s '[:space:]' ',' | sed 's/,$//') != '6,15,4,6,11,3,14,1,1,0,0' ]]; then
    echo 'The demo data check failed. Review MySQL logs before starting the application.' >&2
    exit 1
fi
echo 'Demo ready: staff has all seven statuses; its Approved course ended on 9 October.'
echo 'Start Spring Boot again. All demo account passwords are demo123.'
