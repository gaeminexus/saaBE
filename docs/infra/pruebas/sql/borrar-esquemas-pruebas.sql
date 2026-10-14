-- =============================================================================================
-- Vacia una base de PRUEBAS antes de volver a clonar produccion: borra todos los usuarios no
-- propios de Oracle (los esquemas del SAA) para que setup/01-clonar-produccion.sh los traiga de
-- nuevo. Sin esto, impdp SALTA las tablas que ya existen y pruebas se queda con datos viejos.
--
-- Guardas (si falla cualquiera, no se borra nada):
--   1. Se corre SIEMPRE con "docker exec -i saa-pruebas ...", nunca contra saa-oracle-23ai.
--   2. La empresa 1236 tiene que llamarse 'ASOPREP CUADRE' o 'ASOPREP PRUEBAS'. En produccion se
--      llama 'ASOPREP': si este script llegara a produccion, se detiene aqui.
--   3. Ningun facturador en ambiente SRI 2 (produccion). El clon deja todos en 1.
--   (Si los esquemas ya no existen, no hay nada que proteger y solo informa.)
--
-- Antes: detener el WildFly de pruebas (Ctrl+C en su consola). Las sesiones que queden (DBeaver,
-- etc.) se cortan aqui.
--
-- Uso (PowerShell, en el servidor, desde D:\saa-pruebas):
--   Get-Content .\sql\borrar-esquemas-pruebas.sql | docker exec -i saa-pruebas bash -c 'sqlplus -s -l "system/$ORACLE_PWD@localhost:1521/FREEPDB1"'
--
-- Reverso: no hay; se recupera volviendo a clonar (01-clonar-produccion.sh).
-- Sin tildes a proposito: PowerShell 5.1 manda el texto a docker en ASCII.
-- =============================================================================================

whenever sqlerror exit failure rollback
set serveroutput on feedback off verify off heading on pages 100 lines 200

prompt === ANTES: esquemas en esta base
select username, created from dba_users
 where oracle_maintained = 'N' and username <> 'PDBADMIN' order by 1;

declare
  v_tablas number;
  v_nombre varchar2(200);
  v_prod   number;
  v_vivas  number;
begin
  select count(*) into v_tablas from dba_tables where owner = 'SCP' and table_name = 'PJRQ';
  if v_tablas = 1 then
    execute immediate 'select max(PJRQNMBR) from SCP.PJRQ where PJRQCDGO = 1236' into v_nombre;
    if nvl(v_nombre, '-') not in ('ASOPREP CUADRE', 'ASOPREP PRUEBAS') then
      raise_application_error(-20010, 'la empresa 1236 se llama "' || v_nombre
        || '": esta base NO parece de pruebas (en produccion es ASOPREP). No se borra nada.');
    end if;
    execute immediate 'select count(*) from CBR.FCDR where AMBIENTE = 2' into v_prod;
    if v_prod > 0 then
      raise_application_error(-20011, 'hay ' || v_prod
        || ' facturador(es) en ambiente SRI de PRODUCCION: esta base NO parece de pruebas. No se borra nada.');
    end if;
    dbms_output.put_line('base de pruebas confirmada: ' || v_nombre);
  else
    dbms_output.put_line('SCP.PJRQ no existe: la base ya esta vacia o a medio borrar; se continua.');
  end if;

  -- Cortar sesiones abiertas de esos usuarios (si no, DROP USER da ORA-01940)
  for s in (select sid, serial# sr from v$session
             where username in (select username from dba_users
                                 where oracle_maintained = 'N' and username <> 'PDBADMIN')) loop
    begin
      execute immediate 'alter system kill session ''' || s.sid || ',' || s.sr || ''' immediate';
      dbms_output.put_line('sesion cortada: ' || s.sid || ',' || s.sr);
    exception when others then null;
    end;
  end loop;

  for i in 1 .. 10 loop
    select count(*) into v_vivas from v$session
     where username in (select username from dba_users
                         where oracle_maintained = 'N' and username <> 'PDBADMIN');
    exit when v_vivas = 0;
    dbms_session.sleep(2);
  end loop;

  for u in (select username from dba_users
             where oracle_maintained = 'N' and username <> 'PDBADMIN' order by username) loop
    execute immediate 'drop user "' || u.username || '" cascade';
    dbms_output.put_line('borrado: ' || u.username);
  end loop;

  -- Enlace a produccion que pudiera haber quedado de un clon interrumpido
  for l in (select db_link from dba_db_links where owner = 'SYSTEM' and db_link like 'PROD%') loop
    execute immediate 'drop database link ' || l.db_link;
    dbms_output.put_line('enlace borrado: ' || l.db_link);
  end loop;
end;
/

prompt === DESPUES: tiene que salir vacio
select username from dba_users
 where oracle_maintained = 'N' and username <> 'PDBADMIN' order by 1;

exit
