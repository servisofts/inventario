import Controllers.AlvaroController;
import Servisofts.Servisofts;
import Servisofts.http.Rest;

public class App {
    public static void main(String[] args) {
        try {
            Servisofts.DEBUG = false;
            Servisofts.ManejadorCliente = ManejadorCliente::onMessage;
            Servisofts.Manejador = Manejador::onMessage;
            System.out.println("🔥 REST REGISTRADO 🔥");
            Rest.addController(AlvaroController.class);
            Servisofts.initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}