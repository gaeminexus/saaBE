# Anticipos a trabajadores

**Módulo:** Recursos Humanos · **Fecha:** 2026-08-27 · **Tipo:** proceso nuevo

Permite entregar un anticipo de sueldo a un colaborador y descontárselo del rol en cuotas, dejando rastro contable de que el dinero salió.

Antes esto se hacía a medias: se creaba a mano un descuento recurrente y el sistema **nunca registraba la entrega**. No había documento, ni pago, ni asiento. Ahora el ciclo se cierra solo.

---

## 1. El ciclo, de un vistazo

```
Solicitar  →  Aprobar y pagar  →  (el banco confirma)  →  En descuento  →  Cancelado
                                        ↓
                          asiento + cheque + descuento en el rol
```

El descuento del rol **se crea cuando el pago se confirma**, no cuando se aprueba. Es a propósito: si el pago se revierte, no puede quedar un descuento cobrando un dinero que nunca se entregó.

## 2. Solicitar el anticipo

**Recursos Humanos → Procesos → Anticipos a trabajadores → Nuevo anticipo.**

| Campo | Para qué sirve |
|---|---|
| **Empleado** | Se elige de la lista o se busca por identificación. |
| **Valor** | Lo que se le entrega. |
| **Número de cuotas** | En cuántos roles se le descontará. |
| **Mes de inicio del descuento** | Desde qué rol empieza a cobrarse. Si se deja vacío, arranca desde el mes en que se entregó el dinero. |
| **Motivo** y **Observación** | El sustento de por qué se concede. |

Al escribir valor y cuotas, la pantalla muestra el **valor de cuota** que se descontará del rol. Conviene mirarlo antes de guardar: es lo que el colaborador va a ver en su rol cada mes.

> **Un anticipo vivo por colaborador.** Si la persona ya tiene uno sin terminar de pagar, la pantalla lo avisa al elegirla y no deja continuar. Primero hay que terminar el anterior.

## 3. Aprobar y pagar

Desde la lista, el botón de **aprobar** (✓) sobre un anticipo *Solicitado*.

1. Se elige la **cuenta bancaria de origen**.
2. Se elige la **forma de pago**.

> **Solo hay cheque o débito automático, y es correcto.** No aparece transferencia porque el sistema no guarda la cuenta bancaria del colaborador para este fin. Es la misma razón por la que la caja chica tampoco se repone por transferencia.
>
> **La opción Cheque solo aparece si la cuenta elegida maneja chequera.** Si no la ves, es que esa cuenta no tiene chequera configurada — elegí otra cuenta o usá débito automático.

Al confirmar, el sistema en un solo paso:

- genera el **asiento**: DEBE la cuenta de anticipos a colaboradores / HABER la cuenta del banco;
- registra el **movimiento bancario**;
- **gira el cheque**, si esa fue la forma de pago, y **te muestra el número** — anotalo, lo necesitás para el cheque físico;
- crea el **descuento en el rol** con su calendario de cuotas.

El anticipo queda en **En descuento**.

## 4. Qué pasa después, sin que nadie haga nada

Cada vez que se paga un rol, el sistema cobra la cuota que toca, baja el saldo del anticipo y marca la cuota como descontada. Cuando el saldo llega a cero, el anticipo pasa a **Cancelado**.

El ciclo cuadra solo en contabilidad: la entrega **debita** la cuenta de anticipos y cada descuento del rol la **acredita**. El saldo de esa cuenta es, en todo momento, lo que los colaboradores deben.

## 5. Anular

- **Antes de pagarse** (*Solicitado* o *Aprobado* sin pago confirmado): se anula desde la lista indicando el motivo.
- **Ya pagado**: no se anula desde aquí. Hay que **revertir el pago** desde CxP → Pagos por transferencia. Eso deshace el asiento, el movimiento bancario y el cheque, y el anticipo vuelve a *Aprobado*.
- Si el descuento ya cobró alguna cuota en un rol, el sistema **rechaza** la reversión. Primero hay que resolver el rol.

## 6. Qué revisar si algo no sale

| Síntoma | Causa |
|---|---|
| No deja solicitar para ese colaborador | Ya tiene un anticipo sin terminar. La pantalla muestra su código. |
| No aparece la opción **Cheque** | La cuenta bancaria elegida no maneja chequera. |
| No deja aprobar | El anticipo ya no está en *Solicitado*: alguien lo aprobó o lo anuló antes. |
| Dice que hay que revertir el pago | Correcto: ya se entregó el dinero. Se anula desde CxP, no desde aquí. |
| El colaborador no aparece en la lista | Solo se listan colaboradores **activos**. |

---

## Comprobado

Ciclo completo verificado en el sistema el 2026-08-27, con un anticipo de **$600 en 3 cuotas** a una colaboradora:

| Paso | Resultado |
|---|---|
| Solicitud | Valor de cuota calculado en vivo: **200.00** |
| Aprobación con cheque | **Cheque N° 3** girado por $600, con aviso en pantalla |
| Asiento **8313** | **DEBE $600 `1.4.03.10.02` ANTICIPO REMUNERACIÓN / HABER $600 `1.1.02.05.40` BANCO AMAZONAS** |
| Glosa | *"Anticipo a colaborador … \| 3 cuotas \| Ref: CHQ-3 \| Valor: $600"* |
| Movimiento bancario | Registrado |
| Pago | Confirmado, con origen *Anticipo a empleado* |
| Descuento en el rol | Creado: 3 cuotas de $200, desde septiembre |
| Calendario de cuotas | **3 cuotas** al 01-sep, 01-oct y 01-nov, con saldos 400 / 200 / 0 |

*Pendiente: agregar capturas de pantalla.*
