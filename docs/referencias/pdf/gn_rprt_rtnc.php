<?php
require_once('../lib/config.php');
require_once('../lib/utils.php');
require_once('fn_rprt_rtnc.php');

$dbConn =  connect($db);
$clave = $_GET['c45ghc'];

$opcion = 2;
if(isset($_GET['op'])){
    $opcion = $_GET['op'];
}
//Establecimiento
$tamanoImp = 1;
if(isset($_GET['t'])){
    $tamanoImp = $_GET['t'];
}
if($tamanoImp == 1){
    /*
     * Opcion: 1 Función 2 Link Get
     */
    crearPDF($dbConn, $clave, $opcion);
}else{
    // CONSULTAR SI VAMOS A CREAR PDF
    // DE RETENCIONES PARA IMPRESORAS TÉRMICAS
}
?>