$ErrorActionPreference = "Stop"

$ProjectDir = Split-Path -Parent $PSScriptRoot
$OutDir = Join-Path $ProjectDir "out"
$MainOut = Join-Path $OutDir "main"
$TestOut = Join-Path $OutDir "test"

& (Join-Path $PSScriptRoot "build.ps1")
New-Item -ItemType Directory -Force -Path $TestOut | Out-Null

$TestSources = Get-ChildItem -Path (Join-Path $ProjectDir "src/test/java") -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
javac --release 17 -encoding UTF-8 -cp $MainOut -d $TestOut $TestSources
java -Djava.awt.headless=true -cp "$MainOut;$TestOut" ca.ernestwong.multimedia.TestRunner

