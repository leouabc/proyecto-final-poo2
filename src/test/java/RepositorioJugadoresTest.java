import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import modelo.HistorialPartida;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import servidor.datos.ManejadorDB4O;
import servidor.datos.RepositorioJugadores;

class RepositorioJugadoresTest {
    @TempDir Path directorioTemporal;

    @Test
    void registraUsuariosActualizaEstadisticasYConsultaHistorial() {
        System.setProperty("cardjitsu.db", directorioTemporal.resolve("prueba.db4o").toString());
        try {
            RepositorioJugadores repo = new RepositorioJugadores();
            assertTrue(repo.registrarJugador("GokuTest", "clave123"));
            assertTrue(repo.registrarJugador("VegetaTest", "clave123"));
            assertNotNull(repo.iniciarSesion("GokuTest", "clave123"));
            assertNull(repo.iniciarSesion("GokuTest", "incorrecta"));

            HistorialPartida historial = new HistorialPartida();
            historial.setIdPartida("TEST-DB4O-01");
            historial.setFechaEpochMillis(System.currentTimeMillis());
            historial.setJugador1("GokuTest");
            historial.setJugador2("VegetaTest");
            historial.setGanador("GokuTest");
            historial.setPerdedor("VegetaTest");
            historial.setMotivoFinalizacion("PRUEBA AUTOMATIZADA");
            historial.setRondasJugador1(3);
            historial.setRondasJugador2(1);

            assertTrue(repo.registrarResultadoCompleto(historial));
            assertEquals(1, repo.obtenerHistorialJugador("GokuTest").size());
            assertEquals(1, repo.obtenerHistorialJugador("VegetaTest").size());
            assertEquals(1, repo.obtenerRanking().get(0).getVictorias());
        } finally {
            ManejadorDB4O.cerrarConexion();
            System.clearProperty("cardjitsu.db");
        }
    }
}
