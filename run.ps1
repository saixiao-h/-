$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$classes = Join-Path $root "out\classes"

if (-not (Test-Path $classes)) {
  & (Join-Path $root "build.ps1")
}

Set-Location $root
java -cp $classes com.familyledger.LedgerApplication 8080
