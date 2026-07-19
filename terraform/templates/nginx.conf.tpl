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
    #
    # Headers de seguridad solo aqui (no a nivel server): /api y /ws ya reciben
    # los suyos de Spring Security (X-Content-Type-Options, X-Frame-Options) y
    # de Kong (HSTS, CSP minimo via response-transformer en kong.yml) - ponerlos
    # tambien a nivel server duplicaria esos headers en las respuestas de la API.
    #
    # CSP: script-src sin unsafe-inline/eval (el bundle de Vite es un modulo
    # externo, sin scripts inline). style-src necesita unsafe-inline porque
    # Leaflet posiciona tiles/marcadores con estilos inline via JS, y ademas
    # fonts.googleapis.com por el @import de Google Fonts en index.css (los
    # .woff2 en si vienen de fonts.gstatic.com, de ahi font-src). img-src
    # blanquea los dos tile servers reales de MapView.tsx (CARTO basemaps y
    # GEBCO WMS de bathymetria) - sin esto el mapa se queda en blanco; blob:
    # habilita el preview de foto en MarkerFormModal (URL.createObjectURL antes
    # de mandarla a vision-service) - sin esto el preview queda en blanco y
    # silencioso, el navegador ni intenta la carga. connect-src
    # incluye el STUN de Google que usa useVoiceCall.ts para WebRTC (sin TURN
    # configurado); 'self' ya cubre /api y /ws same-origin.
    location / {
      add_header X-Content-Type-Options "nosniff" always;
      add_header X-Frame-Options "DENY" always;
      add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
      add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: blob: https://*.basemaps.cartocdn.com https://wms.gebco.net; connect-src 'self' stun:stun.l.google.com:19302; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self';" always;

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
