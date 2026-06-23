<#
  TicKetch 인프라 중지 스크립트.
  Spring 서비스(별도 PowerShell 창)는 각 창에서 Ctrl+C 하거나 창을 닫아 종료한다.

  사용법:   .\stop-all.ps1            # 컨테이너 중지 (데이터 보존)
            .\stop-all.ps1 -Down     # 컨테이너 제거 (볼륨은 보존)
            .\stop-all.ps1 -Purge    # 컨테이너 + 볼륨까지 삭제 (DB 초기화)
#>
param(
    [switch]$Down,
    [switch]$Purge
)
$root = $PSScriptRoot
$compose = Join-Path $root 'docker-compose.yml'

if ($Purge) {
    Write-Host "인프라 컨테이너 + 볼륨 삭제 (DB 초기화)"
    docker compose -f $compose down -v
} elseif ($Down) {
    Write-Host "인프라 컨테이너 제거 (볼륨 보존)"
    docker compose -f $compose down
} else {
    Write-Host "인프라 컨테이너 중지 (데이터 보존)"
    docker compose -f $compose stop
}
Write-Host "`n참고: Spring 서비스 창은 각 창에서 Ctrl+C 또는 창 닫기로 종료하세요."
