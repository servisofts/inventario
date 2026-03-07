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
import Servisofts.SocketCliente.SocketCliente;

public class VentaRapida {
    
    public static void ventaRapida(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {

            JSONObject venta = obj.getJSONObject("venta");

            JSONArray detalle = venta.getJSONArray("detalle");

            if (detalle.length() == 0) {
                obj.put("estado", "error");
                obj.put("error", "El array detalle no puede estar vacio");
                return;
            }

            String key_sucursal = venta.getString("key_sucursal");
            String key_almacen = venta.optString("key_almacen", null);

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
            double total_debe = 0;

            JSONObject moneda = obj.getJSONObject("monedaCaja");

            JSONObject tags = new JSONObject()
                    .put("key_usuario", obj.getJSONObject("venta").getString("key_usuario"))
                    .put("key_compra_venta", venta.getString("key"))
                    .put("key_almacen", key_almacen)
                    .put("key_sucursal", key_sucursal);
            // // JSONArray movimientosContablesInventario = new JSONArray();

            for (int i = 0; i < detalle.length(); i++) {
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

                tags.put("key_modelo", modelo.getString("key"));
                double cantidad = compra_detalle.getDouble("cantidad");
                double precio_unitario = compra_detalle.getDouble("precio_unitario");
                double descuento = compra_detalle.optDouble("descuento", 0);
                JSONObject tipo_producto = TipoProducto.getByKey(modelo.getString("key_tipo_producto"));

                tags.put("key_tipo_producto", tipo_producto.getString("key"));

                String key_cuenta_contable_ingreso = tipo_producto.getString("key_cuenta_contable_ganancia");

                JSONObject dataExtra = new JSONObject();
                dataExtra.put("key_compra_venta", venta.getString("key"));
                dataExtra.put("key_compra_venta_detalle", compra_detalle.getString("key"));
                dataExtra.put("precio_unitario_venta", precio_unitario);

                if (tipo_producto.has("key_cuenta_contable") && !tipo_producto.isNull("key_cuenta_contable")) {
                    String key_cuenta_contable_gasto = tipo_producto.optString("key_cuenta_contable_costo");
                    String key_cuenta_contable = tipo_producto.getString("key_cuenta_contable");

                    JSONArray arrInsertado = conectInstance.ejecutarConsultaArray(
                            "select retirar_productos_por_modelo('" + compra_detalle.getString("key_modelo") + "', '"
                                    + key_sucursal + "'," + cantidad + ",'" + "" + "','"
                                    + TipoMovimientoCardex.egreso_venta.name()
                                    + "', null, '" + dataExtra.toString() + "') as json");
                    // System.out.println(arrInsertado);
                    if (arrInsertado.length() == 0) {
                        throw new Exception("No se pudo retirar el producto del inventario");
                    }

                    double totalCosto = 0;
                    double totalDepreciacion = 0;
                    double cant = 0;
                    for (int j = 0; j < arrInsertado.length(); j++) {
                        JSONObject productoInventario = arrInsertado.getJSONObject(j);
                        cant += productoInventario.getDouble("cantidad") * -1;
                        totalCosto += productoInventario.getDouble("precio_compra")
                                * (productoInventario.getDouble("cantidad") * -1);

                        totalDepreciacion += productoInventario.optDouble("depreciacion", 0)
                                * (productoInventario.getDouble("cantidad"));
                    }

                    if (cant < cantidad) {
                        throw new Exception("El producto no cuenta con stock");
                    }

                    asiento_contable.setDetalle(new AsientoContableDetalle(
                            key_cuenta_contable,
                            "Inventario",
                            "haber",
                            totalCosto,
                            totalCosto * moneda.getDouble("tipo_cambio"),
                            tags));

                    asiento_contable.setDetalle(new AsientoContableDetalle(
                            key_cuenta_contable_gasto,
                            "Costo de ventas",
                            "debe",
                            totalCosto,
                            totalCosto * moneda.getDouble("tipo_cambio"),
                            tags));

                    if (totalDepreciacion > 0) {
                        if (!tipo_producto.has("key_cuenta_contable_depreciacion")
                                || tipo_producto.isNull("key_cuenta_contable_depreciacion")) {
                            throw new Exception("No se encontró la cuenta contable de depreciación");
                        }

                        asiento_contable.setDetalle(new AsientoContableDetalle(
                                tipo_producto.optString("key_cuenta_contable_depreciacion"),
                                "Depreciacións",
                                "haber",
                                totalDepreciacion,
                                totalDepreciacion * moneda.getDouble("tipo_cambio"),
                                tags));

                    }

                } else {

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
                            almacen.getString("key"),
                            venta.getString("key_usuario"),
                            dataExtra);

                    conectInstance.insertObject("inventario_cardex", cardex);
                }

                double ingreso = (precio_unitario * cantidad) - descuento;
                double impuesto = obj.optDouble("porcentaje_impuesto", 0);
                if (impuesto > 0)
                    impuesto = impuesto / 100;
                asiento_contable.setDetalle(new AsientoContableDetalle(
                        key_cuenta_contable_ingreso,
                        "Ventas",
                        "haber",
                        ingreso - (ingreso * impuesto),
                        ingreso - (ingreso * impuesto) * moneda.getDouble("tipo_cambio"),
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