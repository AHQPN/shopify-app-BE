# Shopify OAuth Flow - Quick Start Script
# Run this to start tunnel and backend

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "  Shopify OAuth Setup Helper" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Check if cloudflared is installed
$cloudflared = Get-Command cloudflared -ErrorAction SilentlyContinue

if (-not $cloudflared) {
    Write-Host "⚠️  Cloudflared not found!" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Install options:" -ForegroundColor White
    Write-Host "1. winget install --id Cloudflare.cloudflared" -ForegroundColor Green
    Write-Host "2. Download: https://github.com/cloudflare/cloudflared/releases" -ForegroundColor Green
    Write-Host ""
    $continue = Read-Host "Continue without tunnel? (y/n)"
    if ($continue -ne 'y') {
        exit
    }
} else {
    Write-Host "✅ Cloudflared found!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Starting Cloudflare Tunnel..." -ForegroundColor Cyan
    Write-Host ""
    
    # Start tunnel in background
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cloudflared tunnel --url http://localhost:8080" -WindowStyle Normal
    
    Write-Host "Tunnel started! Copy the URL from the new window." -ForegroundColor Green
    Write-Host ""
    $tunnelUrl = Read-Host "Paste your tunnel URL here (e.g., https://abc-123.trycloudflare.com)"
    
    if ($tunnelUrl) {
        Write-Host ""
        Write-Host "📋 Configuration URLs:" -ForegroundColor Yellow
        Write-Host "   App URL: $tunnelUrl" -ForegroundColor White
        Write-Host "   Redirect URL: $tunnelUrl/api/auth/callback" -ForegroundColor White
        Write-Host ""
        Write-Host "Copy these URLs to your Shopify App settings!" -ForegroundColor Cyan
        Write-Host ""
    }
}

Write-Host "Press Enter to start backend..." -ForegroundColor Yellow
Read-Host

Write-Host "Starting Spring Boot backend..." -ForegroundColor Cyan
Write-Host ""

# Start backend
Set-Location D:\Spring\custom-shopify
mvn spring-boot:run
