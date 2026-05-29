$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$classes = Join-Path $root "out\classes"
$sources = Join-Path $root "sources.txt"

if (Test-Path $classes) {
  Remove-Item $classes -Recurse -Force
}
New-Item -ItemType Directory -Path $classes -Force | Out-Null

Get-ChildItem -Path (Join-Path $root "src\main\java") -Recurse -Filter "*.java" |
  ForEach-Object { $_.FullName } |
  Set-Content -Path $sources -Encoding UTF8

javac -encoding UTF-8 -d $classes "@$sources"

Write-Host "Build success."
Write-Host "Run with: .\run.ps1"
