package Component;

import java.sql.SQLException;
import org.json.JSONObject;
import Models.TipoMovimientoCardex;
import Servisofts.SUtil;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class InventarioCardex {

    public static final String COMPONENT = "inventario_cardex";

    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        try {
            switch (obj.getString("type")) {
                case "ingreso_compra":
                    ingreso_compra(obj, session);
                    break;
            }
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void ingreso_compra(JSONObject obj, SSSessionAbstract session) {
        String key_modelo = obj.getString("key_modelo");
        String key_almacen = obj.getString("key_almacen");
        // CrearMovimiento()

    }

    public static JSONObject CrearMovimiento(String key_producto, TipoMovimientoCardex movimiento, double cantidad,
            String key_almacen, String key_usuario) throws SQLException {

        JSONObject inventario_cardex = new JSONObject();
        inventario_cardex.put("key", SUtil.uuid());
        inventario_cardex.put("key_usuario", key_usuario);
        inventario_cardex.put("fecha_on", SUtil.now());
        inventario_cardex.put("estado", 1);
        inventario_cardex.put("key_producto", key_producto);
        inventario_cardex.put("key_almacen", key_almacen);
        inventario_cardex.put("cantidad", cantidad);
        inventario_cardex.put("tipo", movimiento.name());
        return inventario_cardex;
    }
}
