package servidor.red;

import javafx.application.Application;

/**
 * Punto de entrada explícito del servidor JavaFX.
 * Ejecuta esta clase para abrir la consola del servidor y escuchar el puerto 35557.
 */
public final class MainServidor {

    private MainServidor() {
        // Clase de entrada: no necesita instancias.
    }

    public static void main(String[] args) {
        Application.launch(ServidorSocket.class, args);
    }
}
