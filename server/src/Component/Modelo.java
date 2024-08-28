package Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONObject;

import Servisofts.SConfig;
import Servisofts.SPGConect;
import Servisofts.SPGConectInstance;
import Servisofts.SUtil;
import Server.SSSAbstract.SSSessionAbstract;

public class Modelo {
    public static final String COMPONENT = "modelo";

    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        switch (obj.getString("type")) {
            case "getAll":
                getAll(obj, session);
                break;
            case "getByKey":
                getByKey(obj, session);
                break;
            case "registro":
                registro(obj, session);
                break;
            case "editar":
                editar(obj, session);
                break;
            case "buscarIngredientes":
                buscarIngredientes(obj, session);
                break;
            case "procesar":
                procesar(obj, session);
                break;
            case "getCarrito":
                getCarrito(obj, session);
                break;
        }
    }

    public static JSONObject  buscarIgrediente(String key_modelo, JSONArray productos){
        JSONObject producto;
        for (int i = 0; i < productos.length(); i++) {
            producto = productos.getJSONObject(i);
            if(producto.getString("key_modelo").equals(key_modelo)){
                System.out.println("Ingrediente encontrado.---");
                JSONObject producto_ = new JSONObject(producto.toString());
                productos.remove(i);
                return producto_;
            }
        }
        return null;
    }

    public static JSONArray getProductosInventario(SPGConectInstance conectInstance, String key_modelo, String key_sucursal){
        
        try {
            String consulta = "select array_to_json(array_agg(tabla.*))::json as json \n"+
            "from ( \n"+
            "select producto.key, \n"+
            "  sum(producto.cantidad) as cantidad, \n"+
            "  sum(producto.precio_compra) as precio_compra, \n"+
            "  sum(producto.precio_compra) / sum(producto.cantidad) as precio_compra_unitario \n"+
            "from producto, \n"+
            "  almacen \n"+
            "where producto.key_modelo = '"+key_modelo+"' \n"+
            "  and producto.key_almacen = almacen.key \n"+
            "  and almacen.estado > 0 \n"+
            "  and producto.estado > 0 \n"+
            "  and almacen.key_sucursal = '"+key_sucursal+"' \n"+
            "  and producto.cantidad > 0  \n"+
            "group by producto.key \n"+
            "    order by producto.fecha_on asc \n"+
            ") tabla";
            return conectInstance.ejecutarConsultaArray(consulta);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public static void getCarrito(JSONObject obj, SSSessionAbstract session) {
        
        try {
            String consulta = "select array_to_json(array_Agg(tabla.*)) as json from (\n"+
            "    select to_json(tipo_producto.*) as tipo_producto, \n"+
            "    to_json(modelo.*) as modelo, \n"+
            "    sum(producto.cantidad) as cantidad \n"+            
            "    from modelo, \n"+
            "    almacen, \n"+
            "    producto, \n"+
            "    tipo_producto \n"+
            "    where tipo_producto.key = modelo.key_tipo_producto \n"+
            "    and almacen.key_sucursal = '"+obj.getString("key_sucursal")+"' \n"+
            "    and almacen.is_venta = true \n"+
            "    and producto.key_almacen = almacen.key \n"+
            "    and almacen.estado > 0 \n"+
            "    and producto.estado > 0 \n"+
            "    and producto.cantidad > 0 \n"+
            "    and modelo.key = producto.key_modelo \n"+
            "    group by modelo.key, \n"+
            "    tipo_producto.key \n"+
            ") tabla"; 
            JSONArray json = SPGConect.ejecutarConsultaArray(consulta);
            obj.put("estado", "exito");
            obj.put("data", json);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            obj.put("estado", "error");
            obj.put("error", e.getLocalizedMessage());
        }
    }
    
    public static void procesar(JSONObject obj, SSSessionAbstract session) {
        SPGConectInstance conectInstance = new SPGConectInstance(SConfig.getJSON("data_base"));
        try {
            conectInstance.Transacction();

            JSONArray pendientes = buscarIngredientesPendientes(obj);
            if (pendientes.length() > 0) {
                obj.put("estado", "error");
                obj.put("error", "Productos Pendientes");
                obj.put("data", pendientes);
                closeConectInstance(conectInstance);
                return;
            }

            // Producto que se va a crear
            JSONObject productoNew = Modelo.getByKey(obj.getString("key_modelo"));
            productoNew.put("key", SUtil.uuid());
            productoNew.put("fecha_on", SUtil.now());
            productoNew.put("estado", 1);
            productoNew.put("key_almacen", obj.getString("key_almacen"));
            productoNew.put("key_modelo", obj.getString("key_modelo"));
            productoNew.put("cantidad", obj.getDouble("cantidad"));

            JSONArray productoIngredientes = procesarIngredientes(conectInstance, obj, productoNew);

            conectInstance.insertObject("producto", productoNew);
            conectInstance.insertArray("producto_ingrediente", productoIngredientes);

            // Hacer la Contabilidad

            conectInstance.commit();

            obj.put("data", productoNew);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
            rollbackConectInstance(conectInstance);
        } finally {
            closeConectInstance(conectInstance);
        }
    }

    private static JSONArray buscarIngredientesPendientes(JSONObject obj)
            throws SQLException {
        String consulta = "select array_to_json(array_agg(tabla.*))::json as json \n" +
                "from ( \n" +
                "select modelo_ingrediente.key_modelo_ingrediente, \n" +
                "       sum(modelo_ingrediente.cantidad) as cantidad_ingredientes_requeridos, \n" +
                "( \n" +
                "    select sum(producto.cantidad) \n" +
                "    from producto, almacen \n" +
                "    where producto.key_modelo = modelo_ingrediente.key_modelo_ingrediente \n" +
                "    and producto.key_almacen = almacen.key \n" +
                "    and almacen.estado > 0 \n" +
                "    and almacen.key_sucursal = '" + obj.getString("key_sucursal") + "' \n" +
                "    and producto.estado > 0 \n" +
                ")as cantidad_ingredientes_disponibles \n" +
                "from modelo, \n" +
                "     modelo_ingrediente \n" +
                "where modelo.key = '" + obj.getString("key_modelo") + "' \n" +
                "  and modelo_ingrediente.key_modelo = modelo.key \n" +
                "  and modelo.estado > 0  \n" +
                "  and modelo_ingrediente.estado > 0  \n" +
                "group by modelo_ingrediente.key_modelo_ingrediente \n" +
                ") tabla \n" +
                "where tabla.cantidad_ingredientes_disponibles < tabla.cantidad_ingredientes_requeridos";
        return SPGConect.ejecutarConsultaArray(consulta);
    }

    private static JSONArray procesarIngredientes(SPGConectInstance conectInstance, JSONObject obj,
            JSONObject productoNew) throws SQLException {

        BigDecimal precioCompraProductoNew = BigDecimal.ZERO;
        double cantidadProductoNew = productoNew.getDouble("cantidad");

        JSONObject ingredientesModelo = ModeloIngrediente.getAll(obj.getString("key_modelo"));
        JSONArray productoIngredientes = new JSONArray();

        if (ingredientesModelo == null || ingredientesModelo.isEmpty()) {
            productoNew.put("precio_compra", precioCompraProductoNew);
            return productoIngredientes;
        }

        JSONObject ingredienteModelo;
        BigDecimal precioCompraIngredienteNew = BigDecimal.ZERO;
        BigDecimal precioCompraProductoInventario = BigDecimal.ZERO;
        BigDecimal precioCompraUnitarioProductoInventario = BigDecimal.ZERO;
        for (int i = 0; i < JSONObject.getNames(ingredientesModelo).length; i++) {

            ingredienteModelo = ingredientesModelo.getJSONObject(JSONObject.getNames(ingredientesModelo)[i]);

            JSONArray productosInventario = getProductosInventario(conectInstance, ingredienteModelo.getString("key_modelo_ingrediente"), obj.getString("key_sucursal"));
            JSONObject productoInventario;

            double cantidadTotalIngredienteNew = ingredienteModelo.getDouble("cantidad") * cantidadProductoNew;
            double cantidad = cantidadTotalIngredienteNew;
            double cantidadIngredienteUsado;
            double cantidadProductoInventario;
            int j = 0;
            while (cantidad > 0) {
                productoInventario = productosInventario.getJSONObject(j);
                j++;

                JSONObject ingredienteNew = new JSONObject();
                cantidadIngredienteUsado = .0;
                cantidadProductoInventario = productoInventario.getDouble("cantidad");
                precioCompraProductoInventario = new BigDecimal(productoInventario.get("precio_compra") + "");
                precioCompraUnitarioProductoInventario = new BigDecimal(productoInventario.get("precio_compra_unitario") + "");

                // Actualizo el Producto del Invetario
                if (cantidad <= cantidadProductoInventario) {
                    cantidadIngredienteUsado = cantidad;
                    cantidad = 0;
                } else {
                    cantidadIngredienteUsado = cantidadProductoInventario;
                    cantidad -= cantidadIngredienteUsado;
                }
                // Calculo Datos para el Ingrediente Nuevo
                precioCompraIngredienteNew = precioCompraUnitarioProductoInventario.multiply(BigDecimal.valueOf(cantidadIngredienteUsado));

                // Calculo Datos para Actualizar el Producto del Inventario
                cantidadProductoInventario -= cantidadIngredienteUsado;
                precioCompraProductoInventario = precioCompraProductoInventario.subtract(precioCompraIngredienteNew, MathContext.DECIMAL128);
                productoInventario.put("cantidad", cantidadProductoInventario);
                productoInventario.put("precio_compra", precioCompraProductoInventario.toString());
                conectInstance.editObject("producto", productoInventario);

                ingredienteNew.put("key", SUtil.uuid());
                ingredienteNew.put("key_producto", productoNew.getString("key"));
                ingredienteNew.put("key_usuario", obj.getString("key_usuario"));
                ingredienteNew.put("key_producto_ingrediente", productoInventario.getString("key"));
                ingredienteNew.put("cantidad", cantidadIngredienteUsado);
                ingredienteNew.put("precio_compra", precioCompraIngredienteNew.toString());
                ingredienteNew.put("fecha_on", SUtil.now());
                ingredienteNew.put("estado", 1);
                ingredienteNew.put("tipo", "ingreso");
                productoIngredientes.put(ingredienteNew);

                precioCompraProductoNew = precioCompraProductoNew.add(precioCompraIngredienteNew);
            }
        }

        productoNew.put("precio_compra", precioCompraProductoNew);
        return productoIngredientes;
    }

    // todo ver como sacar esto de aca
    private static void rollbackConectInstance(SPGConectInstance conectInstance) {
        try {
            conectInstance.rollback();
        } catch (Exception e1) {

        }
    }
    
    private static void closeConectInstance(SPGConectInstance conectInstance) {
        try {
            conectInstance.getConexion().close();
        } catch (Exception e1) {

        }
    }



    public static void buscarIngredientes(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = ""+
            "select array_to_json(array_agg(tabla.*))::json as json  \n"+
            "from(  \n"+
            "select almacen.key as key_almacen, \n"+
            "producto.key_modelo, \n"+
            "modelo.key_tipo_producto, \n"+
            "modelo.descripcion as modelo, \n"+
            "almacen.descripcion,   \n"+
            "sum(producto.cantidad) as cantidad   \n"+
            "from producto,   \n"+
            "modelo,   \n"+
            "almacen   \n"+
            "where producto.estado > 0   \n"+
            "and almacen.estado > 0   \n"+
            "and modelo.key = producto.key_modelo   \n"+
            "and almacen.key = producto.key_almacen   \n"+
            "and almacen.key_sucursal = '"+obj.getString("key_sucursal")+"'   \n"+
            "and producto.cantidad > 0   \n"+
            "and producto.key_modelo in (   \n"+
            "select modelo.key   \n"+
            "from modelo   \n"+
            "where modelo.key in (   \n"+
            "SELECT modelo_ingrediente.key_modelo_ingrediente   \n"+
            "FROM  modelo_ingrediente   \n"+
            "where modelo_ingrediente.estado > 0   \n"+
            "and modelo_ingrediente.key_modelo = '"+obj.getString("key_modelo")+"'   \n"+
            ")	   \n"+
            ")   \n"+
            "group by almacen.key,   \n"+
            "almacen.descripcion,  \n"+
            "producto.key_modelo, modelo.descripcion, modelo.key_tipo_producto \n"+
            ") tabla  ";

            JSONArray data = SPGConect.ejecutarConsultaArray(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_all('" + COMPONENT + "') as json";
            if(obj.has("key_empresa")){
                consulta = "select get_modelos('" + obj.get("key_empresa") + "') as json";
            }
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject getByDescripcion(String descripcion) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', 'descripcion', '" + descripcion + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            return null;

        }
    }   

    public static void getByKey(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', '" + obj.getString("key") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static JSONObject getByKey(String key) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', '" + key + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void registro(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            data.put("key", SUtil.uuid());
            data.put("estado", 1);
            data.put("fecha_on", SUtil.now());
            data.put("key_usuario", obj.getString("key_usuario"));
            SPGConect.insertArray(COMPONENT, new JSONArray().put(data));
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void editar(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            SPGConect.editObject(COMPONENT, data);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

}
