# Compose de producción — Nginx (única entrada publica) + Kong + los 4 microservicios.
# Las bases de datos son servicios gestionados de AWS (RDS/DocumentDB/ElastiCache).
# Renderizado por Terraform (templatefile) e inyectado via user-data.

services:
  auth-service:
    image: ${ecr_registry}/orcalab/auth-service:latest
    container_name: auth-service
    restart: unless-stopped
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://${rds_endpoint}:5432/auth_db?sslmode=require
      DB_USERNAME: ${db_username}
      DB_PASSWORD: ${db_password}
      JWT_SECRET: ${jwt_secret}
      ADMIN_SEED_EMAIL: admin@orcalab.local
      ADMIN_SEED_PASSWORD: ${admin_seed_password}
      ADMIN_SEED_NOMBRE: Administrador OrcaLab
    networks: [orcalab-net]

  room-service:
    image: ${ecr_registry}/orcalab/room-service:latest
    container_name: room-service
    restart: unless-stopped
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://${rds_endpoint}:5432/room_db?sslmode=require
      DB_USERNAME: ${db_username}
      DB_PASSWORD: ${db_password}
      REDIS_HOST: ${redis_endpoint}
      REDIS_PORT: "6379"
      SPRING_DATA_REDIS_SSL_ENABLED: "true"
      JWT_SECRET: ${jwt_secret}
    networks: [orcalab-net]

  realtime-service:
    image: ${ecr_registry}/orcalab/realtime-service:latest
    container_name: realtime-service
    restart: unless-stopped
    environment:
      SPRING_DATA_MONGODB_URI: mongodb://${mongo_username}:${mongo_password}@${mongo_endpoint}:27017/realtime_db?authSource=admin
      REDIS_HOST: ${redis_endpoint}
      REDIS_PORT: "6379"
      SPRING_DATA_REDIS_SSL_ENABLED: "true"
      JWT_SECRET: ${jwt_secret}
      ROOM_SERVICE_URL: http://room-service:8082
      CORS_ALLOWED_ORIGINS: ${frontend_origin}
    networks: [orcalab-net]

  reporting-service:
    image: ${ecr_registry}/orcalab/reporting-service:latest
    container_name: reporting-service
    restart: unless-stopped
    environment:
      SPRING_DATA_MONGODB_URI: mongodb://${mongo_username}:${mongo_password}@${mongo_endpoint}:27017/reporting_db?authSource=admin
      REDIS_HOST: ${redis_endpoint}
      REDIS_PORT: "6379"
      SPRING_DATA_REDIS_SSL_ENABLED: "true"
      JWT_SECRET: ${jwt_secret}
      ROOM_SERVICE_URL: http://room-service:8082
      CORS_ALLOWED_ORIGINS: ${frontend_origin}
    networks: [orcalab-net]

  kong:
    image: kong:3.7
    container_name: kong
    restart: unless-stopped
    environment:
      KONG_DATABASE: "off"
      KONG_DECLARATIVE_CONFIG: /etc/kong/kong.yml
      KONG_PROXY_LISTEN: "0.0.0.0:8000"
      KONG_ADMIN_LISTEN: "127.0.0.1:8001" # admin API solo loopback en produccion
      KONG_LOG_LEVEL: info
    volumes:
      - /opt/orcalab/kong.yml:/etc/kong/kong.yml:ro
    # Sin "ports": ya no se publica al host. Solo nginx lo alcanza via
    # orcalab-net (nombre de servicio "kong", puerto 8000 interno).
    depends_on:
      - auth-service
      - room-service
      - realtime-service
      - reporting-service
    networks: [orcalab-net]

  # Unica entrada publica de la instancia: sirve el build de React y reenvia
  # /api/**, /ws y /health a Kong. Ver terraform/templates/nginx.conf.tpl.
  nginx:
    image: nginx:1.27-alpine
    container_name: nginx
    restart: unless-stopped
    volumes:
      - /opt/orcalab/nginx.conf:/etc/nginx/nginx.conf:ro
      - /opt/orcalab/front-dist:/usr/share/nginx/html:ro
    ports:
      - "80:80"
    depends_on:
      - kong
    networks: [orcalab-net]

networks:
  orcalab-net:
    driver: bridge
