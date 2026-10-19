<#
.SYNOPSIS
  Prueba de punta a punta de la API: login (3 roles), reserva, aprobación, pago, factura,
  entrega, devolución con daño, validación del ADMIN, incidencias y reportes.

.DESCRIPTION
  Requiere la app corriendo con el perfil dev (datos de prueba cargados).
  Cada ejecución crea una reserva nueva con fechas futuras, así que se puede correr varias veces.
  Al final resuelve la incidencia GRAVE, lo que devuelve la unidad dañada a servicio (el inventario queda igual).

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File docs\pruebas\flujo-completo.ps1
  powershell -ExecutionPolicy Bypass -File docs\pruebas\flujo-completo.ps1 -BaseUrl http://localhost:8081
#>
param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$api = "$BaseUrl/api/v1"
$clave = "Renta2026*"
$script:ok = 0
$script:fallos = 0

# ---------- Utilidades ----------

function Llamar([string]$Metodo, [string]$Ruta, $Token = $null, $Cuerpo = $null) {
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $parametros = @{ Method = $Metodo; Uri = "$api$Ruta"; Headers = $headers }
    if ($null -ne $Cuerpo) {
        $parametros["ContentType"] = "application/json; charset=utf-8"
        $parametros["Body"] = [System.Text.Encoding]::UTF8.GetBytes(($Cuerpo | ConvertTo-Json -Depth 6))
    }
    try {
        $respuesta = Invoke-RestMethod @parametros
        return @{ Estado = 200; Datos = $respuesta }
    } catch {
        $codigo = 0
        if ($_.Exception.Response) { $codigo = [int]$_.Exception.Response.StatusCode }
        return @{ Estado = $codigo; Datos = $null; Error = $_.ErrorDetails.Message }
    }
}

function Verificar([string]$Descripcion, [bool]$Condicion, [string]$Detalle = "") {
    if ($Condicion) {
        Write-Host "  [OK]    $Descripcion" -ForegroundColor Green
        $script:ok++
    } else {
        Write-Host "  [FALLA] $Descripcion $Detalle" -ForegroundColor Red
        $script:fallos++
    }
}

function Esperar-Error([string]$Descripcion, $Resultado, [int]$Codigo) {
    Verificar "$Descripcion -> $Codigo" ($Resultado.Estado -eq $Codigo) "(llegó $($Resultado.Estado): $($Resultado.Error))"
}

function Login([string]$Email) {
    $r = Llamar POST "/auth/login" $null @{ email = $Email; password = $clave }
    if ($r.Estado -ne 200) { throw "No se pudo iniciar sesión con $Email (HTTP $($r.Estado))" }
    return $r.Datos.token
}

function Paso([string]$Titulo) { Write-Host "`n== $Titulo ==" -ForegroundColor Cyan }

# ---------- 1. Autenticación ----------

Paso "1. Autenticación JWT"
$cliente   = Login "juanpablo.ardila@gmail.com"
$proveedor = Login "carlos.rueda@herroriente.com"
$admin     = Login "admin@rentaherramientas.com"
Verificar "Login de CLIENTE, PROVEEDOR y ADMIN" ($cliente -and $proveedor -and $admin)
Esperar-Error "Clave incorrecta" (Llamar POST "/auth/login" $null @{ email = "admin@rentaherramientas.com"; password = "mala" }) 401
Esperar-Error "Ruta protegida sin token" (Llamar GET "/reservas") 401
# Ataque: el cliente cambia su rol a ADMIN dentro del token sin conocer la clave. La firma deja de coincidir.
$partes = $cliente.Split(".")
$payload = $partes[1].Replace("-", "+").Replace("_", "/")
$payload += "=" * ((4 - $payload.Length % 4) % 4)
$json = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload)).Replace('"CLIENTE"', '"ADMIN"')
$falso = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($json)).TrimEnd("=").Replace("+", "-").Replace("/", "_")
$tokenFalso = "$($partes[0]).$falso.$($partes[2])"
Esperar-Error "Token con el rol cambiado a ADMIN (firma no coincide)" (Llamar GET "/clientes" $tokenFalso) 401
Esperar-Error "CLIENTE intentando listar clientes (solo ADMIN)" (Llamar GET "/clientes" $cliente) 403

# ---------- 2. Catálogo y cotización ----------

Paso "2. Catálogo, disponibilidad y cotización"
$inicio = (Get-Date).AddDays(40 + (Get-Random -Maximum 40)).ToString("yyyy-MM-dd")
$fin    = ([datetime]$inicio).AddDays(2).ToString("yyyy-MM-dd")
$herramientaId = 1
$antes = (Llamar GET "/herramientas/$herramientaId").Datos
Verificar "Catálogo público sin token" ($antes.id -eq $herramientaId)
$disp = Llamar GET "/herramientas/$herramientaId/disponibilidad?fechaInicio=$inicio&fechaFin=$fin"
Verificar "Disponibilidad pública ($($disp.Datos.unidadesDisponibles) unidades libres)" ($disp.Datos.unidadesDisponibles -ge 1)
$pedido = @{ fechaInicio = $inicio; fechaFin = $fin; herramientas = @(@{ herramientaId = $herramientaId; cantidad = 1 }) }
$cot = (Llamar POST "/reservas/cotizacion" $cliente $pedido).Datos
Verificar "Cotización: 3 días inclusivos" ($cot.dias -eq 3)
Verificar "Cotización: IVA = 19 % del subtotal" ([math]::Round($cot.subtotal * 0.19, 2) -eq $cot.iva)

# ---------- 3. Reserva y aprobación ----------

Paso "3. Reserva y aprobación"
$reserva = (Llamar POST "/reservas" $cliente $pedido).Datos
Verificar "Cliente crea la reserva $($reserva.codigo) en PENDIENTE" ($reserva.estado -eq "PENDIENTE")
$id = $reserva.id
Esperar-Error "Pagar una reserva PENDIENTE" (Llamar POST "/reservas/$id/pagos" $cliente @{ metodo = "PSE"; titular = "JUAN" }) 422
Esperar-Error "El cliente intenta aprobar su propia reserva" (Llamar PATCH "/reservas/$id/aprobar" $cliente) 403
$aprobada = (Llamar PATCH "/reservas/$id/aprobar" $proveedor).Datos
Verificar "Proveedor aprueba: APROBADA con fecha límite de pago" ($aprobada.estado -eq "APROBADA" -and $aprobada.fechaLimitePago)

# ---------- 4. Pago y factura ----------

Paso "4. Pago y facturación"
$rechazado = (Llamar POST "/reservas/$id/pagos" $cliente @{ metodo = "TARJETA_CREDITO"; numeroTarjeta = "4000000000000000"; titular = "JUAN PABLO ARDILA" }).Datos
Verificar "Tarjeta terminada en 0000: rechazada y la reserva sigue APROBADA" (-not $rechazado.aprobado -and $rechazado.pago.reserva.estado -eq "APROBADA")
$pago = (Llamar POST "/reservas/$id/pagos" $cliente @{ metodo = "TARJETA_CREDITO"; numeroTarjeta = "4242424242424242"; titular = "JUAN PABLO ARDILA" }).Datos
Verificar "Tarjeta 4242: aprobada y la reserva queda PAGADA" ($pago.aprobado -and $pago.pago.reserva.estado -eq "PAGADA")
Verificar "Factura emitida automáticamente ($($pago.pago.factura.numero))" ($null -ne $pago.pago.factura)
Esperar-Error "Pagar otra vez" (Llamar POST "/reservas/$id/pagos" $cliente @{ metodo = "NEQUI"; titular = "JUAN" }) 422
$facturaId = $pago.pago.factura.id
$pdf = Invoke-WebRequest -Uri "$api/facturas/$facturaId/pdf" -Headers @{ Authorization = "Bearer $cliente" } -UseBasicParsing
Verificar "PDF de la factura descargable" ($pdf.Headers["Content-Type"] -like "application/pdf*" -and $pdf.RawContentLength -gt 1000)
Verificar "El proveedor ve la factura" ((Llamar GET "/facturas/$facturaId" $proveedor).Estado -eq 200)

# ---------- 5. Entrega ----------

Paso "5. Entrega"
Esperar-Error "El cliente intenta registrar la entrega" (Llamar POST "/reservas/$id/entrega" $cliente @{ tipo = "RECOGIDA_EN_SEDE" }) 403
Esperar-Error "Entrega a domicilio sin dirección" (Llamar POST "/reservas/$id/entrega" $proveedor @{ tipo = "DOMICILIO" }) 422
$entrega = (Llamar POST "/reservas/$id/entrega" $proveedor @{ tipo = "DOMICILIO"; direccion = "Cra 27 # 36-14, obra Torre Norte"; observaciones = "Prueba automática" }).Datos
Verificar "Proveedor registra la entrega: reserva ENTREGADA" ($entrega.reserva.estado -eq "ENTREGADA")
Esperar-Error "Registrar la entrega dos veces" (Llamar POST "/reservas/$id/entrega" $proveedor @{ tipo = "RECOGIDA_EN_SEDE" }) 409
Verificar "El cliente ve su entrega" ((Llamar GET "/reservas/$id/entrega" $cliente).Estado -eq 200)

# ---------- 6. Devolución con daño GRAVE ----------

Paso "6. Devolución y daños"
$fueraAntes = (Llamar GET "/herramientas/$herramientaId").Datos.unidadesFueraServicio
$devolucion = (Llamar POST "/reservas/$id/devolucion" $proveedor @{
    estadoEquipo = "CON_DANOS"; observaciones = "Prueba automática"
    danos = @(@{ herramientaId = $herramientaId; descripcion = "Carcasa partida por caída"; severidad = "GRAVE"; costoEstimado = 180000 })
}).Datos
Verificar "Proveedor confirma la devolución: reserva DEVUELTA" ($devolucion.reserva.estado -eq "DEVUELTA")
Verificar "La devolución trae el daño reportado" ($devolucion.danos.Count -eq 1 -and $devolucion.danos[0].estado -eq "ABIERTO")
$fueraDespues = (Llamar GET "/herramientas/$herramientaId").Datos.unidadesFueraServicio
Verificar "Daño GRAVE: unidades fuera de servicio $fueraAntes -> $fueraDespues" ($fueraDespues -eq $fueraAntes + 1)
Esperar-Error "Daño de una herramienta que no estaba en la reserva" (Llamar POST "/devoluciones/$($devolucion.id)/danos" $proveedor @{ herramientaId = 2; descripcion = "x"; severidad = "LEVE" }) 422

# ---------- 7. Supervisión del ADMIN ----------

Paso "7. Validación e incidencias (ADMIN)"
Esperar-Error "El proveedor intenta validar la devolución" (Llamar PATCH "/devoluciones/$($devolucion.id)/validar" $proveedor) 403
$validada = (Llamar PATCH "/devoluciones/$($devolucion.id)/validar" $admin).Datos
Verificar "ADMIN valida la devolución" ($validada.validada -and $validada.validadaPor)
Esperar-Error "Validar dos veces" (Llamar PATCH "/devoluciones/$($devolucion.id)/validar" $admin) 422
$danoId = $devolucion.danos[0].id
Esperar-Error "Saltar de ABIERTO a RESUELTO" (Llamar PATCH "/danos/$danoId/estado" $admin @{ estado = "RESUELTO" }) 422
Verificar "Incidencia ABIERTO -> EN_REVISION" ((Llamar PATCH "/danos/$danoId/estado" $admin @{ estado = "EN_REVISION" }).Datos.estado -eq "EN_REVISION")
Verificar "Incidencia EN_REVISION -> RESUELTO" ((Llamar PATCH "/danos/$danoId/estado" $admin @{ estado = "RESUELTO" }).Datos.estado -eq "RESUELTO")
$fueraFinal = (Llamar GET "/herramientas/$herramientaId").Datos.unidadesFueraServicio
Verificar "Al resolver, la unidad reparada vuelve a servicio ($fueraFinal)" ($fueraFinal -eq $fueraAntes)
$misDanos = (Llamar GET "/danos" $cliente).Datos
Verificar "El cliente solo ve daños de sus alquileres" (@($misDanos.contenido | Where-Object { $_.reserva.clienteId -ne 1 }).Count -eq 0)

# ---------- 8. Reportes ----------

Paso "8. Reportes y estadísticas"
$r = Llamar GET "/reportes/resumen" $admin
if ($r.Estado -eq 404) {
    Write-Host "  [--]    Reportes aún no implementados (Fase 7)" -ForegroundColor DarkGray
} else {
    Verificar "Resumen del ADMIN" ($r.Estado -eq 200)
    Verificar "Herramientas más alquiladas" ((Llamar GET "/reportes/herramientas-mas-alquiladas" $admin).Estado -eq 200)
    Verificar "Clientes frecuentes" ((Llamar GET "/reportes/clientes-frecuentes" $admin).Estado -eq 200)
    Verificar "Ingresos por periodo" ((Llamar GET "/reportes/ingresos" $admin).Estado -eq 200)
    Verificar "Métricas propias del proveedor" ((Llamar GET "/reportes/resumen" $proveedor).Estado -eq 200)
    Esperar-Error "El cliente intenta ver reportes" (Llamar GET "/reportes/resumen" $cliente) 403
}

# ---------- Resultado ----------

Write-Host "`nResultado: $script:ok pruebas OK, $script:fallos con falla" -ForegroundColor $(if ($script:fallos -eq 0) { "Green" } else { "Red" })
if ($script:fallos -gt 0) { exit 1 }
