#!/bin/bash
# =============================================================================================
# Clona los esquemas del SAA de producción en esta base de PRUEBAS (base vacía).
# La imagen database/free trae la base prearmada y NO corre /opt/oracle/scripts/setup: se lanza a
# mano con docker exec (ver docs/infra/pruebas/REFRESCAR-PRUEBAS.md).
#
# Producción SOLO se LEE: un database link de esta base hacia producción, e impdp con
# NETWORK_LINK y FLASHBACK_TIME (foto coherente, sin archivo de dump y sin detener nada).
# Todo lo que se escribe (usuarios, tablas, el UPDATE del SRI) se escribe AQUÍ, en pruebas.
#
# Ojo: la imagen ejecuta este archivo con "source", no como proceso aparte. Por eso no hay
# "exit" ni "set -e": cualquiera de los dos cortaría el arranque del contenedor.
# =============================================================================================

CLON_CONN="system/${ORACLE_PWD}@localhost:1521/FREEPDB1"
CLON_LOG=/opt/oracle/oradata/clon_produccion.log

clon_sql() {
  sqlplus -s "$CLON_CONN" <<EOF
set heading off feedback off pages 0 lines 4000 serveroutput on
$1
exit
EOF
}

clon_principal() {
  echo "=== CLON DE PRODUCCION: inicio $(date)" | tee -a "$CLON_LOG"

  if [ -z "$PROD_SYSTEM_PWD" ]; then
    echo "=== CLON DE PRODUCCION: falta PROD_SYSTEM_PWD en el .env; la base queda VACIA" | tee -a "$CLON_LOG"
    return
  fi

  # 0. Solo sobre una base VACÍA: con los esquemas ya creados, impdp SALTA las tablas existentes y
  #    pruebas se quedaría con datos viejos aunque el control parezca bueno. Para refrescar, vaciar
  #    antes con sql/borrar-esquemas-pruebas.sql.
  local existentes
  existentes=$(clon_sql "select count(*) from dba_users where oracle_maintained = 'N' and username <> 'PDBADMIN';" | tr -d '[:space:]')
  if [ "$existentes" != "0" ]; then
    echo "=== CLON DE PRODUCCION: esta base ya tiene $existentes esquema(s). No se clona encima." | tee -a "$CLON_LOG"
    echo "=== Para refrescar: detener el WildFly de pruebas y correr sql/borrar-esquemas-pruebas.sql" | tee -a "$CLON_LOG"
    return
  fi

  # 1. Enlace de solo lectura hacia producción
  clon_sql "create database link PROD connect to system identified by \"${PROD_SYSTEM_PWD}\" using '//${PROD_HOST}:${PROD_PORT}/${PROD_SERVICE}';" | tee -a "$CLON_LOG"
  local prueba
  prueba=$(clon_sql "select 'ENLACE_OK' from dual@PROD;" | tr -d '[:space:]')
  if [ "$prueba" != "ENLACE_OK" ]; then
    echo "=== CLON DE PRODUCCION: no se pudo conectar a producción ($prueba); la base queda VACIA" | tee -a "$CLON_LOG"
    clon_sql "drop database link PROD;" >/dev/null
    return
  fi

  # 2. Tablespaces y roles propios de producción que no existen aquí (si los hubiera)
  clon_sql "
begin
  for t in (select tablespace_name from dba_tablespaces@PROD where contents = 'PERMANENT'
            minus select tablespace_name from dba_tablespaces) loop
    execute immediate 'create tablespace ' || t.tablespace_name || ' datafile ''/opt/oracle/oradata/FREE/FREEPDB1/'
      || lower(t.tablespace_name) || '01.dbf'' size 200M autoextend on next 100M maxsize unlimited';
    dbms_output.put_line('tablespace creado: ' || t.tablespace_name);
  end loop;
  for r in (select role from dba_roles@PROD where oracle_maintained = 'N'
            minus select role from dba_roles) loop
    execute immediate 'create role ' || r.role;
    dbms_output.put_line('rol creado: ' || r.role);
  end loop;
end;
/" | tee -a "$CLON_LOG"

  # 3. Esquemas del SAA (todos los usuarios no-Oracle de producción)
  local esquemas
  esquemas=$(clon_sql "select listagg(username, ',') within group (order by username) from dba_users@PROD where oracle_maintained = 'N' and username <> 'PDBADMIN';" | tr -d '[:space:]')
  echo "=== Esquemas a copiar: $esquemas" | tee -a "$CLON_LOG"

  # 4. Importación directa desde producción
  impdp "$CLON_CONN" network_link=PROD schemas="$esquemas" flashback_time=systimestamp \
        nologfile=y 2>&1 | tee -a "$CLON_LOG"

  # 4b. Estadísticas: por network_link las de los índices no se importan (ORA-20000 «Unable to set
  #     values for index», ~100 avisos inofensivos). Sin ellas el optimizador consulta a ciegas.
  echo "=== Recalculando estadisticas de $esquemas" | tee -a "$CLON_LOG"
  clon_sql "
begin
  for e in (select regexp_substr('$esquemas', '[^,]+', 1, level) s from dual
            connect by regexp_substr('$esquemas', '[^,]+', 1, level) is not null) loop
    dbms_stats.gather_schema_stats(e.s, cascade => true);
    dbms_output.put_line('estadisticas OK: ' || e.s);
  end loop;
end;
/" | tee -a "$CLON_LOG"

  # 5. Control: mismas filas aquí y en producción (antes de borrar el enlace)
  clon_sql "
select rpad(t, 6) || ' prod=' || p || '  pruebas=' || q || case when p = q then '  OK' else '  DIFERENTE' end
from (
  select 'PRST' t, (select count(*) from CRD.PRST@PROD) p, (select count(*) from CRD.PRST) q from dual union all
  select 'APRT',   (select count(*) from CRD.APRT@PROD),   (select count(*) from CRD.APRT)   from dual union all
  select 'ASNT',   (select count(*) from CNT.ASNT@PROD),   (select count(*) from CNT.ASNT)   from dual union all
  select 'DTAS',   (select count(*) from CNT.DTAS@PROD),   (select count(*) from CNT.DTAS)   from dual
);
select 'objetos invalidos en pruebas: ' || count(*) from dba_objects
 where status = 'INVALID' and owner in (select username from dba_users where oracle_maintained = 'N');" | tee -a "$CLON_LOG"

  # 6. Neutralización: que pruebas NO emita al SRI real.
  #    El ambiente de cada comprobante sale de CBR.FCDR.AMBIENTE (FacturaServiceImpl:164, 436):
  #    1 = celcer (pruebas del SRI), 2 = cel (producción).
  clon_sql "
update CBR.FCDR set AMBIENTE = 1 where nvl(AMBIENTE, 0) <> 1;
commit;
select 'facturadores en ambiente SRI de PRODUCCION que quedan: ' || count(*) from CBR.FCDR where AMBIENTE = 2;" | tee -a "$CLON_LOG"

  # 7. Fuera el enlace: no se deja la clave de producción guardada en pruebas
  clon_sql "drop database link PROD;" | tee -a "$CLON_LOG"

  echo "=== CLON DE PRODUCCION TERMINADO $(date)" | tee -a "$CLON_LOG"
}

clon_principal
