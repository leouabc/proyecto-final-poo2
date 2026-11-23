package servidor.red;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class ServidorSocket extends Application{
    @FXML
    private TextArea areaServidor;
    

    private static final int PUERTO = 35557;
    private static List<ObjectOutputStream> clientes = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }
    private static class ManejadorCliente implements Runnable {
        private Socket socket;
        private ObjectOutputStream salida;

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                salida = new ObjectOutputStream(socket.getOutputStream());

                clientes.add(salida);

            } catch (Exception e) {
                System.out.println("Un cliente se desconecto");
                clientes.remove(salida);
            }
        }
    }
    
    @Override 
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gui-server.fxml"));
        loader.setController(this);
        Scene scene = new Scene(loader.load());

        stage.setTitle("Servidor");
        stage.setScene(scene);
        stage.show();

        Thread hiloServidor = new Thread(() -> iniciarServidor());
        hiloServidor.setDaemon(true);
        hiloServidor.start();
    }
    
    private void iniciarServidor() {
        try (ServerSocket server = new ServerSocket(PUERTO)) {
            System.out.println("El servidor esta iniciado");
            Platform.runLater(() -> areaServidor.appendText("SERVIDOR INICIADO \n ----------- \n"));
            
            while (true) {
                Socket cliente = server.accept();
                System.out.println("Cliente conectado");
                Platform.runLater(() -> areaServidor.appendText("Cliente conectado \n"));
                new Thread(new ManejadorCliente(cliente)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML 
    public void initialize() {
        
    }
}
