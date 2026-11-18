# RoundUp

**Video:** https://1drv.ms/v/c/f892dba9026f7074/IQCG2U3I5jzCRauk9mzbotefAW5irobe4UUb-5UBaFqdkG4?e=hhwMWa
API REST de ahorro automático por redondeo: cada compra del cliente se redondea al dólar siguiente
y la diferencia (multiplicada según su plan) se abona a su meta de ahorro. Con login, roles y
bitácora de cada operación.

Proyecto integrador del Programa Digital Trainee 2026 · Nestor Leonel Martinez Valencia.
El diseño (reglas, diagrama entidad-relación y diagrama de clases) está en [`../etapa-1/`](../etapa-1/README.md).

**Tecnologías:** Java 21 · Spring Boot 4.1 · Spring Data JPA · Spring Security con JWT · PostgreSQL 17 ·
Swagger · JUnit 5, Mockito y MockMvc · Docker Compose.

---

## Levantarlo desde cero

Solo necesita Docker.

**1. Cree el archivo `.env`** en esta carpeta (`proyecto/api/`), junto a `docker-compose.yml`, con una
clave de 32 caracteres o más. Créelo desde el editor, no con `>` en PowerShell. Puede partir de
`.env.ejemplo`:

```
JWT_CLAVE=una-clave-larga-de-32-caracteres-o-mas-solo-para-su-maquina
```

**2. Levante la API y la base:**

```
docker compose up --build -d
docker compose logs -f api
```

La primera vez tarda más (baja Maven y las dependencias). Está lista cuando aparece
`Started ApiApplication`.

**3. Abra Swagger:** http://localhost:8080/swagger-ui.html

**4. Para terminar:**

```
docker compose down      # apaga, y conserva el volumen
docker compose down -v   # apaga, y borra también el volumen
```

> Cada vez que la API arranca, la base vuelve a los datos de `import.sql`, porque `ddl-auto` está en `create`.
> La base de Compose se puede mirar con DBeaver en `localhost:5436` (base `roundup`, usuario `roundup`, clave `roundup123`).

### Sin Docker Compose (desde IntelliJ)

Necesita PostgreSQL en `localhost:5432` con la base `roundup` (usuario `banco`, clave `banco123`, el
contenedor `banco-db` del curso) y la variable de entorno `JWT_CLAVE` en la Run Configuration. La
dirección de la base se puede cambiar con las variables `DB_URL`, `DB_USUARIO` y `DB_CLAVE`.

---

## Usuarios de prueba

| Usuario | Clave | Rol | Qué puede hacer |
| :--- | :--- | :--- | :--- |
| `admin` | `Admin2026` | ADMIN | Consultar, y crear, editar y desactivar comercios |
| `mlopez` | `Asesor2026` | ASESOR | Consultar, y abrir, editar (plan o estado) y cerrar cuentas |
| `ana` | `Cliente2026` | CLIENTE | Consultar, comprar y crear metas de ahorro |
| `luis` | `Cliente2026` | CLIENTE | Igual que `ana` |

Las claves se guardan en BCrypt en `import.sql`; nunca la clave tal cual.

**Para probar en Swagger:** use `POST /api/auth/login`, copie el `token`, haga clic en **Authorize** y
péguelo sin la palabra `Bearer`.

### Los datos para probar

| Cuenta | De | Plan | Para qué sirve |
| :--- | :--- | :--- | :--- |
| `RU-000123` | Ana | DOBLE (x2, tope $50/mes) | El caso bueno: tiene la meta ACTIVA "Laptop" |
| `RU-000124` | Luis | BASICO | Saldo de $3.20: para R1, saldo insuficiente |
| `RU-000125` | Rosa | TRIPLE | BLOQUEADA: para R4 |
| `RU-000126` | Ana | BASICO | Sin meta ACTIVA: para R6, se cobra sin redondeo |

Comercios: `SUPER-SELECTOS`, `FARMACIA-SN` y `GASOLINERA-PUMA` (activos) y `TIENDA-CERRADA` (inactivo, para R4).

---

## La operación principal: procesar una compra

`POST /api/compras` cobra la compra más su redondeo y abona el redondeo a la meta ACTIVA de la cuenta.
Es **todo o nada** (`@Transactional`): la compra, el saldo, la meta y la fila de la bitácora. Quien
compra **sale del token**, nunca del JSON.

```json
{ "numeroCuenta": "RU-000123", "codigoComercio": "SUPER-SELECTOS", "monto": 4.30 }
```

Responde **201**: $4.30 en plan DOBLE → redondeo (5.00 − 4.30) × 2 = **$1.40**, se cobran $5.70 y la
meta Laptop sube $1.40.

### Las reglas

| Regla | Qué dice | Dónde se cumple | Si no se cumple |
| :--- | :--- | :--- | :--- |
| R1 | El saldo tiene que alcanzar para el monto **más** el redondeo | Servicio | 400 · "Saldo insuficiente: la compra necesita $5.70 y la cuenta RU-000124 tiene $3.20" |
| R2 | Los redondeos del mes no superan el `tope_mensual` del plan | Servicio (`@Query` que suma los del mes) | La compra pasa, pero el redondeo se recorta a lo que falta |
| R3 | Redondeo = (siguiente dólar − monto) × multiplicador; un monto exacto no redondea | Servicio | — |
| R4 | Solo se compra desde una cuenta ACTIVA y en un comercio activo | Servicio | 400 · "La cuenta RU-000125 esta BLOQUEADA" |
| R5 | Una cuenta tiene como máximo una meta ACTIVA | Servicio, al crear la meta | 400 |
| R6 | El redondeo solo va a una meta ACTIVA; al llegar al objetivo, la meta queda COMPLETADA | Servicio | Sin meta ACTIVA, se cobra sin redondeo |
| R8 | No se repiten DUI, número de cuenta, comprobante, usuario ni código de comercio | Tabla (`UNIQUE`) y servicio | 400 o 409 |
| R9 | Montos mayores que cero; saldos y redondeos nunca negativos | DTO (`@Valid`) y servicio | 400 |
| R10 | Saldo = saldo de apertura − (monto + redondeo) de sus compras | Servicio | Se deshace todo |
| R11 | Lo ahorrado en la meta = la suma de sus redondeos | Servicio | Se deshace todo |
| — | Si la compra falla, no cambia nada, pero el intento queda en la bitácora como FALLIDO | `@Transactional` y `BitacoraServicio` con `REQUIRES_NEW` | Se deshace y responde el error |
| — | La cuenta, el comercio y el usuario tienen que existir | Servicio | 400 |
| — | El CLIENTE solo compra y crea metas en **sus** cuentas: el dueño de la cuenta (cuenta → cliente → usuario) es quien dice el token | Servicio (`cuenta.esDe(username)`) | 400 · "La cuenta RU-000124 no es de ana" |

El número de comprobante sale de una secuencia de la base (`compra_comprobante_seq`), y la cuenta se
lee con `SELECT ... FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`): dos compras simultáneas en la misma
cuenta no pierden saldo.

---

## Los endpoints

Sin token, todo responde **401**, salvo el login y Swagger. Con un rol sin permiso, **403**. Los
errores responden con `ProblemDetail` (`title`, `status`, `detail`, `instance`).

La seguridad tiene dos puertas: las reglas por ruta de `ConfiguracionSeguridad` y `@PreAuthorize` en
la operación principal y en lo delicado.

| Método y ruta | Qué hace | Quién | Responde |
| :--- | :--- | :--- | :--- |
| `POST /api/auth/login` | Entrega el token (vence en 1 hora) | Abierto | 200 · 400 · 401 |
| `GET /api/auth/yo` | Quién soy, según el token | Con token | 200 |
| `POST /api/compras` | **Procesa una compra** | CLIENTE | 201 · 400 |
| `GET /api/cuentas` | Lista las cuentas | Con token | 200 |
| `GET /api/cuentas/{numero}` | Una cuenta | Con token | 200 · 404 |
| `GET /api/cuentas/{numero}/compras` | Sus compras, la más nueva primero | Con token | 200 · 404 |
| `GET /api/cuentas/{numero}/metas` | Sus metas, con su avance | Con token | 200 · 404 |
| `POST /api/cuentas` | Abre una cuenta | ASESOR | 201 · 400 |
| `PATCH /api/cuentas/{numero}` | Cambia el plan o el estado | ASESOR | 200 · 400 · 404 |
| `DELETE /api/cuentas/{numero}` | La cierra (queda CERRADA, no se borra) | ASESOR | 204 · 404 |
| `POST /api/cuentas/{numero}/metas` | Crea una meta de ahorro | CLIENTE | 201 · 400 · 404 |
| `GET /api/comercios` | Lista los comercios | Con token | 200 |
| `GET /api/comercios/{codigo}` | Un comercio | Con token | 200 · 404 |
| `POST /api/comercios` | Crea un comercio | ADMIN | 201 · 400 |
| `PATCH /api/comercios/{codigo}` | Cambia su nombre | ADMIN | 200 · 400 · 404 |
| `DELETE /api/comercios/{codigo}` | Lo desactiva (no se borra) | ADMIN | 204 · 404 |
| `GET /api/planes` | Los planes de redondeo | Con token | 200 |

### Los errores

| Código | Cuándo | `title` |
| :--- | :--- | :--- |
| 400 | Una regla dijo que no, o los datos no cumplen `@Valid`, o el JSON está roto | "Operacion rechazada" · "Datos no validos" · "Cuerpo ilegible" |
| 401 | Falta el token, es falso o venció; o el login con clave mala | "No autenticado" · "Credenciales incorrectas" |
| 403 | El rol no puede hacer esa operación | "Sin permiso" |
| 404 | La cuenta o el comercio no existen | "No encontrado" |
| 409 | La operación choca con datos que ya existen | "Conflicto con los datos" |

---

## Las pruebas

Se corren en su máquina y necesitan PostgreSQL con la base `roundup_test`. La primera vez:

```
docker run --name banco-db -e POSTGRES_USER=banco -e POSTGRES_PASSWORD=banco123 -p 5432:5432 -d postgres:17-alpine
docker exec banco-db psql -U banco -d banco -c "CREATE DATABASE roundup;"
docker exec banco-db psql -U banco -d banco -c "CREATE DATABASE roundup_test;"
```

Después, en esta carpeta:

```
.\mvnw.cmd test
```

No necesitan `JWT_CLAVE`: traen su propia clave de prueba. Tienen que salir **27 de 27**.

| Clase | Qué comprueba | Necesita la base |
| :--- | :--- | :---: |
| `CompraReglasTest` (13) | El caso bueno y cada regla de la compra (también la cuenta ajena), con Mockito | No |
| `CompraServicioTest` (4) | Que una caída a la mitad hace rollback, y que el FALLIDO queda en la bitácora | Sí |
| `SeguridadApiTest` (9) | 401, 403, cada rol, que el usuario sale del token y que un cliente no opera cuentas ajenas, con MockMvc | Sí |
| `ApiApplicationTests` (1) | Que la aplicación arranca | Sí |

---

## Los logs

RoundUp tiene **dos registros**, y un banco necesita los dos:

| | La bitácora | El log |
| :--- | :--- | :--- |
| Para qué | Auditoría del negocio: quién hizo cada compra, cuándo y cómo terminó | Diagnóstico técnico: qué hizo el sistema y por qué falló |
| Dónde | Tabla `bitacora_operacion` | La consola (y un archivo, si se activa) |
| Qué guarda | Solo las compras, EXITOSO o FALLIDO | Todo: el arranque, cada compra, cada rechazo y lo inesperado |
| Quién lo lee | El auditor, el negocio | Soporte y operaciones de TI |

Los logs usan **SLF4J y Logback**, que ya vienen con Spring Boot (no hay dependencias nuevas):

| Nivel | Dónde | Cuándo |
| :--- | :--- | :--- |
| INFO | `CompraServicio` | Una compra se realizó: comprobante, monto, comercio, cuenta, usuario y redondeo |
| WARN | `CompraServicio` | Una regla rechazó la compra (no es una falla del programa: el sistema hizo lo correcto) |
| ERROR | `ManejadorErrores` | Lo inesperado (la base caída, un error de programación), con la traza completa. Al cliente solo le llega un 500 "Error interno", sin detalles |

```
INFO  sv.roundup.api.servicio.CompraServicio : Compra realizada: C-000004 por $4.30 en SUPER-SELECTOS (cuenta RU-000123, usuario ana), redondeo $1.40
WARN  sv.roundup.api.servicio.CompraServicio : Compra rechazada: $4.20 en SUPER-SELECTOS (cuenta RU-000124, usuario luis): Saldo insuficiente: ...
ERROR s.r.api.controlador.ManejadorErrores   : Error inesperado: Unable to acquire JDBC Connection ...
```

**Cómo leerlos:** en la consola de IntelliJ, o con Docker: `docker compose logs -f api`.
**El nivel** está en `application.properties` (`logging.level.sv.roundup.api=INFO`); para escribirlos también a un archivo, se quita el `#` de `logging.file.name`.
**Nunca van en un log:** claves, el token, ni el hash de una contraseña.

---

## Cómo está organizado

```
src/main/java/sv/roundup/api/
├── controlador/   HTTP: recibe DTOs, llama al servicio, responde con ResponseEntity. ManejadorErrores y Swagger
├── dto/           Lo que entra (Solicitud, con @Valid) y lo que sale (Respuesta). Nunca una entidad
├── servicio/      Las reglas y las transacciones: CompraServicio, BitacoraServicio, AutenticacionServicio...
├── repositorio/   Spring Data JPA, con las @Query (tope mensual, join fetch, secuencias, lock)
├── entidad/       Las 12 tablas en JPA
├── excepcion/     OperacionRechazadaException y SaldoInsuficienteException
└── seguridad/     ConfiguracionSeguridad: JWT, BCrypt, roles, 401 y 403
```

Un controlador nunca usa un repositorio: siempre pasa por el servicio.

---

## Lo que no hace

- **No expone el retiro de la meta (R7)**: la tabla `retiro_meta` y los datos existen, pero no hay endpoint.
- **Las consultas no se limitan por dueño**: cualquier usuario con token puede leer una cuenta, sus compras
  y sus metas (`GET`). Comprar y crear metas sí se limita a las cuentas propias.
- No maneja depósitos ni transferencias, no anula compras, no se conecta con tarjetas reales y no paga intereses.
- La base se crea con `ddl-auto=create` y se reinicia en cada arranque; en producción serían migraciones (Flyway).
