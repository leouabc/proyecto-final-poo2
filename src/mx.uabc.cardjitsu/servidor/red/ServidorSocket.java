package servidor.red;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import servidor.datos.ManejadorDB4O;


public class ServidorSocket extends Application {
    private static final int PUERTO = 35557;

    @FXML
    private TextArea areaServidor;

    private volatile boolean servidorActivo;
    private volatile ServerSocket socketServidor;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gui-server.fxml"));
        loader.setController(this);
        Scene scene = new Scene(loader.load());

        stage.setTitle("Servidor Dragon Ball Card-Jitsu");
        stage.setScene(scene);
        stage.show();

        servidorActivo = true;
        Thread hiloServidor = new Thread(this::iniciarServidor, "servidor-socket");
        hiloServidor.setDaemon(true);
        hiloServidor.start();

        stage.setOnCloseRequest(event -> detenerServidor());
    }

    private void iniciarServidor() {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            socketServidor = servidor;
            registrarLog("SERVIDOR INICIADO EN EL PUERTO " + PUERTO);

            while (servidorActivo) {
                Socket socket = servidor.accept();
                String ip = socket.getInetAddress().getHostAddress();
                registrarLog("Cliente conectado: " + ip);

                Thread hiloCliente = new Thread(
                        new HiloCliente(socket, areaServidor),
                        "cliente-" + ip
                );
                hiloCliente.setDaemon(true);
                hiloCliente.start();
            }
        } catch (IOException e) {
            if (servidorActivo) registrarLog("Error del servidor: " + e.getMessage());
        } finally {
            socketServidor = null;
        }
    }

    private void detenerServidor() {
        servidorActivo = false;
        ServerSocket servidor = socketServidor;
        if (servidor != null && !servidor.isClosed()) {
            try { servidor.close(); } catch (IOException ignored) { }
        }
        try { ManejadorDB4O.cerrarConexion(); }
        catch (RuntimeException ex) { registrarLog("No se pudo cerrar DB4O: " + ex.getMessage()); }
    }

    @Override
    public void stop() {
        detenerServidor();
    }

    private void registrarLog(String texto) {
        System.out.println(texto);
        if (areaServidor != null) {
            Platform.runLater(() -> areaServidor.appendText(texto + "\n"));
        }
    }

    @FXML
    public void initialize() {
    }

    public static void main(String[] args) {
        Application.launch(ServidorSocket.class, args);
    }
}
