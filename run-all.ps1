<#
  TicKetch 백엔드 전체 기동 스크립트 (Windows PowerShell)

  순서: 인프라(docker) → Eureka → Config Server → 비즈니스 서비스 5개 + Gateway
  각 Spring 서비스는 별도 PowerShell 창에서 bootRun으로 실행되어 로그를 보여준다.

  사용법:   .\run-all.ps1
  종료:     .\stop-all.ps1  (인프라 중지) + 각 서비스 창 닫기(또는 Ctrl+C)
#>

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$cfgRepo = Join-Path $root 'config-repo'

function Wait-Healthy([string]$container, [int]$timeoutSec = 120) {
    Write-Host "  대기: $container 헬스체크..." -NoNewline
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $timeoutSec) {
        try {
            $status = docker inspect --format '{{.State.Health.Status}}' $container 2>$null
            if ($status -eq 'healthy') { Write-Host " OK"; return }
        } catch {}
        Start-Sleep -Seconds 3; Write-Host "." -NoNewline
    }
    Write-Host " (타임아웃 — 계속 진행)"
}

function Wait-Http([string]$url, [string]$name, [int]$timeoutSec = 120) {
    Write-Host "  대기: $name ($url)..." -NoNewline
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $timeoutSec) {
        try {
            Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3 | Out-Null
            Write-Host " OK"; return
        } catch {
            # 연결되어 응답(4xx 포함)이 오면 기동된 것으로 간주
            if ($_.Exception.Response) { Write-Host " OK"; return }
        }
        Start-Sleep -Seconds 3; Write-Host "." -NoNewline
    }
    Write-Host " (타임아웃 — 계속 진행)"
}

function Start-Service([string]$label, [string]$gradleTask, [string]$preCmd = '') {
    Write-Host "▶ 기동: $label ($gradleTask)"
    $cmd = "cd '$root'; $preCmd ./gradlew $gradleTask --console=plain"
    Start-Process powershell -ArgumentList '-NoExit', '-Command', $cmd -WindowStyle Normal | Out-Null
}

Write-Host "============================================================"
Write-Host " TicKetch 백엔드 전체 기동"
Write-Host "============================================================"

# 1) 인프라 (MySQL x4, MongoDB, Redis, RabbitMQ, Zipkin) — jenkins/sonar는 ci 프로파일이라 제외
Write-Host "`n[1/4] 인프라 컨테이너 기동 (docker compose up -d)"
docker compose -f (Join-Path $root 'docker-compose.yml') up -d
Wait-Healthy 'ticketch-mysql-user'
Wait-Healthy 'ticketch-mysql-event'
Wait-Healthy 'ticketch-mysql-reservation'
Wait-Healthy 'ticketch-mysql-payment'
Wait-Healthy 'ticketch-redis'
Wait-Healthy 'ticketch-rabbitmq'

# 2) Eureka (서비스 디스커버리)
Write-Host "`n[2/4] Eureka Server"
Start-Service 'eureka-server' ':infrastructure:eureka-server:bootRun'
Wait-Http 'http://localhost:8761' 'Eureka'

# 3) Config Server (config-repo 경로 주입)
Write-Host "`n[3/4] Config Server"
Start-Service 'config-server' ':infrastructure:config-server:bootRun' "`$env:CONFIG_REPO_PATH='$cfgRepo';"
Wait-Http 'http://localhost:8888/actuator/health' 'Config Server'

# 4) 비즈니스 서비스 + Gateway
Write-Host "`n[4/4] 비즈니스 서비스 + Gateway"
Start-Service 'user-service'         ':services:user-service:bootRun'
Start-Service 'event-service'        ':services:event-service:bootRun'
Start-Service 'reservation-service'  ':services:reservation-service:bootRun'
Start-Service 'payment-service'      ':services:payment-service:bootRun'
Start-Service 'notification-service' ':services:notification-service:bootRun'
Start-Sleep -Seconds 5
Start-Service 'api-gateway'          ':infrastructure:api-gateway:bootRun'

Write-Host "`n============================================================"
Write-Host " 기동 명령 완료. 각 서비스는 개별 창에서 부팅 중입니다."
Write-Host "------------------------------------------------------------"
Write-Host " Gateway        : http://localhost:8080  (프론트가 호출하는 단일 진입점)"
Write-Host " Eureka 대시보드 : http://localhost:8761"
Write-Host " RabbitMQ UI    : http://localhost:15672  (admin/admin)"
Write-Host " Zipkin         : http://localhost:9411"
Write-Host " 서비스 포트     : user 8081 · event 8082 · reservation 8083 · payment 8084 · notification 8085"
Write-Host "------------------------------------------------------------"
Write-Host " 모든 서비스가 Eureka에 등록되면 준비 완료 (1~2분 소요)."
Write-Host " 종료: .\stop-all.ps1 + 각 서비스 창 닫기"
Write-Host "============================================================"
