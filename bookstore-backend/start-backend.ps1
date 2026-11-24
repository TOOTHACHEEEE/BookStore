# Start the Spring Boot backend with UTF-8 console output.
# Usage: right-click -> "Run with PowerShell", or run: .\start-backend.ps1
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null
Set-Location $PSScriptRoot
.\mvnw.cmd spring-boot:run
