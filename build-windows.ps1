param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GradleArguments)
$ErrorActionPreference = 'Stop'
$distVersion = '9.8.2'
$cache = Join-Path $PSScriptRoot '.gradle-dist'
$dist = Join-Path $cache "gradle-$distVersion"
$gradleBat = Join-Path $dist 'bin\gradle.bat'

# Minecraft 26.3 requires Java 25. Use the JDK's release metadata to avoid
# PowerShell 5.1 treating `javac -version` on stderr as NativeCommandError.
# We choose a real JDK 25 instead of trusting whichever javac.exe is first on PATH.
$jdkCandidates = New-Object 'System.Collections.Generic.List[string]'
function Add-JdkCandidate {
    param([string]$Path)
    if (-not [string]::IsNullOrWhiteSpace($Path)) {
        $jdkCandidates.Add($Path.Trim('"').TrimEnd('\'))
    }
}
foreach ($candidate in @($env:JDK25_HOME, $env:JAVA25_HOME, $env:JAVA_HOME, $env:JDK_HOME)) {
    Add-JdkCandidate $candidate
}
# Also consider JDK installations already present in PATH.
foreach ($binPath in ($env:PATH -split ';')) {
    if ([string]::IsNullOrWhiteSpace($binPath)) { continue }
    $binPath = $binPath.Trim('"').TrimEnd('\')
    if ((Split-Path -Leaf $binPath) -ieq 'bin') {
        Add-JdkCandidate (Split-Path -Parent $binPath)
    }
}
$scanParents = New-Object 'System.Collections.Generic.List[string]'
foreach ($baseDir in @($env:ProgramFiles, ${env:ProgramFiles(x86)}, $env:ProgramW6432)) {
    if ([string]::IsNullOrWhiteSpace($baseDir)) { continue }
    foreach ($vendor in @('Eclipse Adoptium','Java','Microsoft','Amazon Corretto','Zulu','BellSoft','Adoptium','Liberica','Semeru')) {
        $scanParents.Add((Join-Path $baseDir $vendor))
    }
}
if ($env:USERPROFILE) {
    $scanParents.Add((Join-Path $env:USERPROFILE '.jdks'))
    Add-JdkCandidate (Join-Path $env:USERPROFILE 'scoop\apps\temurin25-jdk\current')
    Add-JdkCandidate (Join-Path $env:USERPROFILE 'scoop\apps\openjdk25\current')
}
if ($env:LOCALAPPDATA) {
    $scanParents.Add((Join-Path $env:LOCALAPPDATA 'Programs\Eclipse Adoptium'))
    $scanParents.Add((Join-Path $env:LOCALAPPDATA 'Programs\Java'))
}
foreach ($parent in ($scanParents | Select-Object -Unique)) {
    if (-not (Test-Path -LiteralPath $parent -PathType Container)) { continue }
    foreach ($child in (Get-ChildItem -LiteralPath $parent -Directory -ErrorAction SilentlyContinue)) {
        Add-JdkCandidate $child.FullName
    }
}

$jdkHome = $null
foreach ($candidate in ($jdkCandidates | Select-Object -Unique)) {
    $javacExe = Join-Path $candidate 'bin\javac.exe'
    $javaExe  = Join-Path $candidate 'bin\java.exe'
    $release  = Join-Path $candidate 'release'
    if (-not (Test-Path -LiteralPath $javacExe -PathType Leaf)) { continue }
    if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) { continue }
    if (-not (Test-Path -LiteralPath $release -PathType Leaf)) { continue }
    $metadata = Get-Content -LiteralPath $release -Raw
    if ($metadata -match '(?m)^JAVA_VERSION="25(?:[.\-+"]|$)') {
        $jdkHome = $candidate
        break
    }
}
if (-not $jdkHome) {
    throw @'
JDK 25 not found. The first javac in your PATH may be Java 8.
Set JAVA_HOME to the full JDK 25 directory, then retry. Example:
  $env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot'
  .\gradlew.bat clean build --console=plain
Replace the example path with your actual JDK 25 installation.
'@
}

# Set Java for this Gradle process only; do not permanently modify Windows.
$env:JAVA_HOME = $jdkHome
$env:PATH = (Join-Path $jdkHome 'bin') + ';' + $env:PATH
Write-Host "Using JDK 25: $jdkHome" -ForegroundColor Green

if (-not (Test-Path -LiteralPath $gradleBat)) {
    New-Item -ItemType Directory -Force -Path $cache | Out-Null
    $zip = Join-Path $cache "gradle-$distVersion-bin.zip"
    $checksumFile = "$zip.sha256"
    $downloadBase = "https://services.gradle.org/distributions/gradle-$distVersion-bin.zip"
    Write-Host 'Downloading official Gradle distribution and checksum...'
    Invoke-WebRequest -Uri $downloadBase -OutFile $zip
    Invoke-WebRequest -Uri "$downloadBase.sha256" -OutFile $checksumFile
    $expected = ((Get-Content -Raw -LiteralPath $checksumFile).Trim() -split '\s+')[0].ToLowerInvariant()
    $actual = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($expected -notmatch '^[0-9a-f]{64}$' -or $actual -ne $expected) {
        Remove-Item -LiteralPath $zip -Force -ErrorAction SilentlyContinue
        throw 'Gradle checksum verification failed. No executable will be used.'
    }
    Expand-Archive -LiteralPath $zip -DestinationPath $cache -Force
    Remove-Item -LiteralPath $zip, $checksumFile -Force
}

if (-not (Test-Path -LiteralPath $gradleBat)) {
    throw 'Gradle installation incomplete.'
}
if (-not $GradleArguments -or $GradleArguments.Count -eq 0) {
    $GradleArguments = @('clean', 'build', '--console=plain')
}
Push-Location $PSScriptRoot
try {
    & $gradleBat @GradleArguments
    $result = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $result
