# Compila el frontend, lo sube al bucket S3 de staging privado, y redespliega
# en las instancias vivas del ASG via SSM (sin recrear instancias).
#
# Arquitectura (post "una sola URL"): Nginx + Kong corren en la misma EC2
# detras del ALB. El front ya no se sirve desde S3 publico: S3 es solo un
# staging intermedio. Cada instancia sincroniza s3://bucket -> /opt/orcalab/front-dist
# en el arranque (user_data.sh.tpl); este script hace lo mismo en caliente
# sobre las instancias ya corriendo, via SSM RunCommand, y reinicia nginx.
#
# Requisitos: credenciales AWS vigentes, terraform ya aplicado (bucket + ASG creados),
# SSM Agent en las instancias (via LabInstanceProfile).
# Uso:  .\deploy-front.ps1

$ErrorActionPreference = "Stop"

$FrontDir = Resolve-Path (Join-Path $PSScriptRoot "..\..\..\Orcalab-Front")
$TfDir    = Resolve-Path (Join-Path $PSScriptRoot "..")

aws sts get-caller-identity --query Account --output text | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Credenciales AWS invalidas o expiradas - refresca el Learner Lab." }

Push-Location $TfDir
try {
    $Bucket  = terraform output -raw front_bucket_name
    $AsgName = terraform output -raw asg_name
} finally { Pop-Location }
if (-not $Bucket)  { throw "No se pudo leer front_bucket_name - aplica terraform primero." }
if (-not $AsgName) { throw "No se pudo leer asg_name - aplica terraform primero." }

Write-Host "=== Compilando el front (rutas relativas, mismo origen que el ALB) ===" -ForegroundColor Cyan
Push-Location $FrontDir
try {
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "Fallo el build del front." }

    Write-Host "=== Subiendo dist/ a s3://$Bucket (staging privado) ===" -ForegroundColor Cyan
    aws s3 sync dist/ "s3://$Bucket" --delete
    if ($LASTEXITCODE -ne 0) { throw "Fallo el sync a S3." }
} finally {
    Pop-Location
}

Write-Host "=== Redesplegando en las instancias vivas del ASG ($AsgName) via SSM ===" -ForegroundColor Cyan
$InstanceIds = aws autoscaling describe-auto-scaling-groups `
    --auto-scaling-group-names $AsgName `
    --query "AutoScalingGroups[0].Instances[?LifecycleState=='InService'].InstanceId" `
    --output text
if ($LASTEXITCODE -ne 0) { throw "Fallo al listar instancias del ASG." }

$InstanceIds = ($InstanceIds -split "\s+") | Where-Object { $_ }
if (-not $InstanceIds -or $InstanceIds.Count -eq 0) {
    Write-Host "Sin instancias InService todavia - el proximo boot ya toma el build nuevo desde S3." -ForegroundColor Yellow
} else {
    # rm -rf primero: "aws s3 sync" compara por tamano+fecha, no por contenido -
    # si un archivo nuevo cae en el mismo tamano que el anterior (ej. mismo largo
    # de hash en el nombre del bundle), puede saltarse la descarga y dejar
    # contenido viejo servido. Borrar y resincronizar desde cero lo evita.
    $syncCmd = "rm -rf /opt/orcalab/front-dist/* && aws s3 sync s3://$Bucket /opt/orcalab/front-dist --delete && docker restart nginx"
    $CommandId = aws ssm send-command `
        --instance-ids $InstanceIds `
        --document-name "AWS-RunShellScript" `
        --parameters "commands=[`"$syncCmd`"]" `
        --query "Command.CommandId" --output text
    if ($LASTEXITCODE -ne 0) { throw "Fallo al enviar el comando SSM - revisa que SSM Agent este activo en las instancias." }
    Write-Host "Comando SSM enviado (CommandId: $CommandId) a: $($InstanceIds -join ', ')" -ForegroundColor Green
}

Push-Location $TfDir
try { $Url = terraform output -raw alb_dns_name } finally { Pop-Location }
Write-Host "`nListo: https://$Url" -ForegroundColor Green
