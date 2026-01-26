# Quick update ngrok URL script

$newUrl = Read-Host "Enter new ngrok URL (e.g., https://abc123.ngrok-free.app)"

if ($newUrl) {
    Write-Host "Updating application.properties..." -ForegroundColor Cyan
    
    $propsFile = "D:\Spring\custom-shopify\src\main\resources\application.properties"
    $content = Get-Content $propsFile -Raw
    $content = $content -replace 'https://[a-z0-9]+\.ngrok-free\.app', $newUrl
    Set-Content $propsFile $content
    
    Write-Host "✅ Updated!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. Update Shopify App URLs:" -ForegroundColor White
    Write-Host "   App URL: $newUrl" -ForegroundColor Green
    Write-Host "   Redirect: $newUrl/api/auth/callback" -ForegroundColor Green
    Write-Host ""
    Write-Host "2. Restart backend (Ctrl+C and run again)" -ForegroundColor White
}
