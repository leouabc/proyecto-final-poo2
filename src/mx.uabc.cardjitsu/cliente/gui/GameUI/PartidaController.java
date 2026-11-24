package cliente.gui.GameUI;

import cliente.red.ClienteSocket;
import cliente.red.ManejadorEventosCliente;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import javafx.stage.Stage;
import modelo.Carta;
import modelo.JavaBeans;
import protocolo.MensajeProtocolo;
import protocolo.TipoAccion;

public class PartidaController {

    @FXML
    private Label lblRonda;

    @FXML
    private Label lblRival;

    @FXML
    private Label lblPuntos;

    @FXML
    private TextArea txtAreaChat;

    @FXML
    private TextField txtMensaje;

    @FXML
    private Button btnEnviar;

    @FXML
    private VBox VboxTusCartasGanadas;

    @FXML
    private Label lblResultado;

    @FXML
    private VBox CartaTuyaLanzada;


    @FXML
    private Button btnRegresar;

    @FXML
    private Button btnRevancha;

    @FXML
    private VBox carta1;

    @FXML
    private VBox carta2;

    @FXML
    private VBox carta3;

    @FXML
    private VBox carta4;

    @FXML
    private VBox carta5;

    @FXML
    private Button btnConfirmar;

    private ClienteSocket clienteSocket;
    private JavaBeans jugadorActual;
    private List<Carta> manoActual = new ArrayList<>();
    private final List<VBox> slotsMano = new ArrayList<>();
    private final Map<String, Image> cacheMarcosRaza = new HashMap<>();
    private int indiceSeleccionado = -1;
    private boolean jugadaConfirmada;
    private boolean revanchaPorAbandonoDisponible;

    @FXML
    private void initialize() {
        ManejadorEventosCliente.registrarPartida(this);
        slotsMano.clear();
        if (carta1 != null) slotsMano.add(carta1);
        if (carta2 != null) slotsMano.add(carta2);
        if (carta3 != null) slotsMano.add(carta3);
        if (carta4 != null) slotsMano.add(carta4);
        if (carta5 != null) slotsMano.add(carta5);

        for (int i = 0; i < slotsMano.size(); i++) {
            final int indice = i;
            VBox slot = slotsMano.get(i);
            slot.setOnMouseClicked(event -> seleccionarCarta(indice));
        }

        revanchaPorAbandonoDisponible = false;
        if (btnRevancha != null) {
            btnRevancha.setDisable(true);
            btnRevancha.setText("REVANCHA: ESPERANDO ABANDONO");
        }
        if (btnConfirmar != null) {
            btnConfirmar.setDisable(true);
        }
    }

    public void setClienteSocket(ClienteSocket clienteSocket) {
        this.clienteSocket = clienteSocket;
    }

    public void setJugadorActual(JavaBeans jugadorActual) {
        this.jugadorActual = jugadorActual;
    }

    private ClienteSocket asegurarConexion() {
        if (clienteSocket == null) {
            clienteSocket = new ClienteSocket();
            clienteSocket.initialize();
        }
        return clienteSocket;
    }

    @FXML
    private void enviarMensaje(ActionEvent event) {
        if (txtMensaje == null) {
            return;
        }
        String texto = txtMensaje.getText() != null ? txtMensaje.getText().trim() : "";
        if (texto.isEmpty()) return;
        if (texto.length() > 300) texto = texto.substring(0, 300);

        String remitente = (jugadorActual != null && jugadorActual.getNombre() != null && !jugadorActual.getNombre().isBlank())
                ? jugadorActual.getNombre()
                : "Tú";

        recibirMensajeChat(remitente, texto);
        txtMensaje.clear();

        ClienteSocket socket = asegurarConexion();
        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.CHAT);
        mensaje.setNombreUsuario(remitente);
        mensaje.setMensaje(texto);
        socket.enviar(mensaje);
    }

    public void recibirMensajeChat(String remitente, String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        String autor = (remitente != null && !remitente.isBlank()) ? remitente : "Jugador";
        Platform.runLater(() -> {
            if (txtAreaChat != null) {
                txtAreaChat.appendText(autor + ": " + texto + "\n");
            }
        });
    }

    public void actualizarCabecera(int ronda, String nombreRival, int puntos) {
        Platform.runLater(() -> {
            if (lblRonda != null) {
                lblRonda.setText("RONDA: " + ronda);
            }
            if (lblRival != null) {
                lblRival.setText("RIVAL: " + (nombreRival != null ? nombreRival : "Esperando rival"));
            }
            if (lblPuntos != null) {
                lblPuntos.setText("PUNTOS: " + puntos);
            }
        });
    }

    public void renderizarMano(List<Carta> cartas) {
        Platform.runLater(() -> {
            manoActual = (cartas != null) ? new ArrayList<>(cartas) : new ArrayList<>();
            indiceSeleccionado = -1;
            jugadaConfirmada = false;

            if (btnConfirmar != null) {
                btnConfirmar.setDisable(true);
            }

            for (int i = 0; i < slotsMano.size(); i++) {
                VBox slot = slotsMano.get(i);
                slot.getChildren().clear();
                slot.setDisable(false);
                if (i < manoActual.size()) {
                    VBox tarjeta = crearTarjetaVisual(manoActual.get(i));
                    slot.setMinSize(tarjeta.getPrefWidth(), tarjeta.getPrefHeight());
                    slot.setPrefSize(tarjeta.getPrefWidth(), tarjeta.getPrefHeight());
                    slot.setMaxSize(tarjeta.getPrefWidth(), tarjeta.getPrefHeight());
                    slot.getChildren().add(tarjeta);
                }
            }
        });
    }

    private void seleccionarCarta(int indice) {
        if (jugadaConfirmada || indice < 0 || indice >= manoActual.size()) {
            return;
        }
        indiceSeleccionado = indice;
        if (btnConfirmar != null) {
            btnConfirmar.setDisable(false);
        }
        if (lblResultado != null) {
            lblResultado.setText("Resultado: Seleccionaste " + formatearCarta(manoActual.get(indice)));
        }
    }

    @FXML
    private void onClickConfirmar(ActionEvent event) {
        if (jugadaConfirmada || indiceSeleccionado < 0 || indiceSeleccionado >= manoActual.size()) {
            return;
        }
        jugadaConfirmada = true;
        if (btnConfirmar != null) {
            btnConfirmar.setDisable(true);
        }
        bloquearMano(true);

        Carta cartaElegida = manoActual.get(indiceSeleccionado);
        if (CartaTuyaLanzada != null) {
            CartaTuyaLanzada.getChildren().setAll(crearTarjetaVisual(cartaElegida));
        }
        if (lblResultado != null) {
            lblResultado.setText("Resultado: Esperando jugada del rival...");
        }

        ClienteSocket socket = asegurarConexion();
        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.JUGAR_CARTA);
        mensaje.setIndiceCarta(indiceSeleccionado);
        socket.enviar(mensaje);
    }

    public void mostrarResultadoRonda(Carta cartaPropia, Carta cartaRival, String resultadoTexto, int nuevaRonda, int puntosActuales, List<Carta> nuevaMano, boolean ganoRonda) {
        Platform.runLater(() -> {
            if (CartaTuyaLanzada != null) {
                CartaTuyaLanzada.getChildren().clear();
                if (cartaPropia != null) {
                    CartaTuyaLanzada.getChildren().add(crearTarjetaVisual(cartaPropia));
                }
            }
            boolean empate = resultadoTexto != null && resultadoTexto.toLowerCase().contains("empate");
            if (!empate && ganoRonda && cartaRival != null && VboxTusCartasGanadas != null) {
                VBox cartaGanada = crearTarjetaVisual(cartaRival);
                // Mantener el ancho real de la carta; expandir este VBox provoca
                // que el clip interno recorte la mitad derecha de la imagen.
                cartaGanada.setMaxWidth(cartaGanada.getPrefWidth());
                cartaGanada.setMinWidth(cartaGanada.getPrefWidth());
                VboxTusCartasGanadas.getChildren().add(cartaGanada);
            }
            if (lblResultado != null) {
                lblResultado.setText("Resultado: " + (resultadoTexto != null ? resultadoTexto : ""));
            }
            actualizarCabecera(nuevaRonda, lblRival != null ? lblRival.getText().replace("RIVAL: ", "") : "", puntosActuales);
            if (nuevaMano != null) {
                renderizarMano(nuevaMano);
            } else {
                jugadaConfirmada = false;
                bloquearMano(false);
            }
        });
    }

    public void habilitarFinPartida(String resumenFinal) {
        Platform.runLater(() -> {
            bloquearMano(true);
            if (btnConfirmar != null) {
                btnConfirmar.setDisable(true);
            }
            if (lblResultado != null && resumenFinal != null) {
                lblResultado.setText("Resultado: " + resumenFinal);
            }
            if (btnRevancha != null) {
                btnRevancha.setDisable(true);
                btnRevancha.setText("REVANCHA: ESPERANDO ABANDONO");
            }
        });
    }

    public void habilitarRevanchaPorAbandono(String mensaje) {
        Platform.runLater(() -> {
            revanchaPorAbandonoDisponible = true;
            if (btnRevancha != null) {
                btnRevancha.setDisable(false);
                btnRevancha.setText("BUSCAR NUEVO RIVAL");
            }
            if (lblResultado != null && mensaje != null) lblResultado.setText("Resultado: " + mensaje);
        });
    }

    public void mostrarMensajeEstado(String mensaje) {
        Platform.runLater(() -> {
            if (lblResultado != null && mensaje != null) lblResultado.setText("Resultado: " + mensaje);
        });
    }

    @FXML
    private void onClickRevancha(ActionEvent event) {
        if (!revanchaPorAbandonoDisponible) return;
        revanchaPorAbandonoDisponible = false;
        if (btnRevancha != null) btnRevancha.setDisable(true);

        ClienteSocket socket = asegurarConexion();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/cliente/gui/lobby.fxml"));
            Parent root = loader.load();
            LobbyController lobbyController = loader.getController();
            lobbyController.setClienteSocket(socket);
            if (jugadorActual != null) lobbyController.setJugadorActual(jugadorActual);
            Stage stage = (Stage) btnRevancha.getScene().getWindow();
            stage.setScene(new Scene(root));
            socket.enviar(new MensajeProtocolo(TipoAccion.PEDIR_REVANCHA));
        } catch (IOException ex) {
            revanchaPorAbandonoDisponible = true;
            if (btnRevancha != null) btnRevancha.setDisable(false);
            mostrarMensajeEstado("No se pudo volver al lobby: " + ex.getMessage());
        }
    }

    @FXML
    private void onClickRegresar(ActionEvent event) {
        if (btnRegresar != null) {
            btnRegresar.setDisable(true);
        }

        ClienteSocket socket = asegurarConexion();
        MensajeProtocolo mensaje = new MensajeProtocolo();
        mensaje.setTipo(TipoAccion.SALIR_PARTIDA);
        socket.enviar(mensaje);

        volverAlLobby();
    }

    public void volverAlLobby() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/cliente/gui/lobby.fxml"));
                Parent root = loader.load();
                LobbyController lobbyController = loader.getController();
                lobbyController.setClienteSocket(clienteSocket);
                if (jugadorActual != null) {
                    lobbyController.setJugadorActual(jugadorActual);
                }
                Stage stage = (Stage) btnRegresar.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException e) {
                if (btnRegresar != null) {
                    btnRegresar.setDisable(false);
                }
            }
        });
    }

    private VBox crearTarjetaVisual(Carta personaje) {
        String nombre = personaje != null && personaje.getNombre() != null
                ? personaje.getNombre() : "Personaje desconocido";
        String raza = personaje != null && personaje.getRaza() != null
                ? personaje.getRaza() : "Raza desconocida";
        String poder = personaje != null && personaje.getPoderApi() != null
                ? personaje.getPoderApi() : "No disponible";

        String archivoMarco = archivoMarcoPorRaza(raza);
        Image imagenMarco = cargarMarcoRaza(archivoMarco);

        double altoMarco = 180.0;
        double proporcion = imagenMarco != null && imagenMarco.getHeight() > 0
                ? imagenMarco.getWidth() / imagenMarco.getHeight() : 0.58;
        double anchoMarco = altoMarco * proporcion;

        StackPane composicion = new StackPane();
        composicion.setPrefSize(anchoMarco, altoMarco);
        composicion.setMinSize(anchoMarco, altoMarco);
        composicion.setMaxSize(anchoMarco, altoMarco);
        composicion.setPickOnBounds(false);
        composicion.setClip(new Rectangle(anchoMarco, altoMarco));


        Label estadoImagen = new Label("CARGANDO\nPERSONAJE...");
        estadoImagen.setAlignment(javafx.geometry.Pos.CENTER);
        estadoImagen.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        estadoImagen.setWrapText(true);
        estadoImagen.setMaxWidth(anchoMarco * 0.68);
        estadoImagen.setTranslateY(altoMarco * 0.035);
        estadoImagen.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 8px; -fx-font-weight: bold;");
        estadoImagen.setMouseTransparent(true);

        ImageView retratoApi = new ImageView();
        retratoApi.setFitWidth(anchoMarco * 0.82);
        retratoApi.setFitHeight(altoMarco * 0.76);
        retratoApi.setPreserveRatio(true);
        retratoApi.setSmooth(true);
        retratoApi.setVisible(false);
        retratoApi.setTranslateY(altoMarco * 0.035);
        retratoApi.setMouseTransparent(true);
        // No se coloca un rectángulo oscuro detrás del retrato: el interior
        // transparente del marco permite ver al personaje sin un bloque negro.
        composicion.getChildren().addAll(estadoImagen, retratoApi);

        String urlImagen = personaje == null ? null : personaje.getImagenUrl();
        if (urlImagen != null) {
            urlImagen = urlImagen.trim();
        }
        if (urlImagen != null && (urlImagen.startsWith("https://") || urlImagen.startsWith("http://"))) {
            try {
                Image imagenPersonaje = new Image(urlImagen,
                        anchoMarco * 0.82, altoMarco * 0.76, true, true, true);
                retratoApi.setImage(imagenPersonaje);

                imagenPersonaje.progressProperty().addListener((obs, anterior, progreso) -> {
                    if (progreso.doubleValue() >= 1.0 && !imagenPersonaje.isError()) {
                        Platform.runLater(() -> {
                            retratoApi.setVisible(true);
                            estadoImagen.setVisible(false);
                        });
                    }
                });
                imagenPersonaje.errorProperty().addListener((obs, anterior, error) -> {
                    if (Boolean.TRUE.equals(error)) {
                        Platform.runLater(() -> estadoImagen.setText("IMAGEN NO DISPONIBLE"));
                    }
                });
                if (imagenPersonaje.getProgress() >= 1.0 && !imagenPersonaje.isError()) {
                    retratoApi.setVisible(true);
                    estadoImagen.setVisible(false);
                } else if (imagenPersonaje.isError()) {
                    estadoImagen.setText("IMAGEN NO DISPONIBLE");
                }
            } catch (IllegalArgumentException ex) {
                estadoImagen.setText("URL DE IMAGEN INVÁLIDA");
            }
        } else {
            estadoImagen.setText("SIN IMAGEN\nEN LA API");
        }

        Label titulo = new Label(nombre);
        titulo.setWrapText(true);
        titulo.setAlignment(javafx.geometry.Pos.CENTER);
        titulo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        titulo.setMaxWidth(anchoMarco * 0.72);
        titulo.setStyle("-fx-text-fill: white; -fx-font-size: 8px; -fx-font-weight: bold; -fx-text-alignment: center;");

        Label datoRaza = new Label("RAZA: " + raza.toUpperCase(Locale.ROOT));
        datoRaza.setAlignment(javafx.geometry.Pos.CENTER);
        datoRaza.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        datoRaza.setMaxWidth(anchoMarco * 0.72);
        datoRaza.setStyle("-fx-text-fill: #93C5FD; -fx-font-size: 6.5px; -fx-font-weight: bold;");

        Label datoPoder = new Label("KI: " + poder);
        datoPoder.setAlignment(javafx.geometry.Pos.CENTER);
        datoPoder.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        datoPoder.setMaxWidth(anchoMarco * 0.72);
        datoPoder.setStyle("-fx-text-fill: #F4D58D; -fx-font-size: 7px; -fx-font-weight: bold;");

        VBox datosCarta = new VBox(1, titulo, datoRaza, datoPoder);
        datosCarta.setAlignment(javafx.geometry.Pos.CENTER);
        datosCarta.setPrefWidth(anchoMarco * 0.76);
        datosCarta.setMaxWidth(anchoMarco * 0.76);
        datosCarta.setTranslateY(altoMarco * 0.16);
        datosCarta.setPadding(new javafx.geometry.Insets(3, 3, 3, 3));
        // Texto sin placa oscura: conserva visible el personaje detrás de los datos.
        datosCarta.setStyle("-fx-background-color: transparent; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.95), 3, 0.8, 0, 1);");
        datosCarta.setMouseTransparent(true);
        composicion.getChildren().add(datosCarta);

        if (imagenMarco != null) {
            ImageView vistaMarco = new ImageView(imagenMarco);
            vistaMarco.setFitWidth(anchoMarco);
            vistaMarco.setFitHeight(altoMarco);
            vistaMarco.setPreserveRatio(false);
            vistaMarco.setSmooth(true);
            vistaMarco.setMouseTransparent(true);
            // El marco deja pasar más imagen para que el personaje destaque.
            vistaMarco.setOpacity(0.42);
            // Sin sombra exterior para evitar que aparezcan bloques oscuros
            // fuera del contorno de la carta.
            vistaMarco.setEffect(null);
            composicion.getChildren().add(vistaMarco);
        } else {
            System.err.println("No se encontró el PNG del marco: " + archivoMarco);
        }

        VBox tarjeta = new VBox(composicion);
        tarjeta.setAlignment(javafx.geometry.Pos.CENTER);
        // La tarjeta ocupa exactamente el tamaño del marco: evita el borde vacío
        // que antes aparecía al colocarla en la mano o en el área de cartas ganadas.
        tarjeta.setPrefSize(anchoMarco, altoMarco);
        tarjeta.setMinSize(anchoMarco, altoMarco);
        tarjeta.setMaxSize(anchoMarco, altoMarco);
        tarjeta.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        tarjeta.setPickOnBounds(false);
        tarjeta.setClip(new Rectangle(anchoMarco, altoMarco));
        return tarjeta;
    }

    private String archivoMarcoPorRaza(String raza) {
        String r = raza == null ? "" : raza.toLowerCase(Locale.ROOT);
        if (r.contains("saiyan")) return "Saiyan.png";
        if (r.contains("namek")) return "Namekiano.png";
        if (r.contains("majin")) return "Majin.png";
        if (r.contains("frieza") || r.contains("freezer") || r.contains("frost demon") || r.contains("arcosian")) return "RazaFreezer.png";
        if (r.contains("android")) return "Androides.png";
        if (r.contains("angel")) return "Angel.png";
        if (r.contains("god") || r.contains("destruction")) return "DiosDestruccion.png";
        if (r.contains("human") || r.contains("earthling")) return "Humanos-Corpo.png";
        return "Saiyan.png";
    }

    private Image cargarMarcoRaza(String archivo) {
        Image enCache = cacheMarcosRaza.get(archivo);
        if (enCache != null) return enCache;

        java.net.URL recurso = getClass().getResource("/cliente/gui/" + archivo);
        if (recurso == null) {
            System.err.println("No se encontró el recurso /cliente/gui/" + archivo);
            return null;
        }
        Image imagen = new Image(recurso.toExternalForm(), false);
        if (imagen.isError()) {
            System.err.println("No se pudo leer el marco " + archivo + ": " + imagen.getException());
            return null;
        }
        cacheMarcosRaza.put(archivo, imagen);
        return imagen;
    }

    private String formatearCarta(Carta carta) {
        if (carta == null) return "Personaje";
        String nombre = carta.getNombre() != null ? carta.getNombre() : "Sin nombre";
        String raza = carta.getRaza() != null ? carta.getRaza() : "Raza desconocida";
        String poder = carta.getPoderApi() != null ? carta.getPoderApi() : "No disponible";
        return nombre + " [" + raza + "] - Ki " + poder;
    }

    private void bloquearMano(boolean bloqueada) {
        for (VBox slot : slotsMano) {
            slot.setDisable(bloqueada);
        }
    }
}