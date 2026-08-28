package Component.Operations;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import Component.Modelo;
import Component.Suscripcion;
import Component.TipoProducto;
import Component.InventarioCardex;
import Models.TipoMovimientoCardex;
import Util.ConectInstance;
import Servisofts.SUtil;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;
import Servisofts.SocketCliente.SocketCliente;

public class VentaCaja {

    public static void ventaCaja(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {
            conectInstance = new ConectInstance();
            conectInstance.Transacction();
            JSONObject data = obj.getJSONObject("data");
            JSONObject venta = obj.getJSONObject("data").getJSONObject("compra_venta");
            String key_sucursal = venta.getString("key_sucursal");
            JSONArray detalle = obj.getJSONObject("data").getJSONArray("detalle");

            // Validaciones básicas de campos requeridos
            // if (!venta.has("key") || !venta.has("key_usuario") || !venta.has("key_sucursal")) {
            //     throw new Exception("Venta incompleta: falta key, key_usuario o key_sucursal");
            // }
            // if (!data.has("key_almacen")) {
            //     throw new Exception("Falta key_almacen en data");
            // }

            for (int i = 0; i < detalle.length(); i++) {
                JSONObject compra_detalle = detalle.getJSONObject(i);
                if (!compra_detalle.has("key_modelo") || !compra_detalle.has("cantidad")
                        || !compra_detalle.has("precio_unitario_base")) {
                    throw new Exception("El item debe tener key_modelo, cantidad y precio_unitario_base");
                }
                JSONObject modelo = Modelo.getByKey(compra_detalle.getString("key_modelo"));
                if (modelo == null) {
                    throw new Exception("El modelo con key " + compra_detalle.getString("key_modelo")
                            + " no existe o no es valido");
                }
                compra_detalle.put("modelo", modelo);
                double cantidad = compra_detalle.getDouble("cantidad");
                double precio_unitario = compra_detalle.getDouble("precio_unitario_base");
                double descuento = compra_detalle.optDouble("descuento", 0);
                JSONObject tipo_producto = TipoProducto.getByKey(modelo.getString("key_tipo_producto"));
                compra_detalle.put("tipo_producto", tipo_producto);
                JSONObject dataExtra = new JSONObject();
                dataExtra.put("key_compra_venta", venta.getString("key"));
                dataExtra.put("key_compra_venta_detalle", compra_detalle.getString("key"));
                dataExtra.put("precio_unitario_venta", precio_unitario);

                switch (tipo_producto.getString("tipo")) {
                    case "inventario" -> {
                        JSONArray arrInsertado = conectInstance.ejecutarConsultaArray(
                            "select retirar_productos_por_modelo('" + compra_detalle.getString("key_modelo")
                                + "', '"
                                + key_sucursal + "'," + cantidad + ",'" + venta.getString("key_usuario") + "','"
                                + TipoMovimientoCardex.egreso_venta.name()
                                + "', null, '" + dataExtra.toString() + "') as json");

                        if (arrInsertado.length() == 0) {
                            throw new Exception("No se pudo retirar el producto del inventario");
                        }
                        BigDecimal cantidadRetirada = BigDecimal.ZERO;
                        for (int j = 0; j < arrInsertado.length(); j++) {
                            String cantidadStr = arrInsertado.getJSONObject(j).get("cantidad").toString();
                            BigDecimal cantidadItem = new BigDecimal(cantidadStr);

                            cantidadRetirada = cantidadRetirada.add(cantidadItem);
                        }
                        BigDecimal cantidadNegativa = cantidadRetirada.multiply(BigDecimal.valueOf(-1));
                        BigDecimal cantidadSolicitada = BigDecimal.valueOf(cantidad);
                        if (cantidadNegativa.compareTo(cantidadSolicitada) < 0) {
                            throw new Exception(
                                    "No se pudo retirar el producto del inventario, solo quedan " + cantidadRetirada);
                        }
                        compra_detalle.put("cardex", arrInsertado);
                        // Guardar key_producto del cardex para este detalle (inventario)
                        compra_detalle.put("key_producto", arrInsertado.getJSONObject(0).optString("key_producto", ""));
                    }

                    case "servicio" -> {
                        JSONObject producto = new JSONObject();
                        producto.put("key", SUtil.uuid());
                        producto.put("estado", 1);
                        producto.put("fecha_on", SUtil.now());
                        producto.put("key_usuario", venta.getString("key_usuario"));
                        producto.put("key_modelo", compra_detalle.getString("key_modelo"));
                        producto.put("precio", precio_unitario);
                        producto.put("nombre", modelo.getString("descripcion"));
                        producto.put("key_empresa", tipo_producto.getString("key_empresa"));
                        producto.put("key_compra_venta_detalle", compra_detalle.getString("key"));
                        conectInstance.insertObject("producto", producto);
                        JSONObject cardex = InventarioCardex.CrearMovimiento(
                                producto.getString("key"),
                                TipoMovimientoCardex.egreso_venta,
                                cantidad * -1,
                                venta.optString("key_almacen"),
                                venta.getString("key_usuario"), dataExtra);
                        cardex.put("precio_compra", precio_unitario);
                        conectInstance.insertObject("inventario_cardex", cardex);
                        compra_detalle.put("cardex", new JSONArray().put(cardex));
                        compra_detalle.put("key_producto", producto.getString("key"));
                    }
                    case "activo_fijo" -> {
                        throw new Exception("Aun no implementados la opcion de vender activos fijos");
                    }
                    case "consumible" -> {
                        throw new Exception("Aun no implementados la opcion de vender consumibles");
                    }
                }
            }

            JSONObject request = new JSONObject();
            request.put("component", "asiento_contable");
            request.put("type", "venta_caja");
            request.put("data", data);
            JSONObject response = SocketCliente.sendSinc("contabilidad", request);
            if (response == null) {
                throw new Exception("No se recibió respuesta del servicio contabilidad");
            }
            if (!"exito".equals(response.optString("estado"))) {
                String err = response.optString("error", "Error desconocido en contabilidad");
                throw new Exception(err);
            }
            String ___key_producto = null;
            for (int k = 0; k < detalle.length(); k++) {
                JSONObject it = detalle.getJSONObject(k);
                String kp = it.optString("key_producto", "");
                JSONObject tp = it.optJSONObject("tipo_producto");
                if (kp != null && !kp.isEmpty() && tp != null && "servicio".equals(tp.optString("tipo", ""))) {
                    ___key_producto = kp;
                    break;
                }
            }
            // Si no hay servicio, tomar cualquier key_producto disponible
            if (___key_producto == null) {
                for (int k = 0; k < detalle.length(); k++) {
                    JSONObject it = detalle.getJSONObject(k);
                    String kp = it.optString("key_producto", "");
                    if (kp != null && !kp.isEmpty()) {
                        ___key_producto = kp;
                        break;
                    }
                }
            }
            // Fallback: intentar leer cardex[0].key_producto del primer item
            if ((___key_producto == null || ___key_producto.isEmpty()) && detalle.length() > 0) {
                JSONObject it = detalle.getJSONObject(0);
                if (it.has("cardex") && it.getJSONArray("cardex").length() > 0) {
                    ___key_producto = it.getJSONArray("cardex").getJSONObject(0).optString("key_producto", "");
                }
            }

            if (___key_producto == null || ___key_producto.isEmpty()) {
                throw new Exception("No se encontró un producto válido para asociar la suscripción");
            }

            // Registrar suscripciones por cada detalle (si existen), usando su key_producto
            // correspondiente
            Set<String> processedSubs = new HashSet<>();
            for (int i2 = 0; i2 < detalle.length(); i2++) {
                JSONObject compra_det = detalle.getJSONObject(i2);
                JSONArray suscriptores = compra_det.optJSONArray("suscriptores");
                if (suscriptores == null || suscriptores.length() == 0)
                    continue;

                String kp = compra_det.optString("key_producto", "");
                // fallback al cardex si key_producto no está presente
                if ((kp == null || kp.isEmpty()) && compra_det.has("cardex")
                        && compra_det.getJSONArray("cardex").length() > 0) {
                    kp = compra_det.getJSONArray("cardex").getJSONObject(0).optString("key_producto", "");
                }
                if (kp == null)
                    kp = "";

                for (int s = 0; s < suscriptores.length(); s++) {
                    JSONObject suscriptor = suscriptores.getJSONObject(s);
                    suscriptor.put("key_producto", kp);
                    // identificar suscriptor para evitar duplicados (cliente+inicio+sucursal)
                    String sid = suscriptor.optString("key_cliente", "") + "|"
                            + suscriptor.optString("fecha_inicio", "") + "|" + suscriptor.optString("key_sucursal", "");
                    if (processedSubs.contains(sid))
                        continue;
                    processedSubs.add(sid);
                    JSONObject requestSuscripcion = new JSONObject();
                    requestSuscripcion.put("data", suscriptor);
                    requestSuscripcion.put("key_usuario", venta.getString("key_usuario"));
                    Suscripcion.registro(requestSuscripcion, session);
                }
            }

            // Fallback: registrar suscripciones globales en `data.suscripciones` si existen
            if (data.has("suscripciones") && data.getJSONArray("suscripciones").length() > 0) {
                JSONArray suscripciones = data.getJSONArray("suscripciones");
                for (int j = 0; j < suscripciones.length(); j++) {
                    JSONObject suscripcion = suscripciones.getJSONObject(j);
                    // evitar duplicados comparando con los procesados
                    String sid = suscripcion.optString("key_cliente", "") + "|"
                            + suscripcion.optString("fecha_inicio", "") + "|"
                            + suscripcion.optString("key_sucursal", "");
                    if (processedSubs.contains(sid))
                        continue;
                    suscripcion.put("key_producto", ___key_producto);
                    JSONObject requestSuscripcion = new JSONObject();
                    requestSuscripcion.put("data", suscripcion);
                    requestSuscripcion.put("key_usuario", venta.getString("key_usuario"));
                    Suscripcion.registro(requestSuscripcion, session);
                }
            }

            obj.put("data", response.getJSONObject("data"));
            obj.put("estado", "exito");
            conectInstance.commit();
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage() != null ? e.getMessage() : e.toString());
            e.printStackTrace();
            if (conectInstance != null) {
                try {
                    conectInstance.rollback();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }
}
