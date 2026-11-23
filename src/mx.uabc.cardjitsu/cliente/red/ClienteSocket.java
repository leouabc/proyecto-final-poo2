package cliente.red;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import modelo.JavaBeans;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;

public class ClienteSocket {
    @FXML
    private TextField txtNombre;
    @FXML
    private PasswordField txtContra;

    private JavaBeans jugador = new JavaBeans();

    private static String usuario;
    private static Socket cliente;
    private static ObjectInputStream entrada;
    private static ObjectOutputStream salida;
    private static boolean escuchando = false;

    private static final Map<String, String> usuariosRegistrado = new HashMap<>();
    @FXML 
    public void initialize() {
        
    }
    private void conectarServidor() {
        try {
            conectarServidor();
        } catch (Exception e) {
            System.out.println("No se pudo conectar con el servidor.");
            e.printStackTrace();
        }
    }
    private void iniciarHiloEscucha() {
        if (escuchando) return;
        escuchando = true;

        Thread hiloEscucha = new Thread(() -> {
            try {
                while (cliente != null && !cliente.isClosed()) {
                    MensajeProtocolo respuesta = (MensajeProtocolo) entrada.readObject();
                    procesarMensajeServidor(respuesta);
                }
            } catch (Exception e) {
                escuchando = false;
                System.out.println("Conexión perdida con el servidor.");
            }
        });
        hiloEscucha.setDaemon(true);
        hiloEscucha.start();
    }
    private void procesarMensajeServidor(MensajeProtocolo mensaje) {
        Platform.runLater(() -> {
            switch (mensaje.getTipo()) {
                case REGISTRO:
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Registro", "Solicitud de registro exitosa!");
                    break;
                case LOGIN:
                    cambiarPantalla(, "/cliente/gui/lobby.fxml");
                    break;
                default:
                    break;
            }
        });
    }
    @FXML 
    public void onClickRegistrarse(ActionEvent event) {
        usuario = txtNombre.getText().trim();
        String password = txtContra.getText().trim();
        
        if (usuario.isEmpty() || password.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campso Vacios", "Algunos campos estan vacios");
            return;
        }
        try {
            jugador = new JavaBeans();
            jugador.setNombre(usuario);
            jugador.setContra(password);

            // Solo envías la petición al servidor (sin cambiar de pantalla aquí)
            MensajeProtocolo msg = new MensajeProtocolo(TipoAccion.REGISTRO, jugador);
            salida.writeObject(msg);
            salida.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML 
    public void onClickEntrarTorneo(ActionEvent e) {
    usuario = txtNombre.getText().trim();
    String password = txtContra.getText().trim();

    if (usuario.isEmpty() || password.isEmpty()) {
        mostrarAlerta(Alert.AlertType.WARNING, "Campos Vacíos", "Algunos campos están vacíos");
        return;
    }
    try {
        jugador = new JavaBeans();
        jugador.setNombre(usuario);
        jugador.setContra(password);

        MensajeProtocolo msg = new MensajeProtocolo(TipoAccion.LOGIN, jugador);
        salida.writeObject(msg);
        salida.flush();
    } catch (IOException ex) {
        ex.printStackTrace();
    }
}
    private void cambiarPantalla(String rutaFxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(rutaFxml));
            Stage stage = (Stage) txtNombre.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo cargar la pantalla del torneo.");
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}

