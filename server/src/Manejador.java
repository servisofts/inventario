import Component.*;
import Servisofts.SConsole;
import org.json.JSONObject;
import Server.SSSAbstract.SSSessionAbstract;

public class Manejador {
    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        if (session != null) {
            SConsole.log(session.getIdSession(), "\t|\t", obj.getString("component"), obj.getString("type"));
        } else {
            SConsole.log("http-server", "-->", obj.getString("component"), obj.getString("type"));
        }
        if (obj.isNull("component")) {
            return;
        }
        switch (obj.getString("component")) {
            case Almacen.COMPONENT:
                Almacen.onMessage(obj, session);
                break;
            case Marca.COMPONENT:
                Marca.onMessage(obj, session);
                break;
            case Modelo.COMPONENT:
                Modelo.onMessage(obj, session);
                break;
            case Producto.COMPONENT:
                Producto.onMessage(obj, session);
                break;
            case UnidadMedida.COMPONENT:
                UnidadMedida.onMessage(obj, session);
                break;
            case InventarioDato.COMPONENT:
                InventarioDato.onMessage(obj, session);
                break;
            case TIpoProductoInventarioDato.COMPONENT:
                TIpoProductoInventarioDato.onMessage(obj, session);
                break;
            case ProductoInventarioDato.COMPONENT:
                ProductoInventarioDato.onMessage(obj, session);
                break;
            case TipoProducto.COMPONENT:
                TipoProducto.onMessage(obj, session);
                break;
        }
    }
}
