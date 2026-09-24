$ErrorActionPreference = "Stop"

$ProjectDir = Split-Path -Parent $PSScriptRoot
$OutDir = Join-Path $ProjectDir "out"
$MainOut = Join-Path $OutDir "main"

New-Item -ItemType Directory -Force -Path $MainOut | Out-Null
$Sources = Get-ChildItem -Path (Join-Path $ProjectDir "src/main/java") -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }

javac --release 17 -encoding UTF-8 -d $MainOut $Sources

Write-Host "Build complete: $MainOut"

