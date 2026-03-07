package Component.Operations;

import org.json.JSONArray;
import org.json.JSONObject;
import Component.Modelo;
import Component.TipoProducto;
import Component.InventarioCardex;
import Models.TipoMovimientoCardex;
import Util.ConectInstance;
import Servisofts.Contabilidad.AsientoContable;
import Servisofts.Contabilidad.AsientoContableDetalle;
import Servisofts.Contabilidad.Contabilidad;
import Servisofts.SUtil;
import Servisofts.SPGConect;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;
import Servisofts.SPGConect;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;
import Servisofts.SocketCliente.SocketCliente;

public class CompraRapida {
    
    public static void compraRapida(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {

            JSONObject compra = obj.getJSONObject("compra");

            JSONArray detalle = compra.getJSONArray("detalle");

            if (detalle.length() == 0) {
                obj.put("estado", "error");
                obj.put("error", "El array detalle no puede estar vacio");
                return;
            }

            String key_sucursal = compra.getString("key_sucursal");
            String key_almacen = compra.optString("key_almacen", null);

            // Obtenemos el almacen 1 hay q ver q hacer con esto
            JSONObject almacen = null;

            if (key_almacen == null) {
                almacen = SPGConect.ejecutarConsultaObject(
                        "select to_json(almacen.*) as json \n"
                                + "from almacen where  almacen.estado > 0 and almacen.key_sucursal = '"
                                + key_sucursal + "' limit 1");
            } else {
                almacen = SPGConect.ejecutarConsultaObject(
                        "select to_json(almacen.*) as json \n"
                                + "from almacen where  almacen.estado > 0 and almacen.key = '"
                                + key_almacen + "' limit 1");
            }

            if (almacen == null || almacen.isEmpty()) {
                throw new Exception("No hay un almacen configurado para esta sucursal");
            }

            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            AsientoContable asiento_contable = AsientoContable.fromJSON(obj.optJSONObject("asiento_contable"));

            double porc_iva = Contabilidad.getEnviroment(asiento_contable.key_empresa, "IVA").optDouble("observacion",
                    0);
            // JSONArray movimientosContablesInventario = new JSONArray();

            for (int i = 0; i < detalle.length(); i++) {

                JSONObject tags = obj.getJSONObject("tags")
                        .put("key_usuario", obj.getJSONObject("compra").getString("key_usuario"))
                        .put("key_compra_venta", compra.getString("key"))
                        .put("key_almacen", key_almacen)
                        .put("key_sucursal", key_sucursal);

                JSONObject compra_detalle = detalle.getJSONObject(i);

                if (!compra_detalle.has("key_modelo") || !compra_detalle.has("cantidad")
                        || !compra_detalle.has("precio_unitario")) {
                    throw new Exception("El item debe tener key_modelo, cantidad y precio_unitario");
                }

                JSONObject modelo = Modelo.getByKey(compra_detalle.getString("key_modelo"));
                if (modelo == null) {
                    throw new Exception("El modelo con key " + compra_detalle.getString("key_modelo")
                            + " no existe o no es valido");
                }
                double cantidad = compra_detalle.getDouble("cantidad");
                double precio_unitario = compra_detalle.getDouble("precio_unitario");
                if (compra.optBoolean("facturar", false)) {
                    // if(!compra.optBoolean("facturar_luego", false)){
                    precio_unitario = (precio_unitario / (1 + (porc_iva / 100)));
                    // precio_unitario=Math.round(precio_unitario * 100.0) /100.0;
                    // }

                }
                double subtotal = cantidad * precio_unitario;
                JSONObject tipo_producto = TipoProducto.getByKey(modelo.getString("key_tipo_producto"));

                JSONObject producto = new JSONObject();
                producto.put("key", SUtil.uuid());
                producto.put("estado", 1);
                producto.put("fecha_on", SUtil.now());
                producto.put("key_usuario", compra.getString("key_usuario"));
                producto.put("key_modelo", compra_detalle.getString("key_modelo"));
                producto.put("precio_compra", precio_unitario);
                producto.put("nombre",
                        modelo.getString("descripcion") + " - " + compra_detalle.optString("detalle", ""));
                producto.put("key_empresa", tipo_producto.getString("key_empresa"));
                producto.put("key_compra_venta_detalle", compra_detalle.getString("key"));

                conectInstance.insertObject("producto", producto);

                
                JSONObject dataExtra = new JSONObject();
                dataExtra.put("key_compra_venta", compra.getString("key"));
                dataExtra.put("key_compra_venta_detalle", compra_detalle.getString("key"));
                dataExtra.put("precio_unitario_compra", precio_unitario);

                String key_cuenta_contable = "";
                if (tipo_producto.has("key_cuenta_contable") && !tipo_producto.isNull("key_cuenta_contable")) {

                    JSONObject cardex = InventarioCardex.CrearMovimiento(
                            producto.getString("key"),
                            TipoMovimientoCardex.ingreso_compra,
                            cantidad,
                            almacen.getString("key"),
                            compra.getString("key_usuario"),dataExtra);

                    conectInstance.insertObject("inventario_cardex", cardex);
                    key_cuenta_contable = tipo_producto.getString("key_cuenta_contable");
                } else {
                    key_cuenta_contable = tipo_producto.getString("key_cuenta_contable_costo");

                    JSONObject cardex = InventarioCardex.CrearMovimiento(
                            producto.getString("key"),
                            TipoMovimientoCardex.ingreso_compra,
                            cantidad,
                            almacen.getString("key"),
                            compra.getString("key_usuario"),dataExtra);

                    conectInstance.insertObject("inventario_cardex", cardex);
                }

                subtotal = Math.round(subtotal * 100.0) / 100.0;

                tags.put("key_tipo_producto", tipo_producto.getString("key"));
                tags.put("key_modelo", compra_detalle.getString("key_modelo"));

                asiento_contable.setDetalle(new AsientoContableDetalle(
                        key_cuenta_contable,
                        "Compra rapida - " + compra_detalle.optString("detalle"),
                        "debe",
                        subtotal,
                        subtotal,
                        tags));

            }

            asiento_contable.enviar();
            obj.put("asiento_contable", asiento_contable.toJSON());
            // JSONObject data = new JSONObject();

            // String key_modelo = obj.getString("key_modelo");
            // JSONArray data = SPGConect.ejecutarConsultaArray("select
            // modelo_getallproductos('" + key_modelo + "') as json");

            // obj.put("data", data);
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