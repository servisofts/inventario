package Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONObject;

import Contabilidad.ContaHook;
import Models.TipoMovimientoCardex;
import Servisofts.SConfig;
import Servisofts.SPGConect;
import Servisofts.SPGConectInstance;
import Servisofts.SUtil;
import Servisofts.Contabilidad.AsientoContable;
import Servisofts.Contabilidad.AsientoContableDetalle;
import Servisofts.Contabilidad.Contabilidad;
import Util.ConectInstance;
import picocli.CommandLine.Model;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;
import Servisofts.SocketCliente.SocketCliente;

public class Modelo {
    public static final String COMPONENT = "modelo";

    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        switch (obj.getString("type")) {
            case "getAll":
                getAll(obj, session);
                break;
            case "getAllStock":
                getAllStock(obj, session);
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
            case "addStock":
                addStock(obj, session);
                break;
            case "getAllProductos":
                getAllProductos(obj, session);
                break;
            case "compraCaja":
                compraCaja(obj, session);
                break;
            case "ventaCaja":
                ventaCaja(obj, session);
                break;
            case "compraRapida":
                compraRapida(obj, session);
                break;
            case "ventaRapida":
                ventaRapida(obj, session);
                break;
            case "producir":
                producir(obj, session);
                break;
        }
    }

    public static double getStock(String key_modelo, String key_sucursal) {
        try {
            String consulta = "select get_stock_modelo_sucursal('" + key_modelo + "', '" + key_sucursal + "') as json";
            String a= SPGConect.ejecutarConsultaString(consulta);
            return Double.parseDouble(a);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void producir(JSONObject obj, SSSessionAbstract session) {
        
        try{
            String key_sucursal = obj.getString("key_sucursal");
            String key_almacen = obj.getString("key_almacen");
            String key_modelo = obj.getString("key_modelo");

            //JSONObject sucursal = SocketCliente.sendSinc("empresa", new JSONObject().put("component", "sucursal").put("type", "getByKey").put("key", key_sucursal));
            //sucursal = sucursal.getJSONObject("data");
            JSONObject modeloProducto = Modelo.getByKey(key_modelo);
            JSONObject ingredientesReal = Ingrediente.getAll(key_modelo);
            JSONObject almacen = Almacen.getByKey(key_almacen);



            JSONObject ingredientes = obj.getJSONObject("data");
            JSONArray failed = new JSONArray();

            for (int i = 0; i < JSONObject.getNames(ingredientes).length; i++) {
                String key_ingrediente = JSONObject.getNames(ingredientes)[i];
                JSONObject ingrediente = ingredientes.getJSONObject(key_ingrediente);
                System.out.println(ingrediente.optString("descripcion")+": "+ingrediente.optDouble("cantidad"));

                double cantidad_ingrediente_solicitada = 0;
                for (int j = 0; j < ingrediente.getJSONArray("modelo_ingrediente").length(); j++) {
                    JSONObject modelo_ingrediente = ingrediente.getJSONArray("modelo_ingrediente").getJSONObject(j);
                    JSONObject modelo = Modelo.getByKey(modelo_ingrediente.getString("key_modelo"));
                    cantidad_ingrediente_solicitada += modelo_ingrediente.optDouble("cantidad",0);
                    System.out.println(" - "+modelo.optString("descripcion")+": "+modelo_ingrediente.optDouble("cantidad"));
                    double stock = getStock(modelo.getString("key"), key_sucursal);
                    System.out.println("Stock: "+stock);
                    if(stock < ingrediente.optDouble("cantidad")){
                        failed.put(modelo);
                    }
                }
                if(ingredientesReal.getJSONObject(key_ingrediente).optBoolean("is_required") == true){
                    if(cantidad_ingrediente_solicitada < ingredientesReal.getJSONObject(key_ingrediente).optDouble("cantidad")){
                        obj.put("estado", "error");
                        obj.put("error", "Ingredientes incompletos para " + ingrediente.optString("descripcion"));
                        return;
                    }
                }

                if(cantidad_ingrediente_solicitada > ingredientesReal.getJSONObject(key_ingrediente).optDouble("cantidad")){
                    obj.put("estado", "error");
                    obj.put("error", "Demasiados ingredientes para " + ingrediente.optString("descripcion"));
                    return;
                }


            }

            if(failed.length() > 0){
                obj.put("estado", "error");
                obj.put("error", "Ingredientes insuficientes en stock");
                obj.put("data_error", failed);
                return;
            }

            JSONObject producto = new JSONObject();
            producto.put("key", SUtil.uuid());
            producto.put("fecha_on", SUtil.now());
            producto.put("estado", 1);
            producto.put("key_usuario", obj.getString("key_usuario"));
            producto.put("descripcion", modeloProducto.optString("descripcion"));
            producto.put("observacion", modeloProducto.optString("observacion"));
            producto.put("key_modelo", key_modelo);
            producto.put("codigo", "XXXX");
            producto.put("nombre", modeloProducto.optString("descripcion"));
            //producto.put("key_empresa", sucursal.getString("key_empresa"));


            JSONObject inventarioKardex = new JSONObject();
            inventarioKardex.put("key", SUtil.uuid());
            inventarioKardex.put("fecha_on", SUtil.now());
            inventarioKardex.put("key_usuario", obj.getString("key_usuario"));
            inventarioKardex.put("estado", 1);
            inventarioKardex.put("key_producto", producto.getString("key"));
            inventarioKardex.put("key_almacen", almacen.getString("key"));
            inventarioKardex.put("cantidad", 1);
            inventarioKardex.put("tipo", "ingreso_produccion");




            obj.put("estado", "exito");
        }catch(Exception e){
            e.printStackTrace();
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
        }
    }

    public static JSONObject buscarIgrediente(String key_modelo, JSONArray productos) {
        JSONObject producto;
        for (int i = 0; i < productos.length(); i++) {
            producto = productos.getJSONObject(i);
            if (producto.getString("key_modelo").equals(key_modelo)) {
                System.out.println("Ingrediente encontrado.---");
                JSONObject producto_ = new JSONObject(producto.toString());
                productos.remove(i);
                return producto_;
            }
        }
        return null;
    }

    public static JSONArray getProductosInventario(SPGConectInstance conectInstance, String key_modelo,
            String key_sucursal) {

        try {
            String consulta = "select array_to_json(array_agg(tabla.*))::json as json \n" +
                    "from ( \n" +
                    "select producto.key, \n" +
                    "  sum(producto.cantidad) as cantidad, \n" +
                    "  sum(producto.precio_compra) as precio_compra, \n" +
                    "  sum(producto.precio_compra) / sum(producto.cantidad) as precio_compra_unitario \n" +
                    "from producto, \n" +
                    "  almacen \n" +
                    "where producto.key_modelo = '" + key_modelo + "' \n" +
                    "  and producto.key_almacen = almacen.key \n" +
                    "  and almacen.estado > 0 \n" +
                    "  and producto.estado > 0 \n" +
                    "  and almacen.key_sucursal = '" + key_sucursal + "' \n" +
                    "  and producto.cantidad > 0  \n" +
                    "group by producto.key \n" +
                    "    order by producto.fecha_on asc \n" +
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
            String consulta = "select array_to_json(array_Agg(tabla.*)) as json from (\n" +
                    "    select to_json(tipo_producto.*) as tipo_producto, \n" +
                    "    to_json(modelo.*) as modelo, \n" +
                    "    sum(producto.cantidad) as cantidad \n" +
                    "    from modelo, \n" +
                    "    almacen, \n" +
                    "    producto, \n" +
                    "    tipo_producto \n" +
                    "    where tipo_producto.key = modelo.key_tipo_producto \n" +
                    "    and almacen.key_sucursal = '" + obj.getString("key_sucursal") + "' \n" +
                    "    and almacen.is_venta = true \n" +
                    "    and producto.key_almacen = almacen.key \n" +
                    "    and almacen.estado > 0 \n" +
                    "    and producto.estado > 0 \n" +
                    "    and producto.cantidad > 0 \n" +
                    "    and modelo.key = producto.key_modelo \n" +
                    "    group by modelo.key, \n" +
                    "    tipo_producto.key \n" +
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

            JSONArray productosInventario = getProductosInventario(conectInstance,
                    ingredienteModelo.getString("key_modelo_ingrediente"), obj.getString("key_sucursal"));
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
                precioCompraUnitarioProductoInventario = new BigDecimal(
                        productoInventario.get("precio_compra_unitario") + "");

                // Actualizo el Producto del Invetario
                if (cantidad <= cantidadProductoInventario) {
                    cantidadIngredienteUsado = cantidad;
                    cantidad = 0;
                } else {
                    cantidadIngredienteUsado = cantidadProductoInventario;
                    cantidad -= cantidadIngredienteUsado;
                }
                // Calculo Datos para el Ingrediente Nuevo
                precioCompraIngredienteNew = precioCompraUnitarioProductoInventario
                        .multiply(BigDecimal.valueOf(cantidadIngredienteUsado));

                // Calculo Datos para Actualizar el Producto del Inventario
                cantidadProductoInventario -= cantidadIngredienteUsado;
                precioCompraProductoInventario = precioCompraProductoInventario.subtract(precioCompraIngredienteNew,
                        MathContext.DECIMAL128);
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
            String consulta = "" +
                    "select array_to_json(array_agg(tabla.*))::json as json  \n" +
                    "from(  \n" +
                    "select almacen.key as key_almacen, \n" +
                    "producto.key_modelo, \n" +
                    "modelo.key_tipo_producto, \n" +
                    "modelo.descripcion as modelo, \n" +
                    "almacen.descripcion,   \n" +
                    "sum(producto.cantidad) as cantidad   \n" +
                    "from producto,   \n" +
                    "modelo,   \n" +
                    "almacen   \n" +
                    "where producto.estado > 0   \n" +
                    "and almacen.estado > 0   \n" +
                    "and modelo.key = producto.key_modelo   \n" +
                    "and almacen.key = producto.key_almacen   \n" +
                    "and almacen.key_sucursal = '" + obj.getString("key_sucursal") + "'   \n" +
                    "and producto.cantidad > 0   \n" +
                    "and producto.key_modelo in (   \n" +
                    "select modelo.key   \n" +
                    "from modelo   \n" +
                    "where modelo.key in (   \n" +
                    "SELECT modelo_ingrediente.key_modelo_ingrediente   \n" +
                    "FROM  modelo_ingrediente   \n" +
                    "where modelo_ingrediente.estado > 0   \n" +
                    "and modelo_ingrediente.key_modelo = '" + obj.getString("key_modelo") + "'   \n" +
                    ")	   \n" +
                    ")   \n" +
                    "group by almacen.key,   \n" +
                    "almacen.descripcion,  \n" +
                    "producto.key_modelo, modelo.descripcion, modelo.key_tipo_producto \n" +
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
            if (obj.has("key_empresa")) {
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

    public static void getAllStock(JSONObject obj, SSSessionAbstract session) {
        try {
            if (!obj.has("key_empresa")) {
                obj.put("estado", "error");
                return;
            }
            String consulta = "select get_modelos_con_stock('" + obj.getString("key_empresa") + "') as json";

            if (obj.has("key_almacen")) {
                String key_almacen = obj.getString("key_almacen");
                consulta = "select get_modelos_con_stock('" + obj.getString("key_empresa") + "', '"
                        + key_almacen + "') as json";
            }

            if (obj.has("key_sucursal")) {
                String key_sucursal = obj.getString("key_sucursal");
                consulta = "select get_modelos_con_stock_sucursal('" + obj.getString("key_empresa") + "', '"
                        + key_sucursal + "') as json";
            }

            JSONArray data = SPGConect.ejecutarConsultaArray(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void addStock(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getAllProductos(JSONObject obj, SSSessionAbstract session) {
        try {
            String key_modelo = obj.getString("key_modelo");
            JSONArray data = SPGConect
                    .ejecutarConsultaArray("select modelo_getallproductos('" + key_modelo + "') as json");
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void compraCaja(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {
            conectInstance = new ConectInstance();
            conectInstance.Transacction();
            JSONObject data = obj.getJSONObject("data");
            JSONObject compra_venta = obj.getJSONObject("data").getJSONObject("compra_venta");
            JSONArray detalle = obj.getJSONObject("data").getJSONArray("detalle");

            // JSONObject detalle =
            String key_almacen = data.getString("key_almacen");
            JSONObject almacen = conectInstance.ejecutarConsultaObject("""
                        select to_json(almacen.*) as json
                        from almacen where  almacen.estado > 0 and almacen.key = '%s'
                        limit 1
                    """.formatted(key_almacen));

            if (almacen == null || almacen.isEmpty()) {
                throw new Exception("No se encontro el almacen");
            }

            double porc_iva = 0;
            if (data.optBoolean("facturar", false)) {
                porc_iva = Contabilidad.getEnviroment(compra_venta.getString("key_empresa"), "IVA")
                        .optDouble("observacion", 0);
            }

            JSONArray productos = new JSONArray();
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
                double cantidad = compra_detalle.getDouble("cantidad");
                double precio_unitario = compra_detalle.getDouble("precio_unitario");
                if (data.optBoolean("facturar", false)) {
                    // Hasta q ruddy vea venta
                    precio_unitario = (precio_unitario / (1 + (porc_iva / 100)));
                }

                precio_unitario = (Math.round(precio_unitario * 100)) / 100.00;
                JSONObject tipo_producto = TipoProducto.getByKey(modelo.getString("key_tipo_producto"));
                compra_detalle.put("tipo_producto", tipo_producto);
                JSONObject producto = new JSONObject();
                producto.put("key", SUtil.uuid());
                producto.put("estado", 1);
                producto.put("fecha_on", SUtil.now());
                producto.put("key_usuario", data.getString("key_usuario"));
                producto.put("key_modelo", compra_detalle.getString("key_modelo"));
                producto.put("precio_compra", precio_unitario);
                producto.put("nombre",
                        modelo.getString("descripcion") + " - " + compra_detalle.optString("detalle", ""));
                producto.put("key_empresa", compra_venta.getString("key_empresa"));
                producto.put("key_compra_venta_detalle", compra_detalle.getString("key"));

                conectInstance.insertObject("producto", producto);
                producto.put("tipo_producto", tipo_producto);

                JSONObject cardex = InventarioCardex.CrearMovimiento(
                        producto.getString("key"),
                        TipoMovimientoCardex.ingreso_compra,
                        cantidad,
                        almacen.getString("key"),
                        data.getString("key_usuario"));
                conectInstance.insertObject("inventario_cardex", cardex);

                producto.put("cardex", cardex);

                productos.put(producto);
            }
            data.put("productos", productos);

            JSONObject request = new JSONObject();
            request.put("component", "asiento_contable");
            request.put("type", "compra_caja");
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
                        double cantidad_retirada = 0;
                        for (int j = 0; j < arrInsertado.length(); j++) {
                            cantidad_retirada += arrInsertado.getJSONObject(j).getDouble("cantidad");
                        }
                        if ((cantidad_retirada * -1) < cantidad) {
                            throw new Exception(
                                    "No se pudo retirar el producto del inventario, solo quedan " + cantidad_retirada);
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
                                venta.getString("key_usuario"));
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

                String key_cuenta_contable = "";
                if (tipo_producto.has("key_cuenta_contable") && !tipo_producto.isNull("key_cuenta_contable")) {

                    JSONObject cardex = InventarioCardex.CrearMovimiento(
                            producto.getString("key"),
                            TipoMovimientoCardex.ingreso_compra,
                            cantidad,
                            almacen.getString("key"),
                            compra.getString("key_usuario"));

                    conectInstance.insertObject("inventario_cardex", cardex);
                    key_cuenta_contable = tipo_producto.getString("key_cuenta_contable");
                } else {
                    key_cuenta_contable = tipo_producto.getString("key_cuenta_contable_costo");

                    JSONObject cardex = InventarioCardex.CrearMovimiento(
                            producto.getString("key"),
                            TipoMovimientoCardex.ingreso_compra,
                            cantidad,
                            almacen.getString("key"),
                            compra.getString("key_usuario"));

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

                if (tipo_producto.has("key_cuenta_contable") && !tipo_producto.isNull("key_cuenta_contable")) {
                    String key_cuenta_contable_gasto = tipo_producto.optString("key_cuenta_contable_costo");
                    String key_cuenta_contable = tipo_producto.getString("key_cuenta_contable");
                    JSONObject dataExtra = new JSONObject();
                    dataExtra.put("key_compra_venta", venta.getString("key"));
                    dataExtra.put("key_compra_venta_detalle", compra_detalle.getString("key"));
                    dataExtra.put("precio_unitario_venta", precio_unitario);

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
                            venta.getString("key_usuario"));

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
