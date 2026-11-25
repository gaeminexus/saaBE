// Genera C19-02-semilla-b17.sql (reporte "B17" en el motor de balances: CNT.RPRT + CNT.RNRP + CNT.DTRP) desde los dos
// insumos del expediente del proyecto de cuadre (repositorio cuadreSuper, fase 1):
//   - b17-mayo2025.csv          catalogo del B17 aceptado por la Superintendencia en mayo 2025 (534 cuentas, en orden)
//   - MAPEO-PLAN-CUENTAS.csv    cuenta del SAA -> cuenta del B17 (el que usa 09-genera-b17-desde-saa.js)
// Reglas que reproduce (las mismas del script 09, que genera el B17 de mayo 2025 identico byte a byte):
//   * renglon = cada cuenta del catalogo, en el orden del archivo presentado; el "7" es neto deudor (debe - haber)
//   * cuenta del SAA sin regla -> renglon = prefijo mas largo de la cuenta sin puntos que exista en el catalogo
//     (mapeo automatico RPRTMPAU = 1; coincide con MAPEO-PLAN-CUENTAS en 963 de 995 cuentas, verificado)
//   * las 32 cuentas "REQUIERE ASIGNACION" -> regla explicita con la asignacion propuesta (o excluida si no hay)
//   * cuentas 7.x -> valor absoluto por cuenta (regla "7", signo 4)
//   * signo segun naturaleza (1 y 4 deudoras, 2 3 y 5 acreedoras en el plan de la 1236)
// Uso: node C19-generar-semilla-b17.js <dir fase1-balance-arranque> > C19-02-semilla-b17.sql
const fs = require("fs");
const path = require("path");
const dir = process.argv[2] || "C:/work/cuadreSuper/03-trabajo/fase1-balance-arranque";
const csv = (f) => fs.readFileSync(path.join(dir, f), "utf8").split(/\r?\n/).filter((l) => l.trim()).map((l) => l.split(";"));
const q = (s) => (s === null || s === undefined || s === "" ? "NULL" : `'${String(s).replace(/'/g, "''")}'`);

const [, ...catalogo] = csv("b17-mayo2025.csv");
const [cabM, ...mapeo] = csv("MAPEO-PLAN-CUENTAS.csv");
const col = (c) => cabM.indexOf(c);
const nombreB17 = new Map();
for (const m of mapeo) {
  const b = m[col("cuenta_b17")], n = m[col("nombre_b17")];
  if (b && n && !nombreB17.has(b)) nombreB17.set(b, n);
  const s = m[col("cuenta_saa")].replace(/\./g, "");
  if (!nombreB17.has(s) && m[col("nombre_saa")]) nombreB17.set(s, m[col("nombre_saa")]);
}
const codigos = new Set(catalogo.map((c) => c[0]));

// Verificacion: mapeo automatico + reglas explicitas = MAPEO-PLAN-CUENTAS
const explicitas = [];
let iguales = 0;
for (const m of mapeo) {
  const cuenta = m[col("cuenta_saa")];
  const requiere = m[col("estado")].startsWith("REQUIERE");
  const hoja = requiere ? m[col("asignacion_propuesta")] : m[col("cuenta_b17")];
  let p = cuenta.replace(/\./g, "");
  while (p && !codigos.has(p)) p = p.slice(0, -1);
  if (p === hoja) { iguales++; continue; }
  if (!requiere) throw new Error(`La cuenta ${cuenta} no es REQUIERE ASIGNACION y el automatico no la reproduce (${p} vs ${hoja})`);
  explicitas.push({ cuenta, hoja, nombre: m[col("nombre_saa")], confianza: m[col("confianza")], criterio: m[col("criterio")] });
}


const out = [];
const P = (s) => out.push(s);
const DTRP = "INSERT INTO CNT.DTRP (DTRPCDGO, RPRTCDGO, PLNNCDDS, NUMCTADS, NMBCTADS, PLNNCDHS, NUMCTAHS, NMBCTAHS, DTRPSGNO, DTRPCDRN, DTRPALCN, DTRPMDSG, DTRPRPRT, DTRPFCDS, DTRPFCHS, DTRPOBSR)";
const B17 = "(SELECT RPRTCDGO FROM CNT.RPRT WHERE PJRQCDGO = 1236 AND RPRTCDES = 'B17')";
P("-- =====================================================================================");
P("-- C19-02 — Reporte \"B17\" (Balances FCPC, Superintendencia de Bancos) en el MOTOR DE BALANCES de la empresa 1236:");
P("--          cabecera en CNT.RPRT (tipo 2 = renglones propios), renglones en CNT.RNRP y reglas en CNT.DTRP.");
P("-- Cuadre Superintendencia, tarea C19b (D-031, 2026-10-29) · GENERADO por C19-generar-semilla-b17.js (no editar a");
P("-- mano: regenerar desde el expediente si cambia el catalogo o el mapeo).");
P("--");
P(`-- Contenido: 1 cabecera (alterno 6) · ${catalogo.length} renglones (catalogo del B17 de mayo 2025 en su orden)`);
P(`--            · ${explicitas.length + 1} reglas en DTRP: ${explicitas.filter((e) => e.hoja).length} asignaciones explicitas de cuentas "REQUIERE ASIGNACION",`);
P(`--            ${explicitas.filter((e) => !e.hoja).length} cuenta(s) excluida(s) por no tener asignacion y la regla de valor absoluto de las cuentas de orden.`);
P(`--            Las otras ${iguales} cuentas del plan las resuelve el mapeo automatico del reporte (prefijo mas largo sin puntos).`);
P("-- Validado: el motor (CalculadorEstructura) con esta definicion produce B17M396831052025.txt IDENTICO byte a byte al");
P("-- presentado (docs/logica-negocio/rpr/ESTRUCTURAS-DE-RENGLONES.md §Validacion).");
P("--");
P("-- ⚠ PARA UN AMBIENTE LIMPIO (produccion). En la base de trabajo, donde ya corrieron las versiones anteriores de");
P("--   C19-01..03 (con CNT.CRRP), NO se corre este script: se corre C19-05 (migra CRRP -> DTRP).");
P("-- ⚠ Las asignaciones explicitas son PROPUESTAS del mapeo de la fase 1; cada una debe quedar decidida por el");
P("--   responsable de ASOPREP (DECISIONES-Y-PARTIDAS.md). Cambiar una = UPDATE de DTRPCDRN de esa regla (o regla nueva");
P("--   con vigencia), sin tocar codigo.");
P("-- ⚠ Catalogo vigente desde siempre (RNRPFCDS NULL). El formato de ene-2026 trae 160225, 230501 y 440525 nuevas y ya");
P("--   no 160290 ni 440520: ver BLOQUE 6 (comentado hasta confirmar desde que corte rige).");
P("--");
P("-- SQL PURO, SIN COMMIT. Requiere C19-01. NO SE HA EJECUTADO.");
P("-- =====================================================================================");
P("");
P("-- BLOQUE 1 — CONTROL ANTES");
P("-- 1.1 No debe existir ya un B17 en la 1236 ni otro reporte con el alterno 6. ESPERADO: 0 filas.");
P("SELECT RPRTCDGO, RPRTNMBR, RPRTALTR, RPRTCDES FROM CNT.RPRT WHERE PJRQCDGO = 1236 AND (RPRTCDES = 'B17' OR RPRTALTR = 6);");
P("");
P("-- BLOQUE 2 — CABECERA (tipo 2 renglones propios · saldo al corte · mapeo automatico · acumulacion por prefijo ·");
P("--            archivo TXT de la Superintendencia con suma de control · codigos B17/M/3968)");
P("INSERT INTO CNT.RPRT (RPRTCDGO, PJRQCDGO, RPRTNMBR, RPRTESTD, RPRTALTR, RPRTTPRP, RPRTCDES, RPRTPRDC, RPRTCDEN, RPRTBSVL, RPRTMPAU, RPRTACPF, RPRTFRAR, RPRTSMCT, RPRTTTLO, RPRTNVMX)");
P("VALUES (CNT.SQ_RPRTCDGO.NEXTVAL, 1236, 'B17 BALANCES FCPC (SUPERINTENDENCIA)', 1, 6, 2, 'B17', 'M', '3968', 1, 1, 1, 1, 1, 'B17 BALANCES FONDOS COMPLEMENTARIOS PREVISIONALES CERRADOS', NULL);");
P("");
P("-- BLOQUE 3 — RENGLONES (CNT.RNRP; orden del archivo presentado; tipo 2 = neto deudor solo para el \"7\")");
const R = "FROM CNT.RPRT r WHERE r.PJRQCDGO = 1236 AND r.RPRTCDES = 'B17';";
catalogo.forEach(([codigo, nivel], i) => {
  const tipo = codigo === "7" ? 2 : 1;
  P(`INSERT INTO CNT.RNRP (RNRPCDGO, RPRTCDGO, RNRPCDRN, RNRPNMBR, RNRPNVLL, RNRPORDN, RNRPTPCL, RNRPBSVL, RNRPFRML, RNRPRPAR, RNRPFCDS, RNRPFCHS, RNRPESTD) SELECT CNT.SQ_RNRPCDGO.NEXTVAL, r.RPRTCDGO, ${q(codigo)}, ${q((nombreB17.get(codigo) || "").toUpperCase())}, ${Number(nivel)}, ${i + 1}, ${tipo}, NULL, NULL, 1, NULL, NULL, 1 ${R}`);
});
P("");
P("-- BLOQUE 4 — REGLAS (CNT.DTRP del mismo reporte: NUMCTADS = cuenta del plan, DTRPCDRN = renglon destino)");
P("-- 4.1 Cuentas de orden: valor absoluto por cuenta (destino = mapeo automatico). El renglon \"7\" es su neto deudor.");
P(`${DTRP} SELECT CNT.SQ_DTRPCDGO.NEXTVAL, r.RPRTCDGO, NULL, '7', NULL, NULL, NULL, NULL, 1, NULL, 1, 4, 1, NULL, NULL, 'B17: cuentas de orden en valor absoluto por cuenta; el renglon 7 lleva su neto deudor (como el B17 de mayo 2025)' ${R}`);
P("-- 4.2 Cuentas del SAA sin equivalente directo en el catalogo (MAPEO-PLAN-CUENTAS: REQUIERE ASIGNACION).");
for (const e of explicitas) {
  const obs = e.hoja
    ? `REQUIERE ASIGNACION - propuesta de la fase 1 (confianza ${e.confianza || "s/d"}): ${e.criterio || ""}`.slice(0, 500)
    : "REQUIERE ASIGNACION sin propuesta: NO se reporta hasta que decida el responsable de ASOPREP (sale en las advertencias si tiene saldo)";
  P(`-- ${e.cuenta} ${e.nombre} -> ${e.hoja || "(no se reporta)"}`);
  P(`${DTRP} SELECT CNT.SQ_DTRPCDGO.NEXTVAL, r.RPRTCDGO, NULL, ${q(e.cuenta)}, NULL, NULL, NULL, NULL, 1, ${q(e.hoja)}, 1, 1, ${e.hoja ? 1 : 0}, NULL, NULL, ${q(obs)} ${R}`);
}
P("");
P("-- BLOQUE 5 — FK y nombre de la cuenta del plan en cada regla (PLNNCDDS / NMBCTADS, como los rangos de siempre)");
P(`UPDATE CNT.DTRP d SET (PLNNCDDS, NMBCTADS) = (SELECT p.PLNNCDGO, p.PLNNNMBR FROM CNT.PLNN p WHERE p.PJRQCDGO = 1236 AND p.PLNNCNTA = d.NUMCTADS) WHERE d.RPRTCDGO = ${B17};`);
P("");
P("-- BLOQUE 6 — (COMENTADO) Cambio de catalogo visto en el formato B17 de ene-2026. Descomentar con la fecha de corte");
P("--   desde la que rige (confirmar con el area financiera / manual de la Superintendencia) en lugar de :DESDE.");
P(`-- UPDATE CNT.RNRP SET RNRPFCHS = :DESDE - 1 WHERE RNRPCDRN IN ('160290', '440520') AND RPRTCDGO = ${B17};`);
P("-- INSERT renglones 160225, 230501 y 440525 con RNRPFCDS = :DESDE y RNRPORDN en su posicion; y revisar las reglas");
P("--   1.6.02.25 / 1.6.02.30 (hoy -> 160290) con DTRPFCHS / DTRPFCDS.");
P("");
P("-- BLOQUE 7 — CONTROL DESPUES");
P(`-- 7.1 ESPERADO: 1 cabecera, ${catalogo.length} renglones, ${explicitas.length + 1} reglas.`);
P("SELECT r.RPRTCDGO, r.RPRTNMBR, r.RPRTALTR, (SELECT COUNT(*) FROM CNT.RNRP n WHERE n.RPRTCDGO = r.RPRTCDGO) renglones,");
P("       (SELECT COUNT(*) FROM CNT.DTRP c WHERE c.RPRTCDGO = r.RPRTCDGO) reglas");
P("  FROM CNT.RPRT r WHERE r.PJRQCDGO = 1236 AND r.RPRTCDES = 'B17';");
P("-- 7.2 Codigos de renglon repetidos. ESPERADO: 0 filas.");
P(`SELECT n.RNRPCDRN, COUNT(*) FROM CNT.RNRP n WHERE n.RPRTCDGO = ${B17} GROUP BY n.RNRPCDRN HAVING COUNT(*) > 1;`);
P("-- 7.3 Reglas cuyo destino no es un renglon. ESPERADO: 0 filas.");
P(`SELECT c.NUMCTADS, c.DTRPCDRN FROM CNT.DTRP c WHERE c.RPRTCDGO = ${B17}`);
P("   AND c.DTRPCDRN IS NOT NULL AND NOT EXISTS (SELECT 1 FROM CNT.RNRP n WHERE n.RPRTCDGO = c.RPRTCDGO AND n.RNRPCDRN = c.DTRPCDRN);");
P("-- 7.4 Reglas sobre cuentas que no existen en el plan de la 1236 (PLNNCDDS NULL). ESPERADO: 0 filas.");
P(`SELECT c.NUMCTADS FROM CNT.DTRP c WHERE c.RPRTCDGO = ${B17} AND c.PLNNCDDS IS NULL;`);
P("");
P("-- REVERSO (sin COMMIT)");
P(`-- DELETE FROM CNT.DTRP WHERE RPRTCDGO = ${B17};`);
P(`-- DELETE FROM CNT.RNRP WHERE RPRTCDGO = ${B17};`);
P("-- DELETE FROM CNT.RPRT WHERE PJRQCDGO = 1236 AND RPRTCDES = 'B17';");
process.stdout.write(out.join("\n") + "\n");
console.error(`renglones ${catalogo.length} · explicitas ${explicitas.length} · automaticas ${iguales}`);
