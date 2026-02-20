import Controllers.ModeloController;
import Servisofts.Servisofts;
import Servisofts.http.Rest;

public class App {
    public static void main(String[] args) {
        try {
            Servisofts.DEBUG = false;
            Servisofts.ManejadorCliente = ManejadorCliente::onMessage;
            Servisofts.Manejador = Manejador::onMessage;
            System.out.println("🔥 REST REGISTRADO 🔥");
            Rest.addController(ModeloController.class);
            Servisofts.initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}