package cliente.red;

import javafx.application.Application;


public final class MainCliente {

    private MainCliente() {
    }

    public static void main(String[] args) {
        Application.launch(ManejadorEventosCliente.class, args);
    }
}
