# ---------------------------------------------------------------------------
# Bucket de staging privado del frontend.
#
# Desde el cambio a "una sola URL" (Nginx + Kong en la misma EC2 sirviendo
# todo detras del ALB), este bucket YA NO es el punto de entrada publico del
# front. Ahora es solo un intermediario privado: deploy-front.ps1 sube el
# build (dist/) aqui, y cada instancia del ASG lo sincroniza a
# /opt/orcalab/front-dist en el arranque (ver templates/user_data.sh.tpl) y
# via SSM RunCommand para redeploys sin recrear instancias
# (scripts/deploy-front.ps1). Nginx lo sirve desde ahi.
#
# Se mantiene el bucket (no se borra) por si se quiere revertir el enfoque;
# esta privado (block public access completo, sin bucket policy de lectura).
# ---------------------------------------------------------------------------

data "aws_caller_identity" "current" {}

resource "aws_s3_bucket" "front" {
  # El account id garantiza unicidad global del nombre
  bucket        = "${var.project_name}-front-${data.aws_caller_identity.current.account_id}"
  force_destroy = true # permite terraform destroy aunque tenga objetos

  tags = {
    Name = "${var.project_name}-front-staging"
  }
}

# Bucket privado: nada de acceso publico. Solo lo leen las instancias del ASG
# (via LabInstanceProfile) y quien despliega (via deploy-front.ps1).
resource "aws_s3_bucket_public_access_block" "front" {
  bucket = aws_s3_bucket.front.id

  block_public_acls       = true
  ignore_public_acls      = true
  block_public_policy     = true
  restrict_public_buckets = true
}
