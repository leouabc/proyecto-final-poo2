package cliente.red;

import javafx.application.Application;


public final class MainCliente {

    private MainCliente() {
        // Clase de entrada: no necesita instancias.
    }

    public static void main(String[] args) {
        Application.launch(ManejadorEventosCliente.class, args);
    }
}
