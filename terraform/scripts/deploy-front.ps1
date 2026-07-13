# Compila el frontend y lo sube al bucket S3 de hosting estatico.
# Requisitos: credenciales AWS vigentes, terraform ya aplicado (bucket creado).
# Uso:  .\deploy-front.ps1

$ErrorActionPreference = "Stop"

$FrontDir  = Resolve-Path (Join-Path $PSScriptRoot "..\..\..\Orcalab-Front")
$TfDir     = Resolve-Path (Join-Path $PSScriptRoot "..")

aws sts get-caller-identity --query Account --output text | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Credenciales AWS invalidas o expiradas - refresca el Learner Lab." }

Push-Location $TfDir
try { $Bucket = terraform output -raw front_bucket_name } finally { Pop-Location }
if (-not $Bucket) { throw "No se pudo leer front_bucket_name - aplica terraform primero." }

Write-Host "=== Compilando el front (apunta al ALB via .env.production) ===" -ForegroundColor Cyan
Push-Location $FrontDir
try {
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "Fallo el build del front." }

    Write-Host "=== Subiendo dist/ a s3://$Bucket ===" -ForegroundColor Cyan
    aws s3 sync dist/ "s3://$Bucket" --delete
    if ($LASTEXITCODE -ne 0) { throw "Fallo el sync a S3." }
} finally {
    Pop-Location
}

Push-Location $TfDir
try { $Url = terraform output -raw front_website_url } finally { Pop-Location }
Write-Host "`nListo: $Url" -ForegroundColor Green
