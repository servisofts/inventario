package Component.Operations;

import java.math.BigDecimal;
import org.json.JSONArray;
import org.json.JSONObject;
import Component.Modelo;
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

                        BigDecimal cantidadSolicitada = new BigDecimal(cantidad + ""); // importante si cantidad
                                                                                       // es decimal

                        if (cantidadNegativa.compareTo(cantidadSolicitada) < 0) {
                            throw new Exception(
                                    "No se pudo retirar el producto del inventario, solo quedan " + cantidadRetirada);
                        }
                        compra_detalle.put("cardex", arrInsertado);
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
                                null,
                                venta.getString("key_usuario"), dataExtra);
                        cardex.put("precio_compra", precio_unitario);
                        conectInstance.insertObject("inventario_cardex", cardex);
                        compra_detalle.put("cardex", new JSONArray().put(cardex));
                    }
                    case "activo_fijo" -> {
                        throw new Exception("Aun no implementados la opcion de vender activos fijos");
                    }
                    case "consumible" -> {
                        throw new Exception("Aun no implementados la opcion de vender consumibles");
                    }
                }
                // System.out.println(tipo_producto);
            }

            JSONObject request = new JSONObject();
            request.put("component", "asiento_contable");
            request.put("type", "venta_caja");
            request.put("data", data);
            JSONObject response = SocketCliente.sendSinc("contabilidad", request);
            if (!response.optString("estado").equals("exito")) {
                throw new Exception(response.getString("error"));
            }
            obj.put("data", response.getJSONObject("data"));
            obj.put("estado", "exito");
            conectInstance.commit();
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
            conectInstance.rollback();
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }
}