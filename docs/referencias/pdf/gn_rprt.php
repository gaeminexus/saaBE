<?php
    require_once('../lib/config.php');
    require_once('../lib/utils.php');
    require_once('../lib/tcpdf/tcpdf.php');
    
    $dbConn =  connect($db);
    $clave = $_GET['c45ghc'];
    
    // Datos Facturador, Comprador y Factura
    $sql = $dbConn->prepare("select b.*, b.subtotal 'Sub_12', b.total 'TotalFac', c.id 'Facturador', c.numdoc 'RUC', 
        c.razonSocial 'LOCAL', c.nombreComercial, c.mail 'MAIL', c.telefono 'FONO', c.logo, c.direccion 'DIRECCION',
        c.microEmpresa, c.RIMPE, c.popularRIMPE, c.artesano, c.contribuyenteEspecial, c.contabilidad, c.agenteRetencion,
        c.empTransporte, d.tipoId, d.numdoc, d.nombre, d.direccion, d.telefono, d.mail, c.impCodProd
        from fctr b, fcdr c, cmpr d
        where b.facturador=c.id
        and b.comprador=d.id
        and b.clave=:id");
    $sql->bindValue(':id', $clave);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $primero=$sql->fetch();
    $idFactura = $primero['id'];
    $idAmbiente = $primero['ambiente'];
    $autorizacion = $primero['autorizacion'];
    $fechaAutorizacion = $primero['fechaAutorizacion'];
    $imprimeCodigos = $primero['impCodProd'];
    $fecha = $primero['fecha'];
    
    //Datos Detalle Factura
    /*$sql = $dbConn->prepare("select if(b.id is null, a.descripcion, b.descripcion) as 'Produc', a.* from dtfc a
     LEFT JOIN prdc b
     ON a.producto = b.id
     where a.factura=:factura");*/
    $sql = $dbConn->prepare("SELECT a.*, b.codigo, b.codigoAux from dtfc a
        LEFT JOIN prdc b
        ON a.producto = b.id
        WHERE a.factura=:factura");
    /*$sql = $dbConn->prepare("select * from dtfc a
        where a.factura=:factura");*/
    $sql->bindValue(':factura', $idFactura);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $detFactura=$sql->fetchAll();
    
    //Establecimiento
    $idEstablecimiento = $primero['ptoEmision'];
    $sql = $dbConn->prepare("SELECT b.nombre, b.direccion, b.telefono, b.mail, b.matriz, a.observacion 
        FROM ptem a, estb b
        WHERE a.id=$idEstablecimiento
        AND a.establecimiento = b.id");
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $establecimiento=$sql->fetch();    
    
    //Formas de Pago
    $sql = $dbConn->prepare("select a.*, c.detalle from fpfc a, fctr b, tsri c
        where a.factura = b.id
        and c.lSRI = 24
        and c.codigo = a.formaPago        
        and b.id=:id");
    $sql->bindValue(':id', $idFactura);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $formasPago=$sql->fetchAll();

    //Valor General iva
    $sql = $dbConn->prepare("select a.* from tsri a
        where a.lSRI = 614");
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $regIvaGeneral=$sql->fetchAll();

    if($regIvaGeneral){
        $valorIvaGeneral= (int)$regIvaGeneral[0]['porcentaje'];
    }    
    
    //5 -- Transportista
    $transportista = null;
    if($primero['empTransporte']){
        if($primero['empTransporte'] == 1){
            $idPto = $primero['ptoEmision'];
            $sql = $dbConn->prepare("SELECT b.* FROM ptem a, trpr b
                WHERE a.transportista = b.id
                AND a.id=:id");
            $sql->bindValue(':id', $idPto);
            $sql->execute();
            $sql->setFetchMode(PDO::FETCH_ASSOC);
            $transportista = $sql->fetch();
        }
    }
    
    $RUC=$primero['RUC'];
    $numFactura=$primero['numero'];
    if($idAmbiente){
        if($idAmbiente==2){
            $ambiente = "PRODUCCIÓN";
        }else{            
            $ambiente = "PRUEBAS";
        }
    }else{
        $ambiente = "PRUEBAS";    
    }
    $emision = "NORMAL";
    $claveAcceso = $primero['clave'];
    $htmlAuto= <<<EOD
        <p><strong>R.U.C.:</strong> $RUC</p>
        <p><strong>FACTURA</strong></p>
        <p><strong>No.</strong> $numFactura</p>
        <p><strong>NUMERO DE AUTORIZACIÓN</strong>
        $autorizacion</br>        
        </p>        
        <p><strong>FECHA Y HORA DE AUTORIZACIÓN</strong></p>
        $fechaAutorizacion<br>
        <p><strong>AMBIENTE:</strong> $ambiente</p>
        <p><strong>EMISIÓN:</strong> $emision</p>
        <p><strong>CLAVE DE ACCESO</strong></p>    
    EOD;

    $nombreFcdr = $primero['LOCAL'];    
    $direccionFcdr = $primero['DIRECCION'];
    $telefonoFcdr = $primero['FONO'];
    $mailFcdr = $primero['MAIL'];
    $microEmpresaFcdr = $primero['microEmpresa'];
    $rimpeFcdr = $primero['RIMPE'];
    $rimpePopularFcdr = $primero['popularRIMPE'];
    $codAgenteFcdr = $primero['agenteRetencion'];
    $leyendaAgente = '';
    $codContribuyenteFcdr = $primero['contribuyenteEspecial'];
    $leyendaContribuyente = '';
    $codArtesanoFcdr = $primero['artesano'];
    $leyendaArtesano = '';
    if($codAgenteFcdr){
        if($codAgenteFcdr != ''){
            $leyendaAgente = "<strong>Agente Retención No. </strong>$codAgenteFcdr<br>";
        }
    }
    if($codArtesanoFcdr){
        if($codArtesanoFcdr != ''){
            $leyendaArtesano = "<strong>Código Artesano </strong>$codArtesanoFcdr<br>";
        }        
    }
    if($codContribuyenteFcdr){
        if($codContribuyenteFcdr != ''){
            $leyendaContribuyente = "<strong>Contribuyente Especial No. </strong>$codContribuyenteFcdr<br>";
        }
    }
    $contabilidadFcdr = 'NO';
    if($primero['contabilidad']){
        if($primero['contabilidad']==1){
            $contabilidadFcdr = 'SI';
        }
    }
    $v_tipo_empresa = "";
    if($microEmpresaFcdr){
        if($microEmpresaFcdr == 1){
            $v_tipo_empresa = "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS";
        }
    }
    if($rimpeFcdr){
        if($rimpeFcdr == 1){
            $v_tipo_empresa = "CONTRIBUYENTE RÉGIMEN RIMPE";
        }
    }
    if($rimpePopularFcdr){
        if($rimpePopularFcdr == 1){
            $v_tipo_empresa = "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE";
        }
    }
    $estNombre = $establecimiento['nombre'];
    $estDireccion = $establecimiento['direccion'];
    $estTelefono = $establecimiento['telefono'];
    $estMail = $establecimiento['mail'];   
    if($establecimiento['matriz']){
        if($establecimiento['matriz']==1){
            $htmlInfoFcdr= <<<EOD
                <h3>$nombreFcdr</h3>
                <strong>Dirección:</strong> $direccionFcdr<br><br>
                <strong>Teléfono:</strong> $telefonoFcdr<br>
                <strong>E-mail:</strong> $mailFcdr<br>
                <strong>$v_tipo_empresa</strong><br>
                $leyendaAgente
                $leyendaArtesano
                $leyendaContribuyente
                <strong>Obligado a llevar Contabilidad:</strong>$contabilidadFcdr
            EOD;
        }else{
            $htmlInfoFcdr= <<<EOD
                <h3>&nbsp;$nombreFcdr</strong></h3>
                <div style="font-size:0.8em;">
                <strong>Matriz:</strong><br>
                $direccionFcdr<br>
                $telefonoFcdr / $mailFcdr<br>
                <strong>Sucursal:</strong>$estNombre<br>
                $estDireccion<br>
                $estTelefono / $estMail<br>
                <strong>$v_tipo_empresa</strong><br>
                $leyendaAgente
                $leyendaArtesano
                $leyendaContribuyente
                <strong>Obligado a llevar Contabilidad:</strong>$contabilidadFcdr</div>
            EOD;
        }
    }else{
        $htmlInfoFcdr= <<<EOD
                <h3>&nbsp;$nombreFcdr</strong></h3>
                <div style="font-size:0.8em;">
                <strong>Matriz:</strong><br>
                $direccionFcdr<br>
                $telefonoFcdr / $mailFcdr<br>
                <strong>Sucursal:</strong>$estNombre<br>
                $estDireccion<br>
                $estTelefono / $estMail<br>
                <strong>$v_tipo_empresa</strong><br>
                $leyendaAgente
                $leyendaArtesano
                $leyendaContribuyente
                <strong>Obligado a llevar Contabilidad:</strong>$contabilidadFcdr</div>
            EOD;
    }
    
    $nombreCmdr=$primero['nombre'];
    $numDocCmdr=$primero['numdoc'];
    $direccionCmdr=$primero['direccion'];
    $mailCmdr=$primero['mail'];
    $remision = ""; // ---------> PREGUNTAR DONDE OBTENGO ESTE    
    
    $htmlInfoCmdr= <<<EOD
        <table>
            <tr>
                <td colspan="2"><strong>Razón Social / Nombre:</strong></td>
                <td colspan="5">$nombreCmdr</td>
                <td><strong>RUC / CI:</strong></td>
                <td colspan="3" style="text-align:right;">$numDocCmdr</td>
            </tr>
            <tr>
                <td colspan="2"><strong>Fecha Emisión:</strong></td>
                <td colspan="4">$fecha</td>
                <td>&nbsp;</td>
                <td colspan="2"><strong>Guía Remisión:</strong></td>
                <td style="text-align:right;">$remision</td>
            </tr>
            <tr>
                <td colspan="2"><strong>Dirección:</strong></td>
                <td colspan="5">$direccionCmdr</td>                
                <td><strong>E-mail:</strong></td>
                <td colspan="3" style="text-align:right;">$mailCmdr</td>
            </tr>
        </table>                   
    EOD;
    
    $iaFonos=$primero['telefono'];
    $iaObaservacion=$establecimiento['observacion'].' '.$primero['observacion'];
    
    if($transportista){
        $t_id=$transportista['numdoc'];
        $t_nom=$transportista['nombre'];
        $t_tel=$transportista['telefono'];
        $t_ema=$transportista['mail'];
        $t_pla=$transportista['placa'];
        $htmlInfAdiconal= <<<EOD
            <table>
                <tr>
                    <td colspan="5" style="text-align:center;"><strong>Información Adicional</strong></td>
                </tr>
                <tr>
                    <td><strong>Id:</strong></td>
                    <td>$t_id</td>
                    <td>&nbsp;&nbsp;<strong>Nombre:</strong></td>
                    <td colspan="2">$t_nom</td>
                </tr>
                <tr>
                    <td><strong>Telf.:</strong></td>
                    <td>$t_tel</td>
                    <td>&nbsp;&nbsp;<strong>Email:</strong></td>
                    <td colspan="2">$t_ema</td>
                </tr>
                <tr>
                    <td><strong>Placa:</strong></td>
                    <td>$t_pla</td>
                    <td colspan="3">&nbsp;</td>
                </tr>
            </table>
        EOD;
    }else{
        $htmlInfAdiconal= <<<EOD
            <table>
                <tr>
                    <td colspan="3" style="text-align:center;"><strong>Información Adicional</strong></td>
                </tr>
                <tr>
                    <td colspan="2"><strong>Teléfonos:</strong></td>
                    <td style="text-align:right;">$iaFonos</td>
                </tr>
                <tr>
                    <td colspan="3"><strong>Observación:</strong></td>
                </tr>
                <tr>
                    <td colspan="3">$iaObaservacion</td>
                </tr>
            </table>
        EOD;
    }    
    /*
    $v_Efectivo="99,999.00";
    $v_Transferencia="99,999.00";
    $v_TC="99,999.00";
    $v_TD="99,999.00";
    
    $htmlFormasPago= <<<EOD
        <table>
            <tr>
                <td colspan="3" style="text-align:center;"><strong>Formas de Pago</strong></td>
            </tr>
            <tr>
                <td colspan="2"><strong>Efectivo</strong></td>
                <td style="text-align:right;">$ $v_Efectivo</td>
            </tr>
            <tr>
                <td colspan="2"><strong>Transferencia</strong></td>
                <td style="text-align:right;">$ $v_Transferencia</td>
            </tr>
            <tr>
                <td colspan="2"><strong>Tarjeta de Débito</strong></td>
                <td style="text-align:right;">$ $v_TD</td>
            </tr>
            <tr>
                <td colspan="2"><strong>Tarjeta de Crédito</strong></td>
                <td style="text-align:right;">$ $v_TC</td>
            </tr>
        </table>
    EOD;*/
    //********************************
    $htmlFormasPago='<table>
            <tr>
                <td colspan="6" style="text-align:center;"><strong>Formas de Pago</strong></td>
                <td colspan="2" style="text-align:right;"><strong>Valor</strong></td>
                <td colspan="2" style="text-align:center;"><strong>Plazo</strong></td>
            </tr>';    
    foreach ($formasPago as $forma){
        $numDias = '';
        if($forma['plazo']){
            if($forma['plazo']!=0){
                $numDias = $forma['plazo'];
            }
        }
        $fila = '<tr>
            <td colspan="6">'.$forma['detalle'].'</td>
            <td colspan="2" style="text-align:right;">'.$forma['valor'].'</td>
            <td colspan="2" style="text-align:center;">'.$numDias.' '.$forma['unidadTiempo'].'</td>
        </tr>';
        $htmlFormasPago=$htmlFormasPago.$fila;
    }
    $htmlFormasPago=$htmlFormasPago.'</table>';
    //********************************    
    $subtotal_12=$primero['Sub_12'];
    $subtotal_5=$primero['subtotal5'];
    $subtotal_0=$primero['subcero'];
    $subtotal_8=$primero['subtotal8'];
    $subtotal_no_objeto_iva="0.00";
    $subtotal_exento_iva="0.00";
    $subtotal_sin_impuestos = number_format(($subtotal_12 + $subtotal_0 + $subtotal_5 + $subtotal_8), 2, '.', '');
    $total_descuento=nvl($primero['descuento'], "0.00");
    $ice="0.00";
    $iva_12=$primero['vIVA'];
    $iva_5=$primero['vIVA5'];
    $iva_8=$primero['vIVA8'];
    $total_devolucion_iva="0.00";
    $irbpnr="0.00";
    $valor_total=$primero['TotalFac'];
    $propina=nvl($primero['propina'],"0.00");
    

    $htmlTotales= <<<EOD
        <table>
            <tr>
                <td colspan="2"><strong>SUBTOTAL $valorIvaGeneral%</strong></td>
                <td style="text-align:right;">$subtotal_12</td>
            </tr>
            <tr>
                <td colspan="2"><strong>SUBTOTAL 5%</strong></td>
                <td style="text-align:right;">$subtotal_5</td>
            </tr>
            <tr>
                <td colspan="2"><strong>SUBTOTAL 0%</strong></td>
                <td style="text-align:right;">$subtotal_0</td>
            </tr>
            <tr>
                <td colspan="2"><strong>SUBTOTAL NO OBJETO DE IVA</strong></td>
                <td style="text-align:right;">$subtotal_no_objeto_iva</td>
            </tr>
            <tr>
                <td colspan="2"><strong>SUBTOTAL TARIFA ESPECIAL</strong></td>
                <td style="text-align:right;">$subtotal_8</td>
            </tr>
            <tr>
                <td colspan="2"><strong>SUBTOTAL SIN IMPUESTOS</strong></td>
                <td style="text-align:right;">$subtotal_sin_impuestos</td>
            </tr>
            <tr>
                <td colspan="2"><strong>TOTAL DESCUENTO</strong></td>
                <td style="text-align:right;">$total_descuento</td>
            </tr>
            <tr>
                <td colspan="2"><strong>ICE</strong></td>
                <td style="text-align:right;">$ice</td>
            </tr>
            <tr>
                <td colspan="2"><strong>IVA $valorIvaGeneral%</strong></td>
                <td style="text-align:right;">$iva_12</td>
            </tr>
            <tr>
                <td colspan="2"><strong>IVA 5%</strong></td>
                <td style="text-align:right;">$iva_5</td>
            </tr>
            <tr>
                <td colspan="2"><strong>IVA TARIFA ESPECIAL</strong></td>
                <td style="text-align:right;">$iva_8</td>
            </tr>
            <tr>
                <td colspan="2"><strong>TOTAL DEVOLUCIÓN IVA</strong></td>
                <td style="text-align:right;">$total_devolucion_iva</td>
            </tr>
            <tr>
                <td colspan="2"><strong>IRBPNR</strong></td>
                <td style="text-align:right;">$irbpnr</td>
            </tr>
            <tr>
                <td colspan="2"><strong>VALOR TOTAL</strong></td>
                <td style="text-align:right;">$valor_total</td>
            </tr>
            <tr>
                <td colspan="2"><strong>PROPINA</strong></td>
                <td style="text-align:right;">$propina</td>
            </tr>
        </table>
    EOD;
    
    
    /*$cantidad=1000;
    $descripcion = "Licencia Firma electrónica para 1 año, archivo tipo Certificado p12 con validez de 1 año";
    $precioUnitario = "99,999.23";
    $subsidio = 0.65;
    $precioSinSub = "99,999.23";
    $descuento = "9,999.23";
    $precioTotal = "999,999.23";*/
    
    $dbConn = null;

    // create new PDF document
    $pdf = new TCPDF(PDF_PAGE_ORIENTATION, PDF_UNIT, PDF_PAGE_FORMAT, true, 'UTF-8', false);

    // set document (meta) information
    /*$pdf->SetCreator(PDF_CREATOR);
    $pdf->SetAuthor('Olaf Lederer');
    $pdf->SetTitle('TCPDF Example');
    $pdf->SetSubject('TCPDF Tutorial');
    $pdf->SetKeywords('TCPDF, PDF, example, tutorial');*/
    
    $pdf->SetPrintHeader(false);
    $pdf->setPrintFooter(false);
    $pdf->SetAutoPageBreak(true, 8);
    
    // set font
    $pdf->SetFont('helvetica', '', 9);
    
    // add a page
    $pdf->AddPage();   
    
    // define barcode style
    $style = array(
        'position' => '',
        'align' => 'C',
        'stretch' => false,
        'fitwidth' => true,
        'cellfitalign' => '',
        'border' => false,
        'hpadding' => 'auto',
        'vpadding' => 'auto',
        'fgcolor' => array(0,0,0),
        'bgcolor' => false, //array(255,255,255),
        'text' => true,
        'font' => 'helvetica',
        'fontsize' => 7,
        'stretchtext' => 4
    );    

    //Bordes
    //$pdf->SetLineStyle(array('width' => 0.5, 'cap' => 'butt', 'join' => 'miter', 'dash' => 0, 'color' => array(0, 0, 0)));
    
    // set cell padding
    $pdf->setCellPaddings(1, 1, 1, 1);    
    // set cell margins
    $pdf->setCellMargins(0, 1, 0, 1);    
    // set color for background
    $pdf->SetFillColor(255, 255, 255);
    
    //Div para el logo
    // En la Web
    $lbNombreComercial = '';
    $altoLogo = '124px';
    if($primero['nombreComercial']){
        $lbNombreComercial='<br><span style="font-size:1.5em; font-weight: bold;">'.$primero["nombreComercial"]."</span>";
        $altoLogo = '78px';
    } 
    $divLogo='<div style="width: 100%; text-align: center;">
        <img src="../../'.$primero['logo'].'" height="'.$altoLogo.'">'.
        $lbNombreComercial.'</div>';
    
    //LOCAL
    /*$divLogo='<div style="width: 100%; text-align: center;">
    <img src="logos/'.$primero['logo'].'" height="124px"> </div>';*/
    
    // En la Web
    // $pdf->Image('../'.$primero['logo'], 11, 11, 81 ,45 , 'jpg', '', 'M', false, 300, '', false, false, 0, false, false, false);

    // Logo LOCAL
    //$pdf->Image('logos/prueba_cir.jpg', 11, 11, 81 ,45 , 'jpg', '', 'M', false, 300, '', false, false, 0, false, false, false);
    $pdf->writeHTMLCell(81, '', 11, 11, $divLogo, 0, 1, 0, true, '', true);
    $pdf->writeHTMLCell(95, '', 100, 11, $htmlAuto, 0, 1, 0, true, '', true);
    $pdf->write1DBarcode($claveAcceso, 'C128A', 101, 89, 95, 18, 0.4, $style, 'N');
    // RoundedRect(float $x, float $y, float $w, float $h, float $r[, string $round_corner = '1111' ][, string $style = '' ][, array<string|int, mixed> $border_style = array() ][, array<string|int, mixed> $fill_color = array() ]) : mixed    
    $pdf->RoundedRect(99, 11, 99, 95, 3.50, '1111', 'D');
    $pdf->writeHTMLCell(82, '', 11, 58, $htmlInfoFcdr, 0, 1, 0, true, '', true);
    $pdf->RoundedRect(11, 59, 82, 47, 3.50, '1111', 'D');
    
    $pdf->Line(11, 109, 198, 109, '');
    $pdf->writeHTMLCell(184, '', 11, 108, $htmlInfoCmdr, 0, 1, 0, true, '', true);
    $pdf->Line(11, 130, 198, 130, '');
    //$pdf->RoundedRect(11, 108, 187, 17, 3.50, '1111', 'D');
    // MultiCell($w, $h, $txt, $border=0, $align='J', $fill=0, $ln=1, $x='', $y='', $reseth=true, $stretch=0, $ishtml=false, $autopadding=true, $maxh=0)
    $pdf->SetFont('helvetica', 'B', 7);
    $altoCelda=8;
    $pdf->setCellMargins(0, 0, 0, 0);
    // Cabecera Detalle Factura
    $anchoCodigo=0;
    if($imprimeCodigos){
        if($imprimeCodigos==1){
            $pdf->MultiCell(15, $altoCelda, 'Cod.', 0, 'C', 1, 0, 11, 131, true, 0, false, true, $altoCelda, 'M');
            $pdf->MultiCell(15, $altoCelda, 'Cod.Aux', 0, 'C', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
            $pdf->MultiCell(13, $altoCelda, 'Cant.', 0, 'C', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
            $anchoCodigo=30;
        }else{
            $pdf->MultiCell(13, $altoCelda, 'Cant.', 0, 'C', 1, 0, 11, 131, true, 0, false, true, $altoCelda, 'M');
        }
    }else{
        $pdf->MultiCell(13, $altoCelda, 'Cant.', 0, 'C', 1, 0, 11, 131, true, 0, false, true, $altoCelda, 'M');
    }
    $pdf->MultiCell(113-$anchoCodigo, $altoCelda, 'Descripción', 0, 'C', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
    $pdf->MultiCell(15, $altoCelda, 'P. Unitario', 0, 'C', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');    
    $pdf->MultiCell(15, $altoCelda, 'Subtotal', 0, 'C', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
    $pdf->MultiCell(12, $altoCelda, 'Dto.', 0, 'C', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
    $pdf->MultiCell(20, $altoCelda, 'Precio Total', 0, 'R', 1, 1, '', '', true, 0, false, true, $altoCelda, 'M');
    // Fin Cabecera Detalle Factura
    $pdf->SetFont('helvetica', '', 7);    
    $altoCelda=4;
    if($imprimeCodigos){
        if($imprimeCodigos==1){
            foreach ($detFactura as $det){
                $altoCelda = numLineas($det['descripcion']);
                // Detalle Factura
                $pdf->MultiCell(16, $altoCelda, $det['codigo'], 0, 'C', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
                $pdf->MultiCell(16, $altoCelda, $det['codigoAux'], 0, 'C', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
                $pdf->MultiCell(13, $altoCelda, $det['cantidad'], 0, 'C', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
                $pdf->MultiCell(112-$anchoCodigo, $altoCelda, $det['descripcion'], 0, 'L', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
                $pdf->MultiCell(15, $altoCelda, $det['valor'], 0, 'R', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
                $pdf->MultiCell(15, $altoCelda, $det['subTotal'], 0, 'R', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
                $pdf->MultiCell(12, $altoCelda, nvl($det['descuento'], "0.00"), 0, 'R', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
                $pdf->MultiCell(20, $altoCelda, nvl($det['baseImponible'],"0.00"), 0, 'R', 1, 1, '', '', true, 0, false, true, $altoCelda, 'M');
                // Fin Detalle Factura
            }
        }else{
            foreach ($detFactura as $det){
                $altoCelda = numLineas($det['descripcion']);
                // Detalle Factura
                $pdf->MultiCell(13, $altoCelda, $det['cantidad'], 0, 'C', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
                $pdf->MultiCell(112, $altoCelda, $det['descripcion'], 0, 'L', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
                $pdf->MultiCell(15, $altoCelda, $det['valor'], 0, 'R', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
                $pdf->MultiCell(15, $altoCelda, $det['subTotal'], 0, 'R', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
                $pdf->MultiCell(12, $altoCelda, nvl($det['descuento'], "0.00"), 0, 'R', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
                $pdf->MultiCell(20, $altoCelda, nvl($det['baseImponible'],"0.00"), 0, 'R', 1, 1, '', '', true, 0, false, true, $altoCelda, 'M');
                // Fin Detalle Factura
            }
        }
    }else{
        foreach ($detFactura as $det){
            $altoCelda = numLineas($det['descripcion']);
            // Detalle Factura
            $pdf->MultiCell(13, $altoCelda, $det['cantidad'], 0, 'C', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
            $pdf->MultiCell(112, $altoCelda, $det['descripcion'], 0, 'L', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
            $pdf->MultiCell(15, $altoCelda, $det['valor'], 0, 'R', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
            $pdf->MultiCell(15, $altoCelda, $det['subTotal'], 0, 'R', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
            $pdf->MultiCell(12, $altoCelda, nvl($det['descuento'], "0.00"), 0, 'R', 1, 0, '', '', true, 0, false, true, $altoCelda, 'M');
            $pdf->MultiCell(20, $altoCelda, nvl($det['baseImponible'],"0.00"), 0, 'R', 1, 1, '', '', true, 0, false, true, $altoCelda, 'M');
            // Fin Detalle Factura
        }
    }
    $var_y = $pdf->GetY();
    $var_page = $pdf->getPage();
    $pdf->SetFont('helvetica', '', 8);
    $pdf->Line(11, $var_y, 198, $var_y, '');
    /*$pdf->writeHTMLCell(110, 24, 11, 247, $htmlInfAdiconal, 0, 1, 0, true, '', true);
    $pdf->writeHTMLCell(70, '', 128, 247, $htmlTotales, 0, 1, 0, true, '', true);
    $pdf->writeHTMLCell(110, 20, 11, 271, $htmlFormasPago, 1, 1, 0, true, '', true); //11*/
    $pdf->writeHTMLCell(110, 24, 11, $var_y, $htmlInfAdiconal, 0, 1, 0, true, '', true);
    $pdf->writeHTMLCell(110, 20, 11, '', $htmlFormasPago, 1, 1, 0, true, '', true); //11
    $pdf->setPage($var_page);
    $pdf->writeHTMLCell(70, '', 128, $var_y, $htmlTotales, 0, 1, 0, true, '', true);
    
    //Close and output PDF document   
    $archivo = "$clave.pdf";    
    $pdf->Output($archivo, 'I');
    
function numLineas($texto){
    $alto = 4;
    $largo = strlen($texto);
    $vmaxlinea = 65;        
    $filas = intdiv($largo, $vmaxlinea);
    if($filas){
        if($filas>0){
            $filas = $filas + 1;
            switch ($filas++) {
                case 1:
                    $alto=4;
                    break;
                case 2:
                    $alto=7;
                    break;
                case 3:
                    $alto=10;
                    break;
                case 4:
                    $alto=13;
                    break;
                case 5:
                    $alto=17;
                    break;
            }
        }
    }
    return $alto;
}
?>