// Genera C19-07-reglas-dtrp-ambiente-limpio.sql: en un ambiente limpio con C19b (C19-01 de la rama, sin CNT.CRRP),
// las reglas cuenta -> renglon que main cargo en CNT.CRRP despues de C19-02, escritas en CNT.DTRP:
//   * B17: D-097 (7) 4.8.99.01-07 al renglon 489090 (03-semilla-b17-4.8.99.01-07-al-489090.sql), D-053 4.8.99.08
//     (02-semilla-b17-4.8.99.08.sql) y D-180 4.8.99.9 (def9-09-cuenta-4.8.99.9-gnd-multas.sql).
//   * FCD (C38): las 110 reglas del BLOQUE 5 de C38-01-semilla-flujo-efectivo-comparativo.sql, una por una, con la
//     misma traduccion de columnas que C19-05 (CRRPCNTA -> NUMCTADS, CRRPALCN -> DTRPALCN, CRRPCDRN -> DTRPCDRN,
//     CRRPMDSG -> DTRPMDSG, CRRPRPRT -> DTRPRPRT, CRRPFCDS/FCHS -> DTRPFCDS/FCHS, CRRPOBSR -> DTRPOBSR).
// C38-01 no se toca: es lo que corrio en la base de trabajo y lo que lee el arnes de prueba de C38.
// Uso: node C19-07-generar.js > C19-07-reglas-dtrp-ambiente-limpio.sql   (desde esta carpeta)
const fs = require("fs");
const path = require("path");

const c38 = fs.readFileSync(path.join(__dirname, "C38-01-semilla-flujo-efectivo-comparativo.sql"), "utf8").split(/\r?\n/);
const PREFIJO = "INSERT INTO CNT.CRRP (CRRPCDGO, RPRTCDGO, CRRPCNTA, CRRPALCN, CRRPCDRN, CRRPMDSG, CRRPRPRT, CRRPFCDS, CRRPFCHS, CRRPOBSR, CRRPESTD) SELECT CNT.SQ_CRRPCDGO.NEXTVAL, r.RPRTCDGO, ";
const SUFIJO = " FROM CNT.RPRT r WHERE r.PJRQCDGO = 1236 AND r.RPRTCDES = 'FCD';";

/** Valores SQL separados por coma: literales '...' (con '' escapado), NULL o numeros. */
function valores(s) {
  const out = [];
  let i = 0;
  while (i < s.length) {
    if (s[i] === "'") {
      let j = i + 1, t = "";
      for (;;) {
        if (j >= s.length) throw new Error("literal sin cerrar: " + s);
        if (s[j] === "'" && s[j + 1] === "'") { t += "'"; j += 2; continue; }
        if (s[j] === "'") break;
        t += s[j++];
      }
      out.push({ texto: t });
      i = j + 1;
    } else {
      let j = s.indexOf(",", i);
      if (j < 0) j = s.length;
      const v = s.slice(i, j).trim();
      out.push(v === "NULL" ? null : { numero: v });
      i = j;
    }
    while (s[i] === "," || s[i] === " ") i++;
  }
  return out;
}
const sql = (v) => (v === null ? "NULL" : v.texto !== undefined ? `'${v.texto.replace(/'/g, "''")}'` : v.numero);

const reglas = [];
for (const l of c38) {
  if (!l.startsWith("INSERT INTO CNT.CRRP")) continue;
  if (!l.startsWith(PREFIJO) || !l.endsWith(SUFIJO)) throw new Error("formato inesperado en C38-01: " + l);
  const v = valores(l.slice(PREFIJO.length, l.length - SUFIJO.length));
  if (v.length !== 9 || sql(v[8]) !== "1") throw new Error("se esperaban 9 valores con estado 1: " + l);
  const [cnta, alcn, cdrn, mdsg, rprt, fcds, fchs, obsr] = v;
  // cuenta del plan solo en las reglas por naturaleza (1); las de signo 2/3 son codigos del B17, no cuentas del plan
  const porNaturaleza = sql(mdsg) === "1";
  reglas.push(
    "INSERT INTO CNT.DTRP (DTRPCDGO, RPRTCDGO, PLNNCDDS, NUMCTADS, NMBCTADS, PLNNCDHS, NUMCTAHS, NMBCTAHS, DTRPSGNO, DTRPCDRN, DTRPALCN, DTRPMDSG, DTRPRPRT, DTRPFCDS, DTRPFCHS, DTRPOBSR)" +
      ` SELECT CNT.SQ_DTRPCDGO.NEXTVAL, r.RPRTCDGO, ${porNaturaleza ? "p.PLNNCDGO" : "NULL"}, ${sql(cnta)}, ${porNaturaleza ? "p.PLNNNMBR" : "NULL"}, NULL, NULL, NULL, 1,` +
      ` ${sql(cdrn)}, ${sql(alcn)}, ${sql(mdsg)}, ${sql(rprt)}, ${sql(fcds)}, ${sql(fchs)}, ${sql(obsr)}` +
      " FROM CNT.RPRT r" +
      (porNaturaleza ? ` LEFT JOIN CNT.PLNN p ON p.PJRQCDGO = r.PJRQCDGO AND p.PLNNCNTA = ${sql(cnta)}` : "") +
      " WHERE r.PJRQCDGO = 1236 AND r.RPRTCDES = 'FCD';"
  );
}
if (reglas.length !== 110) throw new Error("C38-01 deberia traer 110 reglas de FCD y trae " + reglas.length);

const B17 = "(SELECT RPRTCDGO FROM CNT.RPRT WHERE PJRQCDGO = 1236 AND RPRTCDES = 'B17')";
const copiaDe = (origen, cuenta, obs) =>
  "INSERT INTO CNT.DTRP (DTRPCDGO, RPRTCDGO, PLNNCDDS, NUMCTADS, NMBCTADS, PLNNCDHS, NUMCTAHS, NMBCTAHS, DTRPSGNO, DTRPCDRN, DTRPALCN, DTRPMDSG, DTRPRPRT, DTRPFCDS, DTRPFCHS, DTRPOBSR)\n" +
  `SELECT CNT.SQ_DTRPCDGO.NEXTVAL, h.RPRTCDGO, p.PLNNCDGO, '${cuenta}', p.PLNNNMBR, NULL, NULL, NULL, 1, '489090', h.DTRPALCN, h.DTRPMDSG, h.DTRPRPRT, h.DTRPFCDS, h.DTRPFCHS,\n` +
  `       '${obs}'\n` +
  "  FROM CNT.DTRP h\n" +
  `  LEFT JOIN CNT.PLNN p ON p.PJRQCDGO = 1236 AND p.PLNNCNTA = '${cuenta}'\n` +
  ` WHERE h.RPRTCDGO IN ${B17} AND h.NUMCTADS = '${origen}'\n` +
  `   AND NOT EXISTS (SELECT 1 FROM CNT.DTRP x WHERE x.RPRTCDGO = h.RPRTCDGO AND x.NUMCTADS = '${cuenta}');`;
const GND = "('4.8.99.01', '4.8.99.02', '4.8.99.03', '4.8.99.04', '4.8.99.05', '4.8.99.06', '4.8.99.07')";

const salida = [
  "-- =====================================================================================",
  "-- C19-07 — AMBIENTE LIMPIO con C19b (C19-01 de la rama: sin CNT.CRRP). Reglas cuenta -> renglon que main cargo en",
  "--          CNT.CRRP despues de C19-02, ahora en el detalle del motor de balances (CNT.DTRP). Decision D-031.",
  "-- GENERADO por C19-07-generar.js desde C38-01-semilla-flujo-efectivo-comparativo.sql (no editar a mano).",
  "--",
  "-- CUANDO: solo donde NO existe CNT.CRRP. Donde existe (base de trabajo; produccion si alli corrio el C19-01 de main)",
  "--   las mismas reglas las migra C19-05 y este script NO va.",
  "-- ORDEN: C19-01 (rama) -> C19-02 -> C19-03 -> C19-04 -> C38-01 SIN su BLOQUE 5 (CRRP) ni su BLOQUE 7 (controles sobre",
  "--   CRRP) -> este script. Desplegar el WAR de C19b despues de C19-01.",
  "-- QUE HACE",
  "--   BLOQUE 2  B17: D-097 (7) 4.8.99.01-07 del renglon 4890 (padre) al 489090 OTROS.",
  "--   BLOQUE 3  B17: D-053 4.8.99.08 GND POR AJUSTES y D-180 4.8.99.9 GND MULTAS al 489090 (copias de la regla de",
  "--             4.8.99.07; no hacen nada si ya existen).",
  "--   BLOQUE 4  FCD (C38): las 110 reglas de caja, orden, puentes y clasificacion por codigo del B17.",
  "-- SQL PURO (sin SET/DEFINE/WHENEVER), SIN COMMIT. NO SE HA EJECUTADO.",
  "-- =====================================================================================",
  "",
  "-- BLOQUE 1 — CONTROL ANTES",
  "-- 1.1 ESPERADO: 0 filas (si CNT.CRRP existe, este script no va: correr C19-05).",
  "SELECT table_name FROM all_tables WHERE owner = 'CNT' AND table_name = 'CRRP';",
  "-- 1.2 ESPERADO: 7 filas, 4.8.99.01-07 al renglon 4890 (como las deja C19-02).",
  `SELECT NUMCTADS, DTRPCDRN FROM CNT.DTRP WHERE RPRTCDGO IN ${B17} AND NUMCTADS LIKE '4.8.99.%' ORDER BY NUMCTADS;`,
  "-- 1.3 ESPERADO: 1 fila, FCD con 35 renglones y 0 reglas (C38-01 sin su BLOQUE 5).",
  "SELECT r.RPRTCDES, (SELECT COUNT(*) FROM CNT.RNRP n WHERE n.RPRTCDGO = r.RPRTCDGO) renglones,",
  "       (SELECT COUNT(*) FROM CNT.DTRP d WHERE d.RPRTCDGO = r.RPRTCDGO) reglas",
  "  FROM CNT.RPRT r WHERE r.PJRQCDGO = 1236 AND r.RPRTCDES = 'FCD';",
  "",
  "-- BLOQUE 2 — B17: 4.8.99.01-07 al renglon 489090 (D-097 (7))",
  "UPDATE CNT.DTRP SET DTRPCDRN = '489090',",
  "       DTRPOBSR = SUBSTR(NVL(DTRPOBSR || ' | ', '') || 'D-097 (7): al renglon 489090 OTROS (el 4890 es padre de 489005/489090), como 4.8.99.08 (D-053)', 1, 500)",
  ` WHERE RPRTCDGO IN ${B17} AND NUMCTADS IN ${GND} AND DTRPCDRN = '4890';`,
  "",
  "-- BLOQUE 3 — B17: 4.8.99.08 (D-053) y 4.8.99.9 (D-180)",
  copiaDe("4.8.99.07", "4.8.99.08", "D-053: GND por ajustes (centavos de redondeo) al renglon 489090 OTROS"),
  copiaDe("4.8.99.08", "4.8.99.9", "D-180: GND multas al renglon 489090 OTROS"),
  "",
  "-- BLOQUE 4 — FCD: reglas (caja, orden, puentes y clasificacion por codigo del B17), traducidas de C38-01 BLOQUE 5",
  ...reglas,
  "",
  "-- BLOQUE 5 — CONTROL DESPUES (antes del COMMIT)",
  "-- 5.1 ESPERADO: 9 filas, 4.8.99.01-08 y 4.8.99.9, todas al 489090.",
  `SELECT NUMCTADS, DTRPCDRN FROM CNT.DTRP WHERE RPRTCDGO IN ${B17} AND NUMCTADS LIKE '4.8.99.%' ORDER BY NUMCTADS;`,
  "-- 5.2 ESPERADO: B17 35 reglas · FLC 12 · FCD 110 · FLE 0 · CMP 0 (las mismas cuentas que CNT.CRRP en la base de trabajo).",
  "SELECT r.RPRTCDES, r.RPRTALTR, (SELECT COUNT(*) FROM CNT.DTRP d WHERE d.RPRTCDGO = r.RPRTCDGO) reglas",
  "  FROM CNT.RPRT r WHERE r.PJRQCDGO = 1236 AND r.RPRTTPRP = 2 ORDER BY r.RPRTALTR;",
  "-- 5.3 Reglas de FCD por naturaleza (DTRPMDSG 1) sobre cuentas que no existen en el plan de la 1236. ESPERADO: 0 filas.",
  "SELECT d.NUMCTADS FROM CNT.DTRP d JOIN CNT.RPRT r ON r.RPRTCDGO = d.RPRTCDGO",
  " WHERE r.PJRQCDGO = 1236 AND r.RPRTCDES = 'FCD' AND d.DTRPMDSG = 1 AND d.PLNNCDDS IS NULL;",
  "-- 5.4 Codigos del B17 usados en la clasificacion de FCD (DTRPMDSG 2 y 3) que no son renglones del B17. ESPERADO: 0 filas.",
  "SELECT d.NUMCTADS FROM CNT.DTRP d JOIN CNT.RPRT r ON r.RPRTCDGO = d.RPRTCDGO",
  " WHERE r.PJRQCDGO = 1236 AND r.RPRTCDES = 'FCD' AND d.DTRPMDSG IN (2, 3)",
  "   AND NOT EXISTS (SELECT 1 FROM CNT.RNRP b JOIN CNT.RPRT rb ON rb.RPRTCDGO = b.RPRTCDGO",
  "                    WHERE rb.PJRQCDGO = 1236 AND rb.RPRTCDES = 'B17' AND b.RNRPCDRN = d.NUMCTADS);",
  "",
  "-- >>> COMMIT manual aqui, solo si los controles del BLOQUE 5 dieron lo esperado. <<<",
  "",
  "-- REVERSO (sin COMMIT)",
  "-- DELETE FROM CNT.DTRP WHERE RPRTCDGO IN (SELECT RPRTCDGO FROM CNT.RPRT WHERE PJRQCDGO = 1236 AND RPRTCDES = 'FCD');",
  `-- DELETE FROM CNT.DTRP WHERE RPRTCDGO IN ${B17} AND NUMCTADS IN ('4.8.99.08', '4.8.99.9');`,
  `-- UPDATE CNT.DTRP SET DTRPCDRN = '4890' WHERE RPRTCDGO IN ${B17} AND NUMCTADS IN ${GND} AND DTRPCDRN = '489090';`,
  "",
];
process.stdout.write(salida.join("\n").replace(/\r?\n/g, "\r\n"));
