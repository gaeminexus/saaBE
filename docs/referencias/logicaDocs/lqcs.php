<?php
include "../lib/config.php";
include "../lib/utils.php";
include "../lib/auth.php";
include "../documents/gn_xml_lqcs.php";
// Response Helper para respuestas JSON estandarizadas
// include "../lib/response_helper.php"; // Descomentar cuando esté listo para usar

ini_set('display_errors', 0);
error_reporting(E_ERROR | E_PARSE);

$dbConn =  connect($db);
$ntabla = "lqcs";
$ntablaDet = "dtlc";

// Autenticación dual: JWT (app) o tradicional (web)
$usuarioJWT = intentarAutenticacionJWT();

error_log("fel - Liquidacion", 0);
date_default_timezone_set('America/Guayaquil');
$attr = "id, tipoComprobante, facturador, comprador, tipoDoc, numero, numEstablecimiento, numPtoEmision, secuencial, ambiente, clave, fecha, observacion, subtotal, subcero, pIVA, vIVA, vICE, vIRBPNR, descuento, porDescuento, propina, subsidio, totalSinSub, ahorroSub, total, ptoEmision, usuario, pathGen, autorizacion, fechaAutorizacion, estado, estadoEmision";
$xdlc = ":id,:tipoComprobante,:facturador,:comprador,:tipoDoc,:numero,:numEstablecimiento,:numPtoEmision,:secuencial,:ambiente,:clave,:fecha, :observacion, :subtotal,:subcero,:pIVA,:vIVA,:vICE,:vIRBPNR,:descuento,:porDescuento,:propina,:subsidio,:totalSinSub,:ahorroSub,:total,:ptoEmision,:usuario,:pathGen,:autorizacion,:fechaAutorizacion,1,1";
$attrDet = "id, liquidacion, descripcion, cantidad, valor, subTotal, porcentajeIVA, valorIVA, porcentajeICE, valorICE, subsidio, precioSinSub, descuento, total, producto, estado";
$xdfcDet = ":id,:liquidacion, :descripcion, :cantidad, :valor, :subTotal, :porcentajeIVA, :valorIVA, :porcentajeICE, :valorICE, :subsidio, :precioSinSub, :descuento, :total, :producto, 1";

$entidades = [
    ['tabla'=>'fcdr', 'campo'=>'facturador'],
    ['tabla'=>'cmpr', 'campo'=>'comprador'],
    ['tabla'=>'ptem', 'campo'=>'ptoEmision'],
    ['tabla'=>'usro', 'campo'=>'usuario'],
];

// METODO GET
if ($_SERVER['REQUEST_METHOD'] == 'GET'){    
    if (isset($_GET['id'])){
        error_log("fel - Liquidacion - GET[id]", 0);
        $resultado = getEntidadFull($dbConn, $ntabla, $_GET['id'], "*", $entidades);
        
        // Si hay JWT, validar permisos
        if ($usuarioJWT && isset($resultado['facturador'])) {
            if (!validarPermisoFacturador($usuarioJWT, $resultado['facturador'])) {
                http_response_code(403);
                echo json_encode(['error' => 'No tiene permisos para acceder a esta liquidación']);
                salir($dbConn);
            }
        }
        
        header("HTTP/1.1 200 OK");
        echo json_encode($resultado);
        salir($dbConn);
    }elseif(isset($_GET['all'])){
        error_log("fel - Liquidacion - GET ALL", 0);
        
        // Si hay JWT, filtrar solo las liquidaciones del facturador del usuario
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
    error_log("fel - Liquidacion - POST", 0);
    //$json = file_get_contents('php://input'); //Recibe el json
    // Converts it into a PHP object
    // error_log($json, 0);
    //$data = json_decode($json, true);
    $criterios = json_decode($_POST['criterios'], true);
    if (isset($criterios['criterios'])){
        selectByCriteria($dbConn, $entidades, $criterios['criterios']);
    }else{
        $data = json_decode($_POST['liquidacion'], true);
        $dbConn->beginTransaction();
        error_log("fel - Liquidacion - POST", 0);
        $tipoComprobante="03"; // Codigo de liquidacion en tabla 3 del SRI
        error_log("fel - Liquidacion - tipoComprobante ".$data['tipoComprobante'], 0);
        $idAmbiente=$data['usuario']['ambiente']; //Ambiente de prueba 1
        $tipoEmision="1"; // Tabla 2 SRI Emision Normal
        error_log("EMISION:".$data['ptoEmision']['id']);
        error_log("Clave:".$data['facturador']['codClave']);
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
                error_log("fel - Liquidacion - POST [Datos generados]-secuencial[$secuencial]-clave[$clave] ");
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
                error_log("fel - Liquidacion - POST [Antes del Insert] ", 0);
                $sql = "INSERT INTO $ntabla
                    ($attr)
                VALUES
                    ($xdlc)";
                error_log($sql, 0);                
                try{
                    $statement = $dbConn->prepare($sql);
                    bindAllValues($statement, $input);
                    $statement->execute();
                    $codigo = $dbConn->lastInsertId();
                    if($codigo){
                        // Inserta detalle liquidacion
                        // Converts it into a PHP object
                        $filas = json_decode($_POST['detalleLiquidacion'], true);
                        $final=Array();
                        foreach ($filas as $dataDet){
                            $swLiquidacion = ['liquidacion'=>$codigo];
                            $swProducto = ['producto'=>null];
                            if(isset($dataDet['producto']['id'])){
                                $swProducto = ['producto'=>$dataDet['producto']['id']];
                            }
                            $inputDet = array_replace($dataDet, $swLiquidacion, $swProducto);
                            error_log("fel - Detalle Liquidacion - POST [Antes del Insert] ", 0);
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
                                error_log("fel - Detalle Liquidacion - POST id[$codigoDet]");
                                $dataDet['id']=$codigoDet;
                                array_push($final,$dataDet);
                            }
                        }
                        // Fin inserta detalle
                        // Inserta forma de pago
                        $formaPa = $_POST['formaPago'];
                        $sql = "INSERT INTO fplc (id, liquidacion, formaPago, valor, plazo, unidadTiempo)
                        VALUES (0, $codigo, '$formaPa', $swTotal, 1, 'dias')";
                        $statement = $dbConn->prepare($sql);
                        $statement->execute();
                        // Fin inserta forma pago
                        error_log("fel - Detalle Liquidacion - ".$clave);
                        // Genera XML
                        list($resp_xml,$pathXMLANG,$pathXMLPHP) = gn_xml_liquidacion($dbConn, $clave, $idAmbiente);
                        // $resp_xml=gn_xml_factura($dbConn, $clave);
                        if($resp_xml=="OK"){
                            error_log("GENERO XML");
                            $sql = "INSERT INTO ptlc (id, liquidacion, path, alterno)
                            VALUES (0, $codigo, '$pathXMLANG', 2)";
                            $statement = $dbConn->prepare($sql);
                            $statement->execute();
                            $sql = "UPDATE lqcs SET pathGen = '$pathXMLANG', estado = 2
                            WHERE   id = $codigo";
                            $statement = $dbConn->prepare($sql);
                            $statement->execute();
                        }else{
                            error_log("fel - Detalle Liquidacion - NO SE GENERO XML");
                        }
                        $dbConn->commit();
                        $result = json_encode(getEntidadFull($dbConn, $ntabla, $codigo, "*", $entidades));
                    } else {
                        $dbConn->rollBack();
                        $result = null;
                    }
                }catch(PDOException $e){
                    // unlink($pathXMLPHP);
                    $result = $e->getMessage();
                    $dbConn->rollBack();
                }
            }
        }else{
            $result = null;
        }
        header("HTTP/1.1 200 OK");        
        error_log("fel - Facturador - Fin POST: ".$result, 0);
        echo $result;
        salir($dbConn);
    } 
}

//METODO DELETE
if ($_SERVER['REQUEST_METHOD'] == 'DELETE'){
  error_log("fel - Liquidacion - DELETE", 0);
  $statement = $dbConn->prepare("DELETE FROM $ntabla where id=:id");
  $statement->bindValue(':id', $_GET['id']);
  $statement->execute();
  header("HTTP/1.1 200 OK");
  echo '{"resultado":"OK"}'; // ************************* REVISAR SI VAMOS A UTILIZAR ESTA ENTIDAD PARA MENSAJES
  salir($dbConn);
}

//Actualizar
if ($_SERVER['REQUEST_METHOD'] == 'PUT'){
    error_log("fel - Liquidacion - PUT", 0);
    $json = file_get_contents('php://input');
    error_log("Json:[".$json."]", 0);
    $data = json_decode($json, true);
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
    $select = "SELECT a.* FROM lqcs a, cmpr b WHERE a.comprador = b.id ";
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
            $sql = $dbConn->prepare("SELECT * FROM $tabla where id=:id and estado=1");
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