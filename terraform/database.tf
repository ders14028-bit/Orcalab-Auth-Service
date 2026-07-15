# ---------------------------------------------------------------------------
# Capa de datos gestionada — separada del cómputo.
# Todo en subnets privadas, cifrado en reposo y en tránsito,
# accesible únicamente desde el SG de las instancias EC2.
# ---------------------------------------------------------------------------

# --- RDS PostgreSQL: auth_db + room_db en la misma instancia -----------------
# RDS solo crea una DB inicial (auth_db); room_db la crea el user-data
# de las instancias de forma idempotente via psql.
resource "aws_db_subnet_group" "postgres" {
  name       = "${var.project_name}-postgres"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "${var.project_name}-postgres-subnets"
  }
}

resource "aws_db_instance" "postgres" {
  identifier     = "${var.project_name}-postgres"
  engine         = "postgres"
  engine_version = "16"
  instance_class = "db.t3.micro"

  allocated_storage = 20
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = "auth_db"
  username = var.db_master_username
  password = var.db_master_password

  db_subnet_group_name   = aws_db_subnet_group.postgres.name
  vpc_security_group_ids = [aws_security_group.data.id]
  publicly_accessible    = false
  multi_az               = false # fuera de alcance del lab; la HA se demuestra en la capa de computo

  skip_final_snapshot = true
  deletion_protection = false

  tags = {
    Name = "${var.project_name}-postgres"
  }
}

# --- MongoDB en EC2 dedicada: realtime_db + reporting_db ---------------------
# El Learner Lab deniega rds:CreateDBInstance para engine docdb (verificado
# contra la politica IAM real), asi que DocumentDB no es viable. Alternativa:
# instancia EC2 dedicada solo a MongoDB, en subnet privada, con autenticacion
# y accesible unicamente desde el SG de las instancias de la app.
resource "aws_instance" "mongo" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = var.mongo_instance_type
  subnet_id     = aws_subnet.private[0].id
  private_ip    = cidrhost(var.private_subnet_cidrs[0], 10) # IP fija: sobrevive recreaciones

  vpc_security_group_ids = [aws_security_group.mongo.id]

  user_data = base64encode(templatefile("${path.module}/templates/mongo_user_data.sh.tpl", {
    mongo_username = var.mongo_master_username
    mongo_password = var.mongo_master_password
  }))

  metadata_options {
    http_tokens = "required" # IMDSv2
  }

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
    encrypted   = true # cifrado en reposo, como en los servicios gestionados
  }

  tags = {
    Name = "${var.project_name}-mongo"
  }

  lifecycle {
    # data.aws_ami.ubuntu usa most_recent=true: sin esto, cualquier apply
    # posterior a que Canonical publique una AMI nueva reemplazaria esta
    # instancia (y su volumen con realtime_db/reporting_db) sin que el cambio
    # tenga nada que ver con lo que se esta aplicando. Esta instancia es
    # stateful (a diferencia de las del ASG) y no debe recrearse por drift de AMI.
    ignore_changes = [ami]
  }
}

# --- ElastiCache Redis: eventos room-events / realtime-events ----------------
# Se usa replication_group (y no cluster) porque es el recurso que soporta
# cifrado en transito; un solo nodo, sin replicas.
resource "aws_elasticache_subnet_group" "redis" {
  name       = "${var.project_name}-redis"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "${var.project_name}-redis-subnets"
  }
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${var.project_name}-redis"
  description          = "Redis de eventos de OrcaLab"

  engine         = "redis"
  engine_version = "7.1"
  node_type      = "cache.t3.micro"

  num_cache_clusters         = 1
  automatic_failover_enabled = false

  subnet_group_name  = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.data.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true

  tags = {
    Name = "${var.project_name}-redis"
  }
}
