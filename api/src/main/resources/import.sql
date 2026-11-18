-- Datos de prueba de RoundUp. Hibernate corre este archivo SOLO con ddl-auto=create, despues de crear las tablas.
-- Una sentencia por linea. En el mismo orden que las entidades: primero lo que no apunta a nadie.
-- Las columnas con DEFAULT en el diagrama (activo, creado_en, fecha_hora...) se escriben: Hibernate no crea los DEFAULT.
-- Los saldos y los monto_ahorrado cuadran con R10 y R11: el estado de cuenta tiene que dar igual que lo guardado.

-- 1. Catalogos
INSERT INTO rol (nombre, descripcion, activo, creado_en) VALUES ('ADMIN', 'Configura catalogos, usuarios y roles; consulta la bitacora', true, now());
INSERT INTO rol (nombre, descripcion, activo, creado_en) VALUES ('ASESOR', 'Registra clientes y abre, bloquea o cierra cuentas', true, now());
INSERT INTO rol (nombre, descripcion, activo, creado_en) VALUES ('CLIENTE', 'Opera solo sobre sus propias cuentas', true, now());

INSERT INTO plan_redondeo (codigo, nombre, multiplicador, tope_mensual, activo, creado_en) VALUES ('BASICO', 'Redondeo simple', 1, 25.00, true, now());
INSERT INTO plan_redondeo (codigo, nombre, multiplicador, tope_mensual, activo, creado_en) VALUES ('DOBLE', 'Redondeo por dos', 2, 50.00, true, now());
INSERT INTO plan_redondeo (codigo, nombre, multiplicador, tope_mensual, activo, creado_en) VALUES ('TRIPLE', 'Redondeo por tres', 3, 75.00, true, now());

-- TIENDA-CERRADA esta inactiva: sirve para probar R4 (comercio con activo = false)
INSERT INTO comercio (codigo, nombre, activo, creado_en) VALUES ('SUPER-SELECTOS', 'Super Selectos', true, now());
INSERT INTO comercio (codigo, nombre, activo, creado_en) VALUES ('FARMACIA-SN', 'Farmacia San Nicolas', true, now());
INSERT INTO comercio (codigo, nombre, activo, creado_en) VALUES ('GASOLINERA-PUMA', 'Gasolinera Puma', true, now());
INSERT INTO comercio (codigo, nombre, activo, creado_en) VALUES ('TIENDA-CERRADA', 'Tienda que ya no opera', false, now());

---- password_hash en BCrypt (nunca la clave tal cual). Claves de prueba para el login:
-- admin / Admin2026 (ADMIN) · mlopez / Asesor2026 (ASESOR) · ana y luis / Cliente2026 (CLIENTE)
INSERT INTO usuario (username, password_hash, correo, activo, creado_en) VALUES ('admin', '$2a$10$tr.hOkeHuIW5zC4SkVNH.OipLaKYC8dt3TgMcgR7l1v.O02kvoacG', 'admin@roundup.sv', true, now());
INSERT INTO usuario (username, password_hash, correo, activo, creado_en) VALUES ('mlopez', '$2a$10$5t0OxbOOG5nMYMSzHXteVuOKjsxpuhlkuzzikvSlPPqXJ/rl.TQS6', 'marta.lopez@roundup.sv', true, now());
INSERT INTO usuario (username, password_hash, correo, activo, creado_en) VALUES ('ana', '$2a$10$Im9YhLQB5i02J.VcJHrGwuEeR/cEnL9d5zPpOnAufqv8a6B4F7UZy', 'ana.martinez@correo.com', true, now());
INSERT INTO usuario (username, password_hash, correo, activo, creado_en) VALUES ('luis', '$2a$10$Im9YhLQB5i02J.VcJHrGwuEeR/cEnL9d5zPpOnAufqv8a6B4F7UZy', 'luis.diaz@correo.com', true, now());

-- usuario 1 = ADMIN, 2 = ASESOR, 3 y 4 = CLIENTE
INSERT INTO usuario_rol (usuario_id, rol_id, asignado_en) VALUES (1, 1, now());
INSERT INTO usuario_rol (usuario_id, rol_id, asignado_en) VALUES (2, 2, now());
INSERT INTO usuario_rol (usuario_id, rol_id, asignado_en) VALUES (3, 3, now());
INSERT INTO usuario_rol (usuario_id, rol_id, asignado_en) VALUES (4, 3, now());

INSERT INTO asesor (usuario_id, codigo_empleado, nombres, apellidos, activo, creado_en) VALUES (2, 'EMP-001', 'Marta', 'Lopez', true, now());

-- Rosa todavia no activa la app: usuario_id NULL
INSERT INTO cliente (usuario_id, dui, nombres, apellidos, correo, telefono, activo, creado_en) VALUES (3, '01234567-8', 'Ana', 'Martinez', 'ana.martinez@correo.com', '7000-0001', true, now());
INSERT INTO cliente (usuario_id, dui, nombres, apellidos, correo, telefono, activo, creado_en) VALUES (4, '02345678-9', 'Luis', 'Diaz', 'luis.diaz@correo.com', '7000-0002', true, now());
INSERT INTO cliente (usuario_id, dui, nombres, apellidos, correo, telefono, activo, creado_en) VALUES (NULL, '03456789-0', 'Rosa', 'Melgar', 'rosa.melgar@correo.com', '7000-0003', true, now());

-- 3. Cuentas (cliente, asesor y plan ya existen)
-- RU-000123 Ana, DOBLE: el caso bueno. saldo = 500.00 - (4.30 + 1.40) = 494.30            (R10)
-- RU-000124 Luis, BASICO, $3.20: para R1 (saldo insuficiente, el ejemplo del README)
-- RU-000125 Rosa, BLOQUEADA: para R4
-- RU-000126 Ana, BASICO, sin meta ACTIVA: para R6. saldo = 100.00 - 8.00 - 4.00 + 1.15 = 89.15 (R10)
INSERT INTO cuenta (numero_cuenta, cliente_id, asesor_id, plan_redondeo_id, estado, saldo_apertura, saldo, fecha_apertura) VALUES ('RU-000123', 1, 1, 2, 'ACTIVA', 500.00, 494.30, '2026-10-01 09:00:00');
INSERT INTO cuenta (numero_cuenta, cliente_id, asesor_id, plan_redondeo_id, estado, saldo_apertura, saldo, fecha_apertura) VALUES ('RU-000124', 2, 1, 1, 'ACTIVA', 3.20, 3.20, '2026-10-02 10:00:00');
INSERT INTO cuenta (numero_cuenta, cliente_id, asesor_id, plan_redondeo_id, estado, saldo_apertura, saldo, fecha_apertura) VALUES ('RU-000125', 3, 1, 3, 'BLOQUEADA', 200.00, 200.00, '2026-10-03 11:00:00');
INSERT INTO cuenta (numero_cuenta, cliente_id, asesor_id, plan_redondeo_id, estado, saldo_apertura, saldo, fecha_apertura) VALUES ('RU-000126', 1, 1, 1, 'ACTIVA', 100.00, 89.15, '2026-10-04 12:00:00');

-- 4. Metas (una sola ACTIVA por cuenta, R5)
-- meta 1 Laptop (RU-000123): monto_ahorrado = 1.40, el redondeo de la compra C-000001           (R11)
-- meta 2 Vacaciones (RU-000124): de Luis; sirve para R12 (no puede recibir compras de otra cuenta)
-- meta 3 Audifonos (RU-000126): llego al objetivo (0.75 + 0.40 = 1.15), se retiro todo y quedo RETIRADA: 1.15 - 1.15 = 0.00 (R6, R7, R11)
INSERT INTO meta_ahorro (cuenta_id, nombre, monto_objetivo, monto_ahorrado, estado, creado_en) VALUES (1, 'Laptop', 600.00, 1.40, 'ACTIVA', '2026-10-01 09:05:00');
INSERT INTO meta_ahorro (cuenta_id, nombre, monto_objetivo, monto_ahorrado, estado, creado_en) VALUES (2, 'Vacaciones', 300.00, 0.00, 'ACTIVA', '2026-10-02 10:05:00');
INSERT INTO meta_ahorro (cuenta_id, nombre, monto_objetivo, monto_ahorrado, estado, creado_en) VALUES (4, 'Audifonos', 1.00, 0.00, 'RETIRADA', '2026-10-04 12:05:00');

-- 5. Compras, retiros y bitacora (lo que apunta a cuentas y metas)
-- C-000001: $4.30 en plan DOBLE -> (5.00 - 4.30) x 2 = 1.40, el ejemplo de R3
-- C-000002 y C-000003: plan BASICO -> 0.75 y 0.40 a la meta Audifonos
INSERT INTO compra (numero_comprobante, cuenta_id, comercio_id, meta_id, monto, redondeo, fecha_hora) VALUES ('C-000001', 1, 1, 1, 4.30, 1.40, '2026-10-10 14:00:00');
INSERT INTO compra (numero_comprobante, cuenta_id, comercio_id, meta_id, monto, redondeo, fecha_hora) VALUES ('C-000002', 4, 2, 3, 7.25, 0.75, '2026-10-11 15:00:00');
INSERT INTO compra (numero_comprobante, cuenta_id, comercio_id, meta_id, monto, redondeo, fecha_hora) VALUES ('C-000003', 4, 3, 3, 3.60, 0.40, '2026-10-12 16:00:00');

INSERT INTO retiro_meta (numero_comprobante, meta_id, monto, fecha_hora) VALUES ('R-000001', 3, 1.15, '2026-10-13 09:00:00');

INSERT INTO bitacora_operacion (usuario_id, cuenta_id, tipo_operacion, resultado, detalle, fecha_hora) VALUES (3, 1, 'COMPRA', 'EXITOSO', 'Compra C-000001 por $4.30 en SUPER-SELECTOS, redondeo $1.40', '2026-10-10 14:00:00');
INSERT INTO bitacora_operacion (usuario_id, cuenta_id, tipo_operacion, resultado, detalle, fecha_hora) VALUES (3, 4, 'COMPRA', 'EXITOSO', 'Compra C-000002 por $7.25 en FARMACIA-SN, redondeo $0.75', '2026-10-11 15:00:00');
INSERT INTO bitacora_operacion (usuario_id, cuenta_id, tipo_operacion, resultado, detalle, fecha_hora) VALUES (3, 4, 'COMPRA', 'EXITOSO', 'Compra C-000003 por $3.60 en GASOLINERA-PUMA, redondeo $0.40', '2026-10-12 16:00:00');
INSERT INTO bitacora_operacion (usuario_id, cuenta_id, tipo_operacion, resultado, detalle, fecha_hora) VALUES (3, 4, 'RETIRO_META', 'EXITOSO', 'Retiro R-000001 de la meta Audifonos por $1.15', '2026-10-13 09:00:00');
INSERT INTO bitacora_operacion (usuario_id, cuenta_id, tipo_operacion, resultado, detalle, fecha_hora) VALUES (4, 2, 'COMPRA', 'FALLIDO', 'Saldo insuficiente: la compra necesita $5.00 y la cuenta RU-000124 tiene $3.20', '2026-10-14 18:00:00');

DROP SEQUENCE IF EXISTS compra_comprobante_seq;
CREATE SEQUENCE compra_comprobante_seq START WITH 4;

-- Numero de cuenta de las cuentas nuevas: RU-000127 en adelante (arriba ya hay RU-000123 a RU-000126)
DROP SEQUENCE IF EXISTS cuenta_numero_seq;
CREATE SEQUENCE cuenta_numero_seq START WITH 127;
