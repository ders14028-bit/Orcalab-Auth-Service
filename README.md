# OrcaLab — Backend

Plataforma colaborativa en tiempo real para investigación y conservación de orcas. Este repositorio contiene el backend, implementado como un conjunto de microservicios independientes en Java 17 / Spring Boot 3.5.

> Documento de referencia: *OrcaLab: Plataforma Colaborativa en Tiempo Real para Investigación y Conservación de Orcas* (propuesta de Inception, 17 historias de usuario en 5 épicas).

---

## 1. Arquitectura general

### 1.1 Diagrama completo (estado objetivo del sistema)

```
                                    ┌──────────┐
                                    │  Usuario │
                                    └────┬─────┘
                                         │ HTTPS
                                         ▼
                        ┌──────────────────────────────────┐
                        │   AWS Application Load Balancer   │   ◄── capa de infraestructura (nube)
                        │   (distribuye tráfico entre AZs,   │       basada en Lab de Alta Disponibilidad
                        │    health checks a nivel de red)   │
                        └────────────────┬───────────────────┘
                                         │
                    ┌────────────────────┼────────────────────┐
                    ▼                    ▼                    ▼
             ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
             │ EC2 / ASG   │      │ EC2 / ASG   │      │ EC2 / ASG   │   Auto Scaling Group:
             │ instancia 1 │      │ instancia 2 │      │ instancia N │   agrega/quita instancias
             │ (Kong)      │      │ (Kong)      │      │ (Kong)      │   según CPU/carga
             └──────┬──────┘      └──────┬──────┘      └──────┬──────┘
                    └────────────────────┼────────────────────┘
                                         ▼
                        ┌─────────────────────────────────┐
                        │        Kong API Gateway          │
                        │  · Enrutamiento por path          │
                        │  · Balanceo entre réplicas         │
                        │    de cada microservicio           │
                        │    (upstreams + health checks)     │
                        │  · Validación de JWT en el borde   │  ◄── pendiente de implementar
                        │  · Rate limiting                   │
                        │  · TLS termination (HTTPS)         │
                        └───┬───────┬───────┬───────┬───────┘
                            │       │       │       │
             ┌──────────────┘       │       │       └──────────────┐
             │              ┌───────┘       └───────┐              │
             ▼              ▼                        ▼              ▼
     ┌───────────────┐┌───────────────┐     ┌────────────────┐┌───────────────┐
     │ auth-service  ││ room-service  │     │realtime-service││reporting-svc  │
     │ HU-01,02,03   ││ HU-04,05      │     │HU-06,07,08*,   ││HU-11,12,13*,  │
     │               ││               │     │09,10           ││16*            │
     │ REST :8081    ││ REST :8082    │     │REST+WS :8083   ││REST :8084     │
     │ [réplicas N]  ││ [réplicas N]  │     │[réplicas N]    ││[réplicas N]   │
     └───────┬───────┘└───────┬───────┘     └───┬───────┬────┘└───────┬───────┘
             │ JDBC            │ JDBC             │       │             │
             ▼                 ▼                  │       │             │
     ┌───────────────┐ ┌───────────────┐           │       │             │
     │  PostgreSQL   │ │  PostgreSQL   │           │       │             │
     │   auth_db     │ │   room_db     │           │       │             │
     └───────────────┘ └───────┬───────┘           │       │             │
                                │                   │       │             │
                    Publica eventos                │       │             │
                     (room-events)                 │       │             │
                                │                   ▼       │             │
                                │           ┌───────────────┐│            │
                                │           │   MongoDB     ││            │
                                │           │  realtime_db  ││            │
                                │           │ (mensajes,    ││            │
                                │           │  marcadores,  ││            │
                                │           │  rutas,       ││            │
                                │           │  alertas)     ││            │
                                │           └───────────────┘│            │
                                │                             │ Publica    │
                                │                             │ eventos    │
                                │                             │(realtime-  │
                                │                             │ events)    │
                                ▼                             ▼            │
                        ┌───────────────────────────────────────┐         │
                        │              REDIS                    │         │
                        │  Streams: room-events, realtime-events │         │
                        │  (persistencia de eventos + replay)    │         │
                        └───────┬───────────────────┬───────────┘         │
                                │ Consume              │ Consume            │
                                │ room-events          │ realtime-events    │
                                └───────────┬───────────┘                   │
                                            ▼                               │
                                  (reporting-service consume ambos)  ◄──────┘
                                            │
                                            ▼
                                   ┌───────────────┐
                                   │    MongoDB    │
                                   │ reporting_db  │
                                   │ (salas,       │
                                   │ observaciones,│
                                   │ feed actividad)│
                                   └───────────────┘

     ─────────────────────── Observabilidad (transversal) ───────────────────────

     auth-service ─┐
     room-service ─┤
  realtime-service ─┼── /actuator/prometheus ──► Prometheus (:9090) ──┐
 reporting-service ─┘                                                  │
                                                                        ▼
     auth-service ─┐                                              ┌─────────┐
     room-service ─┤                                              │ Grafana │
  realtime-service ─┼── logs de archivo ──► Promtail ──► Loki ────►│ (:3000) │
 reporting-service ─┘                       (:3100)                │dashboard│
                                                                    └─────────┘

* HU-08, HU-13, HU-16 aún no implementadas
```

### 1.2 Resumen de puertos y bases de datos

> **Nota (despliegue AWS):** en producción, cada instancia del ASG corre además
> **Nginx en el puerto 80**, delante de Kong, como única entrada publica de la
> instancia (ver sección "Una sola URL: Nginx + Kong en la misma instancia" más
> abajo). Kong deja de exponerse directamente al ALB.

| Componente | Puerto | Base de datos propia | Puerto BD |
|---|---|---|---|
| Nginx (front + proxy, solo en AWS) | 80 | — | — |
| Kong (API Gateway) | 8000 (proxy, interno) / 8001 (admin) | — (o Postgres/DB-less) | — |
| `auth-service` | 8081 | PostgreSQL `auth_db` | 5433 |
| `room-service` | 8082 | PostgreSQL `room_db` | 5434 |
| `realtime-service` | 8083 (REST + WS `/ws`) | MongoDB `realtime_db` | 27019 |
| `reporting-service` | 8084 | MongoDB `reporting_db` | 27020 |
| Redis (eventos) | 6379 | — | — |
| Prometheus | 9090 | — | — |
| Loki | 3100 | — | — |
| Grafana | 3000 | — | — |

### 1.3 Regla de oro: nadie lee la base de datos de otro

Ningún microservicio tiene en su configuración una cadena de conexión a la base de datos de otro servicio. La única forma de enterarse de cambios en otro dominio es **consumir eventos de Redis Streams**. Esto es lo que permite que, más adelante, cada servicio pueda escalarse a múltiples réplicas de forma independiente sin generar inconsistencias de datos entre sí.

### Principio arquitectónico central: independencia de datos

Cada microservicio tiene **su propia base de datos** y **nadie más se conecta a ella directamente**. Cuando un servicio necesita datos que "pertenecen" a otro, no lee su base de datos: escucha **eventos de dominio** publicados en Redis Streams y mantiene su propia copia local (o su propio estado en memoria) de lo que necesita.

Esto se decidió explícitamente al inicio del proyecto para evitar el anti-patrón de "lecturas cruzadas" entre servicios, que rompe el encapsulamiento y crea acoplamiento oculto entre bases de datos.

| Servicio | Publica en | Consume de |
|---|---|---|
| `room-service` | `room-events` | — |
| `realtime-service` | `realtime-events` | `room-events` |
| `reporting-service` | — | `room-events`, `realtime-events` |

### Alta disponibilidad (diseño planeado)

El plan de HA se apoya en **Kong** como API Gateway/balanceador de carga frente a réplicas de cada microservicio (upstreams con health checks), en vez de una sola instancia expuesta directamente. Esto queda pendiente de implementar; el diseño de independencia de datos ya construido es el prerrequisito necesario para que el balanceo entre réplicas no genere inconsistencias.

---

## 2. Microservicios y su alcance (Historias de Usuario)

### `auth-service` — Épica 1: Gestión de Usuarios y Seguridad
- **Puerto:** 8081 · **BD:** PostgreSQL (`auth_db`)
- HU-01 Registro de usuario
- HU-02 Inicio de sesión (JWT)
- HU-03 Gestión de roles y RBAC

**Roles globales implementados:** `ADMINISTRADOR`, `INVESTIGADOR` (el rol "Público" del documento de Inception se descartó; el proyecto se limita a uso educativo/institucional).

**Estado: ✅ Completo y probado end-to-end** (registro, login, JWT, manejo de errores).

---

### `room-service` — Épica 2: Gestión de Salas de Investigación
- **Puerto:** 8082 · **BD:** PostgreSQL (`room_db`)
- HU-04 Creación de sala de investigación
- HU-05 Unirse a una sala existente
- HU-06 *(presencia se implementa en realtime-service, ver abajo)*

**Roles locales por sala (no van en el JWT, viven en `room-service`):** `LIDER`, `MIEMBRO` — patrón similar a Discord. El creador de una sala queda como `LIDER` automáticamente y puede promover a otros miembros.

**Funcionalidad implementada:**
- Crear sala, listar mis salas, ver detalle
- Unirse / salir de una sala
- Expulsar miembro (solo LIDER)
- Cambiar rol de un miembro (solo LIDER)
- Regla de negocio: no se puede dejar una sala sin ningún LIDER
- Publica eventos de dominio: `SalaCreada`, `UsuarioSeUnioASala`, `MiembroSalioDeSala`, `RolDeMiembroCambiado`

**Estado: ✅ Completo y probado end-to-end**, incluyendo permisos y publicación de eventos verificada directamente en Redis Streams.

---

### `realtime-service` — Épica 3: Colaboración en Tiempo Real
- **Puerto:** 8083 · **BD:** MongoDB (`realtime_db`) + Redis (eventos y estado)
- HU-06 Presencia de usuarios en sala
- HU-07 Mapa colaborativo en tiempo real
- HU-08 Línea temporal sincronizada — **pendiente** (ver sección 4)
- HU-09 Chat contextual
- HU-10 Alertas en tiempo real

**Mecanismo de transporte:** WebSocket + STOMP (`/ws`), autenticado con el mismo JWT emitido por `auth-service`, validado en el handshake de conexión.

**Funcionalidad implementada:**
- **Presencia (HU-06):** al conectarse a una sala, el usuario aparece en la lista de presentes con su rol (LIDER/MIEMBRO, obtenido consumiendo eventos de `room-service`, sin leer su base de datos). Al desconectarse, se retira automáticamente (verificado en milisegundos).
- **Chat contextual (HU-09):** mensajes en tiempo real, persistidos en MongoDB propio, con historial disponible vía REST, y soporte para vincular un mensaje a un marcador del mapa.
- **Mapa colaborativo (HU-07):** agregar y editar marcadores (avistamientos, zonas de interés), trazar rutas de migración, todo sincronizado en tiempo real vía WebSocket, con registro de autoría (quién creó/editó cada elemento y cuándo).
- **Alertas (HU-10):** se generan automáticamente cuando se crea un marcador de tipo `CRITICO`, notificando a todos los presentes en la sala y quedando persistidas con historial consultable.
- Publica eventos enriquecidos (con datos completos, no solo IDs) hacia `realtime-events`: `MarcadorAgregado`, `MarcadorEditado`, `RutaTrazada`, `AlertaGenerada`, `MensajeEnviado`.

**Estado: ✅ HU-06, HU-07, HU-09, HU-10 completas y probadas end-to-end** con cliente de prueba WebSocket. HU-08 (línea temporal, modo seguir/libre) se decidió posponer para el final por ser la de mayor complejidad relativa y no bloquear a los demás servicios.

---

### `reporting-service` — Épica 4: Panel y Reportes
- **Puerto:** 8084 · **BD:** MongoDB (`reporting_db`)
- HU-11 Panel general de actividad
- HU-12 Consulta de observaciones registradas
- HU-13 Generación de reportes (PDF/CSV) — **pendiente**
- HU-16 KPIs de negocio — **pendiente** (reclasificada aquí desde Observabilidad, ya que depende de datos de negocio que este servicio ya agrega)

**Funcionalidad implementada:**
- Dos consumidores de eventos independientes (uno para `room-events`, otro para `realtime-events`) que alimentan colecciones propias: `Sala`, `Observacion`, `EventoActividad`.
- Panel general con conteo de investigaciones activas, total de observaciones y feed de eventos recientes.
- Consulta de observaciones por sala, con filtro opcional por tipo.

**Estado: ✅ HU-11 y HU-12 completas y probadas end-to-end**, incluyendo consumo retroactivo de eventos históricos desde Redis Streams (persistencia de stream verificada tras reinicio del servicio).

---

### `observability-service` — Épica 5: Observabilidad
- **No es un servicio Spring Boot propio**: es la configuración de infraestructura (Prometheus, Loki, Promtail, Grafana) que instrumenta a los 4 servicios anteriores.
- HU-14 Registro de logs estructurados
- HU-15 Métricas técnicas (concurrencia y rendimiento)
- HU-17 Dashboard de métricas — parcialmente (falta panel visual dedicado, la consulta vía Grafana Explore ya funciona)

**Stack:**
- **Prometheus** scrapea `/actuator/prometheus` de los 4 servicios (Micrometer ya integrado en cada uno)
- **Loki + Promtail** centralizan los logs de archivo de cada servicio
- **Grafana** consolida ambas fuentes, con datasources provisionados automáticamente

**Estado: ✅ Funcionando end-to-end** — los 4 targets de Prometheus reportan `UP`, y se verificó una consulta real de logs de `auth-service` vía Grafana Explore.

---

## 3. Cómo levantar el entorno de desarrollo

### Prerrequisitos
- Java 17, Maven, Docker Desktop corriendo

### 1. Levantar la infraestructura (bases de datos, Redis)

```powershell
docker start orcalab-auth-db orcalab-room-db orcalab-redis orcalab-realtime-mongo orcalab-reporting-mongo
```

Si los contenedores no existen aún, créalos:

```powershell
docker run --name orcalab-auth-db -e POSTGRES_DB=auth_db -e POSTGRES_USER=auth_user -e POSTGRES_PASSWORD=auth_password -p 5433:5432 -d postgres:16
docker run --name orcalab-room-db -e POSTGRES_DB=room_db -e POSTGRES_USER=room_user -e POSTGRES_PASSWORD=room_password -p 5434:5432 -d postgres:16
docker run --name orcalab-redis -p 6379:6379 -d redis:7
docker run --name orcalab-realtime-mongo -p 27019:27017 -d mongo:7
docker run --name orcalab-reporting-mongo -p 27020:27017 -d mongo:7
```

### 2. Levantar cada microservicio (una terminal por servicio)

```powershell
cd auth-service        && mvn spring-boot:run   # puerto 8081
cd room-service         && mvn spring-boot:run   # puerto 8082
cd realtime-service      && mvn spring-boot:run   # puerto 8083
cd reporting-service     && mvn spring-boot:run   # puerto 8084
```

### 3. Levantar el stack de observabilidad

```powershell
cd observability-service
docker-compose up -d
```

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)

### 4. Probar el flujo básico (PowerShell)

```powershell
# Registro
Invoke-RestMethod -Method POST -Uri http://localhost:8081/api/auth/registro -ContentType "application/json" `
  -Body '{"email":"admin@orcalab.edu","password":"password123","nombre":"Admin","rol":"ADMINISTRADOR"}'

# Login
$login = Invoke-RestMethod -Method POST -Uri http://localhost:8081/api/auth/login -ContentType "application/json" `
  -Body '{"email":"admin@orcalab.edu","password":"password123"}'
$headers = @{ Authorization = "Bearer $($login.token)" }

# Crear sala
Invoke-RestMethod -Method POST -Uri http://localhost:8082/api/salas -ContentType "application/json" -Headers $headers `
  -Body '{"nombre":"Investigacion Orcas Pacifico","descripcion":"Sala de monitoreo"}'

# Ver panel de reporting
Invoke-RestMethod -Method GET -Uri http://localhost:8084/api/reportes/panel -Headers $headers
```

Para probar WebSocket (presencia, chat, mapa) se usa un cliente STOMP de prueba (`test-presence.html`, incluido en el repo/entorno de desarrollo) que se conecta a `ws://localhost:8083/ws` con el token JWT como header `Authorization` en el frame `CONNECT`.

---

## 4. Pendientes

| Ítem | Prioridad (MoSCoW) | Notas |
|---|---|---|
| HU-08 Línea temporal sincronizada | Must | Pospuesta: mayor complejidad relativa (modo seguir/libre, ≤300ms), no bloquea otros servicios |
| HU-13 Reportes PDF/CSV | Could | Baja prioridad según documento de Inception |
| HU-16 KPIs de negocio | Should | Depende de tener suficiente volumen de datos agregados en reporting-service |
| Kong (API Gateway + balanceo) | — | Diseño de independencia de datos ya construido como prerrequisito |
| `docker-compose.yml` unificado (todos los servicios) | — | Actualmente cada servicio corre vía `mvn spring-boot:run` en la máquina host; falta dockerizarlos |
| Frontend | — | Backend primero, por decisión explícita, para evitar recablear contratos de API a medio camino |

---

## Limitación conocida: realtime-service con múltiples réplicas

**Síntoma:** con 2+ instancias del backend detrás del ALB, dos usuarios de la misma
sala enrutados a instancias distintas no se ven mutuamente (presencia), no reciben
los mensajes/marcadores/cursores/alertas del otro, y la señalización de voz falla —
con comportamiento inconsistente según a qué instancia enrute el balanceador.

**Causa raíz:** realtime-service usa `enableSimpleBroker` (broker STOMP **en
memoria, local a cada instancia**). Cada `convertAndSend` solo llega a los clientes
WebSocket conectados a esa misma instancia; no hay sincronización entre réplicas.
Los eventos de membresía/presencia sí se propagan (todas las instancias consumen el
stream `room-events` de Redis y difunden localmente), pero los broadcasts que nacen
en controllers (chat, mapa, voz) no.

**Mitigación actual:** el ASG está fijado a **1 réplica** (min=1/max=1/desired=1 en
`terraform/variables.tf`) para garantizar consistencia del tiempo real en demo.
Los valores de diseño (min=2/max=4) quedan comentados ahí mismo.

**Fix real (pendiente):** migrar de `enableSimpleBroker` a un relay STOMP externo
compartido — broker RabbitMQ vía `enableStompBrokerRelay`, o un relay propio sobre
Redis pub/sub que reemplace los `convertAndSend` directos y reenvíe cada broadcast
a todas las instancias para que cada una lo entregue a sus clientes locales.

---

## Arquitectura de despliegue: una sola URL (Nginx + Kong en la misma instancia)

**Por qué se hizo:** el profesor exige que el proyecto se acceda mediante **una
sola URL**, no dos separadas. Antes de este cambio, el front vivía en un bucket
S3 (endpoint REST, `https://orcalab-front-<cuenta>.s3.us-east-1.amazonaws.com`)
y el backend en el ALB (`https://orcalab-alb-....elb.amazonaws.com`) — dos
orígenes distintos.

**Qué cambió:** cada instancia del Auto Scaling Group ahora corre, además de
Kong y los 4 microservicios, un contenedor **Nginx** (`terraform/templates/nginx.conf.tpl`)
que es la única entrada publica de la instancia (puerto 80, detrás del ALB):

- `/` y los assets estáticos → sirve el build de React (`Orcalab-Front/dist`,
  sincronizado desde un bucket S3 de **staging privado** a
  `/opt/orcalab/front-dist` en el arranque, ver `terraform/templates/user_data.sh.tpl`).
- `/api/**`, `/ws`, `/health` → `proxy_pass` a Kong (`localhost:8000` dentro del
  bridge de docker de la instancia; Kong ya no se publica directamente al host
  ni al ALB).

El bucket S3 del front (`front_bucket_name` en outputs) **se mantiene**, pero
pasó de sitio público a staging intermedio: `terraform/scripts/deploy-front.ps1`
sube el build ahí y luego redespliega en caliente vía `aws ssm send-command`
sobre las instancias vivas del ASG (re-sync + `docker restart nginx`), sin
esperar a que se recreen instancias.

**Por qué esta opción y no otras:**
- **CloudFront** (daría HTTPS + una sola URL fácilmente vía `*.cloudfront.net`)
  está bloqueado por IAM en este Learner Lab — confirmado con error real, no
  solo simulación.
- **Route53** (dominio propio + un solo registro DNS apuntando al ALB) se dejó
  fuera de alcance por ahora — no se persiguió un dominio real ni una validación
  DNS/email de ACM para esta iteración.
- **DocumentDB** bloqueado (deny explícito de IAM) es la razón por la que Mongo
  vive en EC2 propia — no relacionado directamente con esta decisión, pero es
  la misma familia de restricciones del Learner Lab que fuerza soluciones "a
  mano" en vez de servicios gestionados.
- **Kong no tiene serving estático nativo en su edición open-source** (DB-less);
  hacerlo con un plugin custom en Lua era más complejo que agregar un sidecar
  Nginx, que es un patrón estándar y bien entendido.

**Trade-offs aceptados conscientemente:**
1. **El certificado autofirmado ahora cubre todo el sitio**, no solo el
   backend: cualquier evaluador debe aceptar la advertencia "no seguro" del
   navegador **una vez** al visitar `https://<alb-dns-name>/` (Avanzado →
   Continuar de todos modos) antes de que cargue nada — front, API y WebSocket
   comparten el mismo origen y el mismo certificado. Antes este paso solo
   afectaba al backend (el front en S3 tenía certificado válido de Amazon).
2. **Se pierde la separación estático/dinámico**: si Kong o alguno de los
   microservicios se cae, Nginx sigue respondiendo pero las llamadas a `/api/**`
   y `/ws` fallarán — ya no hay independencia entre "el front sigue arriba" y
   "el backend está caído", como sí la había cuando el front vivía en S3
   (servicio gestionado, separado del ALB/EC2 por completo).
3. Como beneficio colateral: al compartir origen, **desaparece el problema de
   CORS** entre front y backend (`Orcalab-Front/.env.production` ya no fija
   `VITE_API_BASE_URL`; las llamadas usan rutas relativas resueltas por Nginx).
   El fallback `try_files ... /index.html` de Nginx también deja sin efecto el
   viejo problema de recargar en rutas profundas de React Router, sea cual sea
   el router usado por el front.

---

## 5. Decisiones de diseño relevantes

- **Roles simplificados a 2 (Admin/Investigador) en vez de 4**, dado el enfoque educativo/institucional del proyecto (se descartaron "Público" y "Organización" del documento de Inception original).
- **Sin `pom.xml` padre / monorepo Maven multi-módulo**: cada microservicio es un proyecto Spring Boot standalone, para minimizar riesgo de acoplamiento y mantener la curva de aprendizaje simple.
- **Sin módulo compartido de DTOs/eventos entre servicios**: cada servicio mantiene su propia copia de las clases que representan eventos que consume, evitando dependencias Java cruzadas entre microservicios.
- **Redis Streams (no Pub/Sub simple)** para eventos: permite reproceso retroactivo del historial completo (verificado: al reiniciar `reporting-service`, reconstruyó su estado completo re-leyendo eventos desde el inicio del stream).