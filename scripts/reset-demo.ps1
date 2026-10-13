# Reset only the local demo database after backing up its current contents.
param([switch]$Yes)
$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskCompose = @('compose', '--project-name', 'cats-dev', '--file', (Join-Path $taskRoot 'compose.yml'))
$taskVolume = 'cats-dev_dev-data'

# Stop at the first Docker failure, before any later destructive step.
function Invoke-Docker {
    & docker @args
    if ($LASTEXITCODE -ne 0) { throw 'Docker command failed. The reset did not finish.' }
}

Write-Host 'This clears cats-dev and restores the fixed October 2026 demo data.'
Write-Host 'Stop Spring Boot first. Automated tests and production are not reset.'
$taskPort = 8081
if ($env:CATS_HTTP_PORT) { $taskPort = [int]$env:CATS_HTTP_PORT }
if (Get-NetTCPConnection -State Listen -LocalPort $taskPort -ErrorAction SilentlyContinue) {
    throw "Stop the application listening on port $taskPort, then run this script again."
}
if (-not $Yes -and (Read-Host 'Type RESET to continue') -cne 'RESET') {
    Write-Host 'Cancelled.'
    exit 0
}

# Pin the Compose file/project and check the actual volume before removing it.
$taskConfig = (Invoke-Docker @taskCompose config --format json) | ConvertFrom-Json
if ($taskConfig.name -ne 'cats-dev' -or $taskConfig.volumes.'dev-data'.name -ne $taskVolume -or
    $taskConfig.services.db.environment.MYSQL_DATABASE -ne 'cats-dev') {
    throw 'Unexpected development database configuration.'
}
foreach ($taskSQL in @('schema.sql', 'demo-data.sql')) {
    if (-not (Test-Path -LiteralPath (Join-Path $taskRoot "src/main/resources/db/$taskSQL") -PathType Leaf)) {
        throw "Missing $taskSQL. Nothing has been removed."
    }
}
$taskExisting = @(Invoke-Docker volume ls --format '{{.Name}}') -contains $taskVolume
if ($taskExisting) {
    $taskLabels = (Invoke-Docker volume inspect $taskVolume --format '{{json .Labels}}') | ConvertFrom-Json
    if ($taskLabels.'com.docker.compose.project' -ne 'cats-dev' -or
        $taskLabels.'com.docker.compose.volume' -ne 'dev-data') {
        throw 'The volume is not owned by the cats-dev project.'
    }
    Invoke-Docker @taskCompose up -d --wait
    $taskBackupDir = Join-Path $taskRoot 'backups'
    New-Item -ItemType Directory -Path $taskBackupDir -Force | Out-Null
    $taskBackup = Join-Path $taskBackupDir ('cats-dev-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.sql')
    Invoke-Docker @taskCompose exec -T db sh -c 'MYSQL_PWD=$MYSQL_PASSWORD exec mysqldump --user=$MYSQL_USER --single-transaction --no-tablespaces --hex-blob --set-gtid-purged=OFF --result-file=/tmp/cats-demo-backup.sql $MYSQL_DATABASE'
    Invoke-Docker @taskCompose cp db:/tmp/cats-demo-backup.sql $taskBackup
    if ((Get-Item -LiteralPath $taskBackup).Length -eq 0) { throw 'The backup is empty. Nothing has been removed.' }
    Write-Host "Backup: $taskBackup"
    Invoke-Docker @taskCompose down
    Invoke-Docker volume rm $taskVolume
}

Invoke-Docker @taskCompose up -d --wait
# Check the main fixtures, the one ended Approved course, and absence of migration history.
$taskQuery = 'SELECT (SELECT COUNT(*) FROM users), (SELECT COUNT(*) FROM training_entitlement), (SELECT COUNT(*) FROM course_detail), (SELECT COUNT(*) FROM course_batch), (SELECT COUNT(*) FROM course_application), (SELECT COUNT(*) FROM course_fee_application), (SELECT COUNT(*) FROM excluded_days), (SELECT COUNT(*) FROM course_application WHERE status=''APPROVED''), (SELECT COUNT(*) FROM course_application WHERE course_id=102 AND staff_id=4 AND status=''APPROVED'' AND course_end_date=''2026-10-09'' AND experience_comments IS NULL), (SELECT COUNT(*) FROM course_fee_application WHERE course_application_id=102), (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=''flyway_schema_history'');'
$taskCounts = $taskQuery | & docker @taskCompose exec -T db sh -c 'MYSQL_PWD=$MYSQL_PASSWORD exec mysql --user=$MYSQL_USER --database=$MYSQL_DATABASE --batch --skip-column-names'
if ($LASTEXITCODE -ne 0) { throw 'The demo data query failed.' }
if ((($taskCounts.Trim() -split '\s+') -join ',') -ne '6,15,4,6,11,3,14,1,1,0,0') {
    throw 'The demo data check failed. Review the MySQL container logs before starting the application.'
}
Write-Host 'Demo ready: staff has all seven statuses; its Approved course ended on 9 October.'
Write-Host 'Start Spring Boot again. All demo account passwords are demo123.'
