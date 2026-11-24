package servidor.red;

import javafx.application.Application;

public final class MainServidor {

    private MainServidor() {
    }

    public static void main(String[] args) {
        Application.launch(ServidorSocket.class, args);
    }
}
