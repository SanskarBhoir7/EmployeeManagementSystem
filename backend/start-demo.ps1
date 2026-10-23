$ErrorActionPreference = 'Stop'

Push-Location $PSScriptRoot
$mavenExitCode = 1

try {
    $env:DB_NAME = 'ems_pro_demo'
    $env:DB_USERNAME = 'ems_demo'
    $securePassword = Read-Host 'Enter the ems_demo MySQL password' -AsSecureString
    $env:DB_PASSWORD = [PSCredential]::new('ems_demo', $securePassword).GetNetworkCredential().Password
    $securePassword.Dispose()

    $randomBytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($randomBytes)
        $env:JWT_SECRET = [BitConverter]::ToString($randomBytes).Replace('-', '')
    }
    finally {
        $rng.Dispose()
    }

    $env:SPRING_PROFILES_ACTIVE = 'demo'
    & mvn -o spring-boot:run
    $mavenExitCode = $LASTEXITCODE
}
finally {
    $env:DB_PASSWORD = $null
    $env:JWT_SECRET = $null
    Pop-Location
}

exit $mavenExitCode
