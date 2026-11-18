# RoundUp: ahorro automático por redondeo

**Proyecto integrador · Etapa 1: el diseño**
Nestor Leonel Martinez Valencia · Programa Digital Trainee 2026

---

## 1. Nombre y de qué se trata

**RoundUp** es una API REST para que los clientes de un banco ahorren sin darse cuenta. Cada vez
que el cliente paga en un comercio, la compra se redondea al dólar siguiente y la diferencia se
envía a su meta de ahorro. Por ejemplo, en una compra de $4.30 se cobran $5.00 y los $0.70 van a
la meta "Laptop".

Resuelve un problema común: a mucha gente le cuesta apartar dinero a fin de mes. RoundUp ahorra
en montos tan pequeños que no se sienten, y el cliente puede elegir un plan que multiplica el
redondeo (x2, x3) para ahorrar más rápido.

## 2. Quién lo usa: los tres roles

| Rol | Quién es | Qué puede hacer |
| :--- | :--- | :--- |
| **ADMIN** | jefatura o administrador | configura los catálogos (planes de redondeo y comercios), gestiona usuarios y roles, consulta la bitácora |
| **ASESOR** | operador de agencia | registra clientes, abre cuentas con su saldo de apertura y su plan, cambia el plan, bloquea o cierra cuentas |
| **CLIENTE** | el usuario final | solo sobre **sus propias** cuentas: crea metas, paga compras (la operación principal), consulta sus compras y el avance de sus metas, y retira lo ahorrado |

"Sus propias cuentas" se puede comprobar porque el usuario está ligado a su cliente
(`cliente.usuario_id`) y cada cuenta a su cliente (`cuenta.cliente_id`). El asesor está ligado a
su usuario con `asesor.usuario_id`.

## 3. Las entidades

Los diagramas están en `diagrama-er.png` (las tablas en PostgreSQL) y `diagrama-clases.png` (las
mismas entidades en Java).

| Módulo | Entidad | Qué representa en el negocio |
| :--- | :--- | :--- |
| Negocio | **Cuenta** | La cuenta de la que sale el dinero de cada compra, con su saldo, su estado y el plan de redondeo que tiene asignado. |
| | **MetaAhorro** | Un objetivo de ahorro del cliente ("Laptop", $600.00) que va juntando los redondeos. |
| | **Compra** | Cada pago en un comercio, con su monto, el redondeo que se cobró y su número de comprobante. |
| | **RetiroMeta** | El momento en que el cliente saca lo ahorrado de una meta y el dinero vuelve a su cuenta. |
| Personas | **Cliente** | La persona dueña de las cuentas, identificada por su DUI. |
| | **Asesor** | El empleado del banco que registra clientes y abre cuentas. |
| Catálogos | **PlanRedondeo** | Cuánto se multiplica el redondeo (BASICO x1, DOBLE x2, TRIPLE x3) y el tope mensual de ahorro. |
| | **Comercio** | El lugar donde se hizo la compra (supermercado, farmacia, gasolinera). |
| Bitácora | **BitacoraOperacion** | Qué operación se hizo, quién la hizo, sobre qué cuenta, cuándo y cómo terminó. |
| Seguridad | **Usuario** | La cuenta de acceso al sistema: usuario, contraseña cifrada y si está activo. |
| | **Rol** | ADMIN, ASESOR o CLIENTE. |
| | **UsuarioRol** | Qué roles tiene cada usuario (resuelve el muchos a muchos). |

## 4. La operación principal: procesar una compra

Cambia **cuatro tablas** y tiene que ser todo o nada (`@Transactional`).

**Entrada:** el `numero_cuenta`, el `codigo` del comercio y el `monto` de la compra. El usuario que
la ejecuta se toma de la sesión.

**Validaciones previas** (antes de tocar nada): la cuenta está ACTIVA y el comercio está activo
(**R4**); el monto es mayor que cero (**R9**); se calcula el redondeo (**R3**, con el tope de **R2**
y solo si hay meta ACTIVA, **R6**, que sea de la misma cuenta, **R12**) y se verifica que el saldo
alcance para monto + redondeo (**R1**).

**Los cambios, en orden:**

1. Inserta la compra en `compra`, con su `numero_comprobante`, el `monto`, el `redondeo` y la
   `meta_id` (NULL si no había meta ACTIVA).
2. Descuenta `monto + redondeo` del `saldo` en `cuenta` (**R10**).
3. Si hubo redondeo, lo suma al `monto_ahorrado` en `meta_ahorro` (**R11**). Si
   `monto_ahorrado >= monto_objetivo`, la meta pasa a COMPLETADA (**R6**).
4. Registra la operación en `bitacora_operacion` con `tipo_operacion` COMPRA y `resultado` EXITOSO.

**Si falla a la mitad:** por ejemplo, en el paso 3. Se deshace todo: no queda la compra
registrada, el saldo de la cuenta queda como estaba y la meta no cambia. El cliente no puede
pagar un redondeo que nunca llega a su meta. El intento se registra en la bitácora con
`resultado` FALLIDO en una transacción aparte, para que ese registro no se pierda con el rollback.

**Salida:** el comprobante, con el `numero_comprobante`, el comercio, el monto, el redondeo, el
total cobrado, el saldo nuevo de la cuenta y el avance de la meta en porcentaje (y si se completó).

## 5. Las reglas de negocio

| # | La regla, con los nombres del diagrama | Dónde se cumple | Si no se cumple |
| :--- | :--- | :--- | :--- |
| R1 | Una compra solo se procesa si `cuenta.saldo` >= `monto` + `redondeo` | servicio, al pagar | se rechaza: "Saldo insuficiente: la compra necesita $5.00 y la cuenta RU-000123 tiene $3.20" |
| R2 | La suma de los `compra.redondeo` de una cuenta en el mes calendario no supera el `tope_mensual` de su `plan_redondeo` | servicio, al pagar | la compra se procesa, pero el `redondeo` se recorta a lo que falta para el tope (puede quedar en $0.00) |
| R3 | `redondeo` = (siguiente dólar − `monto`) × `plan_redondeo.multiplicador`. Si el monto es exacto, el redondeo es $0.00. Ejemplo: $4.30 en plan DOBLE → $1.40 | servicio, al pagar | la compra no se registra: se deshace todo |
| R4 | Solo se compra desde una `cuenta` en estado ACTIVA y en un `comercio` con `activo` = true | servicio, al pagar | se rechaza: "La cuenta RU-000123 está BLOQUEADA" |
| R5 | Una `cuenta` tiene como máximo una `meta_ahorro` en estado ACTIVA | tabla (índice único parcial) + servicio, al crear la meta | se rechaza: "Ya tienes una meta ACTIVA: Laptop" |
| R6 | El redondeo solo se abona a una `meta_ahorro` ACTIVA. Cuando `monto_ahorrado` >= `monto_objetivo`, pasa a COMPLETADA y deja de recibir redondeos | servicio, al pagar | si no hay meta ACTIVA, la compra se cobra sin redondeo (`meta_id` = NULL, `redondeo` = $0.00) |
| R7 | Solo se retira una `meta_ahorro` ACTIVA o COMPLETADA con `monto_ahorrado` > 0, y una sola vez: se retira todo el `monto_ahorrado`, se suma al `saldo` de la cuenta y la meta queda RETIRADA | tabla (UNIQUE en `retiro_meta.meta_id`) + servicio, al retirar | se rechaza: "La meta Laptop ya fue RETIRADA" |
| R8 | El `dui` tiene el formato 00000000-0 y no se repite. Tampoco se repiten `numero_cuenta`, `numero_comprobante`, `username` ni `correo` | tabla (UNIQUE) + servicio (formato) | se rechaza: "Ya existe un cliente con ese DUI" |
| R9 | `compra.monto`, `monto_objetivo`, `retiro_meta.monto` y `tope_mensual` son mayores que cero; `saldo`, `saldo_apertura`, `monto_ahorrado` y `redondeo` nunca son negativos; `multiplicador` está entre 1 y 3 | tabla (CHECK) | la base rechaza el INSERT o el UPDATE |
| R10 | `cuenta.saldo` = `saldo_apertura` − la suma de (`monto` + `redondeo`) de sus compras + la suma de los `retiro_meta.monto` de sus metas | servicio, en la operación principal y al retirar | se deshace toda la operación |
| R11 | `meta_ahorro.monto_ahorrado` = la suma de los `compra.redondeo` de la meta − el `retiro_meta.monto` (si ya se retiró) | servicio, en la operación principal y al retirar | se deshace toda la operación |
| R12 | `compra.meta_id` es una `meta_ahorro` ACTIVA de la misma `cuenta` de la compra (`meta_ahorro.cuenta_id` = `compra.cuenta_id`) | servicio, al pagar | se rechaza: "La meta Carro no pertenece a la cuenta RU-000123" |

**Datos guardados a propósito** (las excepciones de la 3FN):
- `cuenta.saldo` y `meta_ahorro.monto_ahorrado` son **DERIVADOS**: se mantienen al día en cada
  operación porque recalcularlos sumando todo el historial en cada consulta no es viable. Cuadran
  con R10 y R11.
- `compra.redondeo` es una **copia congelada**: es lo que se cobró ese día. Si el ADMIN cambia
  después el multiplicador del plan, las compras viejas no cambian.

## 6. Lo que no hace

- No maneja depósitos ni transferencias: el dinero entra a la cuenta con el saldo de apertura y
  con los retiros de las metas.
- No anula ni reversa compras: una compra registrada no se modifica ni se borra.
- No se conecta con tarjetas ni con comercios reales: la compra se registra por la API.
- No paga intereses sobre lo ahorrado ni reparte el redondeo entre varias metas.
