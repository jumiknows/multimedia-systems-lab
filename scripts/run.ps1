$ErrorActionPreference = "Stop"

$ProjectDir = Split-Path -Parent $PSScriptRoot
$MainOut = Join-Path $ProjectDir "out/main"

& (Join-Path $PSScriptRoot "build.ps1")
java -cp $MainOut ca.ernestwong.multimedia.MultimediaLabApp

