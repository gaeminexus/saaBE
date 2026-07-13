<?php
include "../lib/config.php";
include "../lib/utils.php";
include "../lib/auth.php";
include "../documents/gn_xml_11.php";
include "../lib/fn_inventario.php";
// Response Helper para respuestas JSON estandarizadas
// include "../lib/response_helper.php"; // Descomentar cuando esté listo para usar

ini_set('display_errors', 0);
error_reporting(E_ERROR | E_PARSE);

date_default_timezone_set('America/Guayaquil');
$dbConn =  connect($db);
$ntabla = "fctr";
$ntablaDet = "dtfc";

// Autenticación dual: JWT (app) o tradicional (web)
$usuarioJWT = intentarAutenticacionJWT();

error_log("fel - Factura", 0);
$attr = "id, tipoComprobante, facturador, comprador, tipoDoc, numero, numEstablecimiento, numPtoEmision, secuencial, ambiente, clave, fecha, observacion, subtotal, subcero, subtotal5, subtotal8, pIVA, vIVA, vIVA5, vIVA8, vICE, vIRBPNR, descuento, porDescuento, propina, subsidio, totalSinSub, ahorroSub, total, ptoEmision, usuario, pathGen, autorizacion, fechaAutorizacion, formaPago, estado, estadoEmision";
$xdfc = ":id,:tipoComprobante,:facturador,:comprador,:tipoDoc,:numero,:numEstablecimiento,:numPtoEmision,:secuencial,:ambiente,:clave,:fecha,:observacion, :subtotal,:subcero, :subtotal5, :subtotal8, :pIVA,:vIVA, :vIVA5, :vIVA8, :vICE,:vIRBPNR,:descuento,:porDescuento,:propina,:subsidio,:totalSinSub,:ahorroSub,:total,:ptoEmision,:usuario,:pathGen,:autorizacion,:fechaAutorizacion,:formaPago,1,1";
$attrDet = "id, factura, descripcion, cantidad, valor, subTotal, descuento, baseImponible, porcentajeIVA, valorIVA, porcentajeICE, valorICE, subsidio, precioSinSub, total, producto, codigoIVASRI, estado";
$xdfcDet = ":id,:factura, :descripcion, :cantidad, :valor, :subTotal, :descuento, :baseImponible, :porcentajeIVA, :valorIVA, :porcentajeICE, :valorICE, :subsidio, :precioSinSub, :total, :producto, :codigoIVASRI, 1";

$entidades = [
    ['tabla'=>'fcdr', 'campo'=>'facturador'],
    ['tabla'=>'cmpr', 'campo'=>'comprador'],
    ['tabla'=>'ptem', 'campo'=>'ptoEmision'],
    ['tabla'=>'usro', 'campo'=>'usuario'],
];

// METODO GET
if ($_SERVER['REQUEST_METHOD'] == 'GET'){    
    if (isset($_GET['id'])){
        error_log("fel - Factura - GET[id]", 0);
        $resultado = getEntidadFull($dbConn, $ntabla, $_GET['id'], "*", $entidades);
        
        // Si hay JWT, validar permisos de acceso
        if ($usuarioJWT && isset($resultado['facturador'])) {
            if (!validarPermisoFacturador($usuarioJWT, $resultado['facturador'])) {
                http_response_code(403);
                echo json_encode(['error' => 'No tiene permisos para acceder a esta factura']);
                salir($dbConn);
            }
        }
        
        header("HTTP/1.1 200 OK");
        echo json_encode($resultado);
        salir($dbConn);
    }elseif(isset($_GET['all'])){
        error_log("fel - Factura - GET ALL", 0);
        
        // Si hay JWT, filtrar solo las facturas del facturador del usuario
        if ($usuarioJWT) {
            $sql = $dbConn->prepare("SELECT * FROM $ntabla WHERE facturador = :facturador AND estado = 1 ORDER BY id DESC");
            $sql->bindValue(':facturador', $usuarioJWT['facturador']);
            $sql->execute();
            $sql->setFetchMode(PDO::FETCH_ASSOC);
            $resultado = $sql->fetchAll();
            header("HTTP/1.1 200 OK");
            echo json_encode(addEntidades($dbConn, $resultado, $entidades));
        } else {
            header("HTTP/1.1 200 OK");
            echo json_encode(getAllFull($dbConn, $ntabla, $entidades));
        }
        salir($dbConn);
    }
}

// METODO POST
if ($_SERVER['REQUEST_METHOD'] == 'POST'){
    //$json = file_get_contents('php://input'); //Recibe el json
    // Converts it into a PHP object
    // error_log($json, 0);
    //$data = json_decode($json, true);
    $criterios = json_decode($_POST['criterios'], true);
    if (isset($criterios['criterios'])){
        selectByCriteria($dbConn, $entidades, $criterios['criterios']);
    }else{
        $continuar = true;
        $data = json_decode($_POST['factura'], true);
        $dbConn->beginTransaction();
        error_log("fel - Factura - POST xxxxxx [".$data['fecha']."]", 0);
        $tipoComprobante="01"; // Codigo de factura en tabla 3 del SRI
        $idAmbiente=$data['usuario']['ambiente']; //Ambiente de prueba 1
        $tipoEmision="1"; // Tabla 2 SRI Emision Normal
        $facturador = $data['facturador']['id'];
        $licencia = validaLicencia($dbConn, $facturador);
        if($licencia != "OK"){
            $dbConn->rollBack();
            header("HTTP/1.1 500 Error Interno del Servidor");
            //header("HTTP/1.1 400 Bad Request");
            echo $licencia;
            salir($dbConn);
        }        
        $secuencial=obtenerSecuencial($dbConn, $data['ptoEmision']['id'], $tipoComprobante);
        error_log("Secuencial:".$secuencial);
        if($secuencial){
            if($secuencial>0){
                $clave = getMod11Dv($data, $tipoComprobante, $idAmbiente, $tipoEmision, $secuencial);
                error_log("fel - Factura - POST [Datos generados]-secuencial[$secuencial]-clave[$clave] ");
                //Inventario
                $tieneInventario = $data['facturador']['inventario'];
                $swFacturador = ['facturador'=>$data['facturador']['id']];
                $swComprador = ['comprador'=>$data['comprador']['id']];
                $swPtoEmision = ['ptoEmision'=>$data['ptoEmision']['id']];
                $swUsuario = ['usuario'=>$data['usuario']['id']];
                $swSecuencial = ['secuencial'=>$secuencial];
                $swNumero = ['numero'=>$data['numEstablecimiento'].'-'.$data['numPtoEmision'].'-'.$secuencial];
                $swClave = ['clave'=>$clave];
                $v_Fecha = date("Y-m-d H:m:s",strtotime($data['fecha']));
                $swFecha = ['fecha'=>$v_Fecha];
                $swTotal = $data['total'];
                $input = array_replace($data, $swComprador, $swFacturador, $swPtoEmision, $swUsuario, $swSecuencial, $swFecha, $swClave, $swNumero);
                $sql = "INSERT INTO $ntabla
                    ($attr)
                VALUES
                    ($xdfc)";
                error_log($sql, 0);                
                try{
                    $statement = $dbConn->prepare($sql);
                    bindAllValues($statement, $input);
                    $statement->execute();
                    $codigo = $dbConn->lastInsertId();
                    if($codigo){
                        // Inserta detalle factura
                        // Converts it into a PHP object
                        $filas = json_decode($_POST['detalleFactura'], true);
                        $final=Array();
                        foreach ($filas as $dataDet){
                            $swFactura = ['factura'=>$codigo];
                            $swProducto = ['producto'=>null];
                            if(isset($dataDet['producto']['id'])){
                                $swProducto = ['producto'=>$dataDet['producto']['id']];
                            }
                            $inputDet = array_replace($dataDet, $swFactura, $swProducto);
                            $sql = "INSERT INTO $ntablaDet
                                ($attrDet)
                            VALUES
                                ($xdfcDet)";
                            error_log($sql, 0);
                            $statement = $dbConn->prepare($sql);
                            bindAllValues($statement, $inputDet);
                            $statement->execute();
                            $codigoDet = $dbConn->lastInsertId();
                            if($codigoDet){
                                $dataDet['id']=$codigoDet;
                                array_push($final,$dataDet);
                            }else{
                                $continuar = false;
                                $result = "Problemas al registrar Detalle de la factura";
                                break;
                            }
                        }
                        // Inserta forma de pago
                        $formaPa = $_POST['formaPago'];
                        $plazo = $_POST['plazo'];
                        $sql = "INSERT INTO fpfc (id, factura, formaPago, valor, plazo, unidadTiempo)
                        VALUES (0, $codigo, '$formaPa', $swTotal, $plazo, 'dias')";
                        $statement = $dbConn->prepare($sql);
                        $statement->execute();
                        // Genera XML
                        list($resp_xml,$pathXMLANG,$pathXMLPHP) = gn_xml_factura_1($dbConn, $clave, $idAmbiente);
                        if($resp_xml=="OK"){
                            error_log("GENERO XML");
                            $sql = "INSERT INTO ptfc (id, factura, path, alterno)
                            VALUES (0, $codigo, '$pathXMLANG', 2)";
                            $statement = $dbConn->prepare($sql);
                            $statement->execute();
                            $sql = "UPDATE fctr SET pathGen = '$pathXMLANG', estado = 2
                            WHERE   id = $codigo";
                            $statement = $dbConn->prepare($sql);
                            $statement->execute();
                        }else{
                            $continuar = false;
                            error_log("fel - Detalle Factura - NO SE GENERO XML");
                            $result = "Problemas al generar XML de la Factura";
                        }
                        if($continuar){
                            if($tieneInventario == 1){
                                $entidad = 1;
                                $idEntidad = $codigo; 
                                $tipoMovimiento = 2;
                                $mensajeInventario = movimientoKardex($dbConn, $entidad, $idEntidad, $tipoMovimiento);
                                if($mensajeInventario != 'OK'){
                                    $continuar = false;
                                    $result = $mensajeInventario;
                                }
                            }
                        }
                        if($continuar){
                            $dbConn->commit();
                            $result = json_encode(getEntidadFull($dbConn, $ntabla, $codigo, "*", $entidades));
                        }
                    } else {
                        $continuar = false;
                        $result = "Problemas al registrar la factura";
                    }
                }catch(PDOException $e){
                    $continuar = false;
                    $result = $e->getMessage();
                }
            }
        }else{
            $continuar = false;
            $result = 'Problemas al generar el secuencial';
        }
        if($continuar){
            header("HTTP/1.1 200 OK");
            error_log("fel - Facturador - Fin POST: ".$result, 0);
            echo $result;
        }else{
            $dbConn->rollBack();
            header("HTTP/1.1 500 Error Interno del Servidor");
            //header("HTTP/1.1 400 Bad Request");
            error_log("fel - Facturador - Fin POST: ".$result, 0);
            echo $result;
        }
        salir($dbConn);
    } 
}

//METODO DELETE
if ($_SERVER['REQUEST_METHOD'] == 'DELETE'){
  error_log("fel - Factura - DELETE", 0);
  $statement = $dbConn->prepare("DELETE FROM $ntabla where id=:id");
  $statement->bindValue(':id', $_GET['id']);
  $statement->execute();
  header("HTTP/1.1 200 OK");
  echo '{"resultado":"OK"}'; // ************************* REVISAR SI VAMOS A UTILIZAR ESTA ENTIDAD PARA MENSAJES
  salir($dbConn);
}

//Actualizar
if ($_SERVER['REQUEST_METHOD'] == 'PUT'){
    error_log("fel - Factura - PUT", 0);
    $json = file_get_contents('php://input');
    error_log("Json:[".$json."]", 0);
    $data = json_decode($json, true);
    // VERIFICA SI FACTURADOR TIENE INVENTARIO
    $v_Inv = $data['facturador']['inventario'];
    $swFacturador = ['facturador'=>$data['facturador']['id']];
    $swComprador = ['comprador'=>$data['comprador']['id']];
    $swPtoEmision = ['ptoEmision'=>$data['ptoEmision']['id']];
    $swUsuario = ['usuario'=>$data['usuario']['id']];
    $input = array_replace($data, $swComprador, $swFacturador, $swPtoEmision, $swUsuario);
    $codigo = $input['id'];
    $fields = getParams($input);
    $sql = "UPDATE $ntabla
        SET $fields
        WHERE id='$codigo'";
    $statement = $dbConn->prepare($sql);
    bindAllValues($statement, $input);
    $statement->execute();
    // ACTUALIZA INVENTARIO SI ANULA FACTURA
    if($v_Inv == 1){
        // 
        if(isset($data['estadoEmision'])){
            if ($data['estadoEmision']==3){
                $entidad = 1;
                $idEntidad = $codigo;
                $tipoMovimiento = 7;
                $mensajeInventario = movimientoKardex($dbConn, $entidad, $idEntidad, $tipoMovimiento);
            }
        }
    }
    header("HTTP/1.1 200 OK");
    $campos = getCampos($input);
    echo json_encode(getEntidad($dbConn, $ntabla, $codigo,$campos)); // ******************************* Preguntar si manda toda la entidad
    salir($dbConn);
}

function selectByCriteria($dbConn, $entidades, $input)
{
    error_log(">>>>>>>> Criterios <<<<<<<<<<<<<", 0);
    $condicion = '';
    foreach($input as $criterio)
    {
        if($criterio['campo']=='desde'){
            $condicion = $condicion." AND fecha >= '".$criterio['valor']."'";
        }elseif ($criterio['campo']=='hasta'){
            $condicion = $condicion." AND fecha < date_add('".$criterio['valor']."', interval 1 day)";
        }else{
            $condicion = $condicion." AND ".$criterio['campo']." = '".$criterio['valor']."'";
        }   
    }
    $select = "SELECT a.* FROM fctr a, cmpr b WHERE a.comprador = b.id ";
    error_log("-->".$select.$condicion, 0);
    $sql = $dbConn->prepare($select.$condicion);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $registros = $sql->fetchAll();
    $final=Array();
    foreach ($registros as $resultado) {
        foreach ($entidades as $datos) {
            $tabla = $datos['tabla'];
            $campo = $datos['campo'];
            $dato = $resultado[$campo];
            // No filtrar por estado en entidades relacionadas para mostrar info histórica
            // (Ej: compradores inactivos en facturas antiguas deben seguir mostrándose)
            $sql = $dbConn->prepare("SELECT * FROM $tabla where id=:id");
            $sql->bindValue(':id', $dato);
            $sql->execute();
            $consulta = array($campo => $sql->fetch(PDO::FETCH_ASSOC));
            $resultado=array_replace($resultado, $consulta);
        }
        array_push($final,$resultado);
    }
    header("HTTP/1.1 200 OK");
    echo json_encode($final);
    salir($dbConn);
}
?>