# ---------------------------------------------------------------------------
# Hosting estático del frontend en S3.
# El build (Orcalab-Front/dist) se sube con scripts/deploy-front.ps1.
# ---------------------------------------------------------------------------

data "aws_caller_identity" "current" {}

resource "aws_s3_bucket" "front" {
  # El account id garantiza unicidad global del nombre
  bucket        = "${var.project_name}-front-${data.aws_caller_identity.current.account_id}"
  force_destroy = true # permite terraform destroy aunque tenga objetos

  tags = {
    Name = "${var.project_name}-front"
  }
}

resource "aws_s3_bucket_website_configuration" "front" {
  bucket = aws_s3_bucket.front.id

  index_document {
    suffix = "index.html"
  }

  # index.html tambien como error document: al recargar en una ruta profunda
  # de React Router (ej. /salas/3), S3 no encuentra el objeto y devuelve el
  # index, dejando que el router del cliente resuelva la ruta.
  error_document {
    key = "index.html"
  }
}

# El account-level block de acceso publico esta activo por defecto en buckets
# nuevos; hay que relajarlo en este bucket para poder adjuntar la politica de
# lectura publica de objetos.
resource "aws_s3_bucket_public_access_block" "front" {
  bucket = aws_s3_bucket.front.id

  block_public_acls       = true # ACLs siguen bloqueadas: solo se abre via politica
  ignore_public_acls      = true
  block_public_policy     = false
  restrict_public_buckets = false
}

# Minimo privilegio: solo s3:GetObject sobre los objetos (bucket/*),
# sin listar el bucket ni ninguna otra accion.
resource "aws_s3_bucket_policy" "front_public_read" {
  bucket = aws_s3_bucket.front.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "PublicReadObjects"
        Effect    = "Allow"
        Principal = "*"
        Action    = "s3:GetObject"
        Resource  = "${aws_s3_bucket.front.arn}/*"
      }
    ]
  })

  depends_on = [aws_s3_bucket_public_access_block.front]
}
