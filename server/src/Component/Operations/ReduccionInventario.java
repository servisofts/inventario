package Component.Operations;

import org.json.JSONArray;
import org.json.JSONObject;
import Component.Modelo;
import Component.TipoProducto;
import Contabilidad.ContaHook;
import Models.TipoMovimientoCardex;
import Util.ConectInstance;
import Servisofts.Contabilidad.AsientoContable;
import Servisofts.Contabilidad.AsientoContableDetalle;
import Servisofts.Contabilidad.AsientoContableTipo;
import Servisofts.SUtil;
import Servisofts.SPGConect;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class ReduccionInventario {

    public static void reduccion_inventario(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {
            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            JSONArray detalle = obj.getJSONArray("data");

            if (!obj.has("key_almacen_origen")) {
                throw new Exception("El almacen origen no es valido");
            }

            String key_empresa = obj.getString("key_empresa");
            String key_cuenta_perdida = ContaHook.getAjusteEmpresa(key_empresa, "inventario_perdida_por_merma")
                    .optString("key");

            AsientoContable asiento = new AsientoContable(AsientoContableTipo.egreso);
            asiento.descripcion = "Reducción de inventario: " + obj.optString("descripcion", "");
            asiento.observacion = obj.optString("motivo_reduccion", "");
            asiento.key_empresa = key_empresa;
            asiento.key_usuario = obj.getString("key_usuario");

            JSONObject dataExtra = new JSONObject();
            dataExtra.put("motivo_reduccion", obj.optString("motivo_reduccion", ""));
            dataExtra.put("descripcion", obj.optString("descripcion", ""));

            for (int i = 0; i < detalle.length(); i++) {
                JSONObject item = detalle.getJSONObject(i);

                if (!item.has("key_modelo") || !item.has("cantidad")) {
                    throw new Exception(
                            "El item debe tener key_modelo y cantidad");
                }

                JSONObject modelo = Modelo.getByKey(item.getString("key_modelo"));
                if (modelo == null) {
                    throw new Exception("El modelo con key " + item.getString("key_modelo")
                            + " no existe o no es valido");
                }
                double cantidad = item.getDouble("cantidad");

                if (cantidad <= 0) {
                    throw new Exception("La cantidad a reducir debe ser mayor a 0");
                }

                int cantidadAlmacen = SPGConect.ejecutarConsultaInt(
                        "select get_stock_modelo('" + item.getString("key_modelo") + "', '"
                                + obj.getString("key_almacen_origen") + "') as json");

                if (cantidadAlmacen < cantidad) {
                    throw new Exception("No hay suficiente stock del modelo: " + modelo.getString("descripcion")
                            + " en el almacen origen");
                }

                JSONArray arrInsertado = conectInstance.ejecutarConsultaArray(
                        "select retirar_productos_por_modelo_almacen('" + item.getString("key_modelo") + "', '"
                                + obj.getString("key_almacen_origen") + "'," + cantidad + ",'"
                                + obj.getString("key_usuario") + "','"
                                + TipoMovimientoCardex.perdida.name()
                                + "', null, '" + dataExtra.toString() + "') as json");

                if (arrInsertado.length() == 0) {
                    throw new Exception("No se pudo retirar el producto del inventario");
                }

                double montoPerdida = 0;
                for (int j = 0; j < arrInsertado.length(); j++) {
                    JSONObject productoInventario = arrInsertado.getJSONObject(j);
                    montoPerdida += productoInventario.getDouble("precio_compra")
                            * (productoInventario.getDouble("cantidad") * -1);
                }

                JSONObject tipo_producto = TipoProducto.getByKey(modelo.getString("key_tipo_producto"));

                JSONObject tags = new JSONObject();
                tags.put("key_modelo", modelo.getString("key"));
                tags.put("key_almacen", obj.getString("key_almacen_origen"));

                asiento.setDetalle(new AsientoContableDetalle(
                        key_cuenta_perdida,
                        "Pérdida de inventario: " + modelo.getString("descripcion"),
                        "debe",
                        montoPerdida,
                        montoPerdida,
                        tags));

                asiento.setDetalle(new AsientoContableDetalle(
                        tipo_producto.getString("key_cuenta_contable"),
                        "Inventario: " + modelo.getString("descripcion"),
                        "haber",
                        montoPerdida,
                        montoPerdida,
                        tags));
            }

            JSONObject respuestaAsiento = asiento.enviar();
            JSONObject asientoData = respuestaAsiento.getJSONObject("data");
            obj.put("key_asiento", asientoData.getString("key"));
            obj.put("codigo_asiento", asientoData.getString("codigo"));

            JSONObject historialReduccion = new JSONObject();
            historialReduccion.put("key", SUtil.uuid());
            historialReduccion.put("estado", 1);
            historialReduccion.put("fecha_on", SUtil.now());
            historialReduccion.put("key_usuario", obj.getString("key_usuario"));
            historialReduccion.put("key_empresa", key_empresa);
            historialReduccion.put("descripcion", obj.getString("descripcion"));
            historialReduccion.put("data", obj);
            conectInstance.insertObject("historial_traspaso", historialReduccion);

            obj.put("asiento_contable", asientoData);
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
