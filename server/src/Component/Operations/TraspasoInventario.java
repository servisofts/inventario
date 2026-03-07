package Component.Operations;

import org.json.JSONArray;
import org.json.JSONObject;
import Component.Modelo;
import Component.InventarioCardex;
import Models.TipoMovimientoCardex;
import Util.ConectInstance;
import Servisofts.SUtil;
import Servisofts.SPGConect;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class TraspasoInventario {
    
    public static void traspaso_inventario(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {
            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            JSONArray detalle = obj.getJSONArray("data");

            if (!obj.has("key_almacen_origen")) {
                throw new Exception("El almacen origen no es valido");
            }

            if (!obj.has("key_almacen_destino")) {
                throw new Exception("El almacen destino no es valido");
            }

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
                    throw new Exception("La cantidad a transferir debe ser mayor a 0");
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
                                + TipoMovimientoCardex.traspaso_egreso.name()
                                + "', null, '{}') as json");

                if (arrInsertado.length() == 0) {
                    throw new Exception("No se pudo retirar el producto del inventario");
                }

                for (int j = 0; j < arrInsertado.length(); j++) {
                    JSONObject productoInventario = arrInsertado.getJSONObject(j);

                    JSONObject movimientoIngreso = InventarioCardex.CrearMovimiento(
                            productoInventario.getString("key_producto"),
                            TipoMovimientoCardex.traspaso_ingreso,
                            productoInventario.getDouble("cantidad") * -1,
                            obj.getString("key_almacen_destino"),
                            obj.getString("key_usuario"));

                    conectInstance.insertObject("inventario_cardex", movimientoIngreso);
                }
            }

            JSONObject historialTraspaso = new JSONObject();
            historialTraspaso.put("key", SUtil.uuid());
            historialTraspaso.put("estado", 1);
            historialTraspaso.put("fecha_on", SUtil.now());
            historialTraspaso.put("key_usuario", obj.getString("key_usuario"));
            historialTraspaso.put("key_empresa", obj.getString("key_empresa"));
            historialTraspaso.put("descripcion", obj.getString("descripcion"));
            historialTraspaso.put("data", obj);
            conectInstance.insertObject("historial_traspaso", historialTraspaso);

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