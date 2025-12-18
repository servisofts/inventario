import Component.*;
import Servisofts.SConsole;
import org.json.JSONObject;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class Manejador {
    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        if (session != null) {
            SConsole.log(session.getIdSession(), "\t|\t", obj.getString("component"),
                    obj.getString("type"));
        } else {
            SConsole.log("http-server", "-->", obj.getString("component"), obj.getString("type"));
        }
        if (obj.isNull("component")) {
            return;
        }
        switch (obj.getString("component")) {
            case Almacen.COMPONENT: Almacen.onMessage(obj, session); break;
            case Marca.COMPONENT: Marca.onMessage(obj, session); break;
            case Producto.COMPONENT: Producto.onMessage(obj, session); break;
            case UnidadMedida.COMPONENT: UnidadMedida.onMessage(obj, session); break;
            case InventarioDato.COMPONENT: InventarioDato.onMessage(obj, session); break;
            case TIpoProductoInventarioDato.COMPONENT: TIpoProductoInventarioDato.onMessage(obj, session); break;
            case ProductoInventarioDato.COMPONENT: ProductoInventarioDato.onMessage(obj, session); break;
            case TipoProducto.COMPONENT: TipoProducto.onMessage(obj, session); break;
            case ProductoHistorico.COMPONENT: ProductoHistorico.onMessage(obj, session); break;
            case ProductoEntrega.COMPONENT: ProductoEntrega.onMessage(obj, session); break;
            case ProductoIngrediente.COMPONENT: ProductoIngrediente.onMessage(obj, session); break;
            case ProductoUtilizado.COMPONENT: ProductoUtilizado.onMessage(obj, session); break;
            case Modelo.COMPONENT: Modelo.onMessage(obj, session); break;
            case ModeloIngrediente.COMPONENT: ModeloIngrediente.onMessage(obj, session); break;
            case Ingrediente.COMPONENT: Ingrediente.onMessage(obj, session); break;
            case Receta.COMPONENT: Receta.onMessage(obj, session); break;
            case CategoriaProducto.COMPONENT: CategoriaProducto.onMessage(obj, session); break;
            case SubProducto.COMPONENT: SubProducto.onMessage(obj, session); break;
            case SubProductoDetalle.COMPONENT: SubProductoDetalle.onMessage(obj, session); break;
            case ConteoManualInventario.COMPONENT: ConteoManualInventario.onMessage(obj, session); break;
            case Proveedor.COMPONENT: Proveedor.onMessage(obj, session); break;
            case ModeloProveedor.COMPONENT: ModeloProveedor.onMessage(obj, session); break;
            case ModeloCliente.COMPONENT: ModeloCliente.onMessage(obj, session); break;
            case ModeloTag.COMPONENT: ModeloTag.onMessage(obj, session); break;
            case Tag.COMPONENT: Tag.onMessage(obj, session); break;
            case DB.COMPONENT: DB.onMessage(obj, session); break;
            case Reporte.COMPONENT: Reporte.onMessage(obj, session); break;
        }
    }
}
