# Nginx sirve el front estatico (React build) y hace de unica entrada publica
# de la instancia: todo lo que no es / se reenvia a Kong (localhost:8000 dentro
# del bridge de docker) sin modificar el path, para respetar strip_path:false
# de las rutas en api-gateway/kong.yml.
# Renderizado por Terraform (templatefile) e inyectado via user-data, igual que kong.yml.

worker_processes 1;

events {
  worker_connections 1024;
}

http {
  include       /etc/nginx/mime.types;
  default_type  application/octet-stream;
  sendfile      on;

  server {
    listen 80;
    server_name _;

    # Build de React (Orcalab-Front/dist), sincronizado desde S3 en el arranque.
    root /usr/share/nginx/html;
    index index.html;

    # Fallback a index.html: aunque el front usa HashRouter (las rutas van
    # despues del #, el servidor nunca las ve), se deja como red de seguridad
    # ante un archivo estatico no encontrado.
    location / {
      try_files $uri $uri/ /index.html;
    }

    # Rutas REST de los 4 microservicios (auth, salas, canales, marcadores,
    # rutas, alertas, reportes) - todas bajo /api/** en kong.yml.
    location /api/ {
      proxy_pass http://kong:8000;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket/STOMP de realtime-service (incluye sub-rutas de fallback SockJS,
    # ej. /ws/info, /ws/195/abc/websocket).
    location /ws {
      proxy_pass http://kong:8000;
      proxy_http_version 1.1;
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection "upgrade";
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
      proxy_read_timeout 3600s;
      proxy_send_timeout 3600s;
    }

    # Health check del ALB -> Kong -> actuator/health de auth-service.
    location /health {
      proxy_pass http://kong:8000;
      proxy_set_header Host $host;
    }
  }
}
