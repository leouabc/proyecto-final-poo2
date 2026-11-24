package servidor.datos;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import modelo.HistorialPartida;
import modelo.JavaBeans;

public class RepositorioJugadores {
    private static final Object DB_LOCK = new Object();

    public boolean registrarJugador(String nombre, String contra) {
        synchronized (DB_LOCK) {
            if (!credencialesValidas(nombre, contra) || buscarPorNombreInterno(nombre) != null) return false;
            ObjectContainer db = ManejadorDB4O.getConexion();
            db.store(new JavaBeans(nombre.trim(), PasswordUtil.hash(contra)));
            db.commit();
            return true;
        }
    }

    public JavaBeans iniciarSesion(String nombre, String contra) {
        synchronized (DB_LOCK) {
            if (nombre == null || contra == null) return null;
            JavaBeans encontrado = buscarPorNombreInterno(nombre.trim());
            if (encontrado == null || encontrado.getContra() == null) return null;
            String guardada = encontrado.getContra();
            if (guardada.startsWith("PBKDF2$")) {
                return PasswordUtil.verificar(contra, guardada) ? encontrado : null;
            }
            if (guardada.equals(contra)) {
                encontrado.setContra(PasswordUtil.hash(contra));
                ObjectContainer db = ManejadorDB4O.getConexion();
                db.store(encontrado);
                db.commit();
                return encontrado;
            }
            return null;
        }
    }

    public JavaBeans buscarPorNombre(String nombre) {
        synchronized (DB_LOCK) {
            return buscarPorNombreInterno(nombre);
        }
    }

    private JavaBeans buscarPorNombreInterno(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        ObjectContainer db = ManejadorDB4O.getConexion();
        JavaBeans ejemplo = new JavaBeans();
        ejemplo.setNombre(nombre.trim());
        ObjectSet<JavaBeans> resultados = db.queryByExample(ejemplo);
        return resultados.hasNext() ? resultados.next() : null;
    }


    public void actualizarResultadoPartida(String nombre, boolean gano) {
        synchronized (DB_LOCK) {
            JavaBeans jugador = buscarPorNombreInterno(nombre);
            if (jugador == null) return;
            aplicarResultado(jugador, gano);
            ObjectContainer db = ManejadorDB4O.getConexion();
            db.store(jugador);
            db.commit();
        }
    }

    public boolean registrarResultadoCompleto(HistorialPartida historial) {
        if (historial == null || esVacio(historial.getJugador1()) || esVacio(historial.getJugador2())
                || esVacio(historial.getGanador()) || esVacio(historial.getPerdedor())) return false;
        synchronized (DB_LOCK) {
            JavaBeans jugador1 = buscarPorNombreInterno(historial.getJugador1());
            JavaBeans jugador2 = buscarPorNombreInterno(historial.getJugador2());
            if (jugador1 == null || jugador2 == null) return false;

            String ganador = historial.getGanador().toLowerCase(Locale.ROOT);
            String nombre1 = jugador1.getNombre().toLowerCase(Locale.ROOT);
            boolean ganoJugador1 = ganador.equals(nombre1);
            boolean ganoJugador2 = ganador.equals(jugador2.getNombre().toLowerCase(Locale.ROOT));
            if (ganoJugador1 == ganoJugador2) return false;

            aplicarResultado(jugador1, ganoJugador1);
            aplicarResultado(jugador2, ganoJugador2);

            ObjectContainer db = ManejadorDB4O.getConexion();
            db.store(jugador1);
            db.store(jugador2);
            db.store(historial);
            db.commit();
            return true;
        }
    }

    public List<HistorialPartida> obtenerHistorialJugador(String nombre) {
        List<HistorialPartida> resultado = new ArrayList<>();
        if (esVacio(nombre)) return resultado;
        synchronized (DB_LOCK) {
            ObjectSet<HistorialPartida> todos = ManejadorDB4O.getConexion().query(HistorialPartida.class);
            String buscado = nombre.trim().toLowerCase(Locale.ROOT);
            for (HistorialPartida registro : todos) {
                if (registro == null) continue;
                boolean coincide = mismo(registro.getJugador1(), buscado) || mismo(registro.getJugador2(), buscado);
                if (coincide) resultado.add(copiarHistorial(registro));
            }
        }
        resultado.sort(Comparator.comparingLong(HistorialPartida::getFechaEpochMillis).reversed());
        return resultado;
    }

    private static boolean mismo(String valor, String buscadoNormalizado) {
        return valor != null && valor.trim().toLowerCase(Locale.ROOT).equals(buscadoNormalizado);
    }

    private HistorialPartida copiarHistorial(HistorialPartida origen) {
        HistorialPartida copia = new HistorialPartida();
        copia.setIdPartida(origen.getIdPartida());
        copia.setFechaEpochMillis(origen.getFechaEpochMillis());
        copia.setJugador1(origen.getJugador1());
        copia.setJugador2(origen.getJugador2());
        copia.setGanador(origen.getGanador());
        copia.setPerdedor(origen.getPerdedor());
        copia.setMotivoFinalizacion(origen.getMotivoFinalizacion());
        copia.setRondasJugador1(origen.getRondasJugador1());
        copia.setRondasJugador2(origen.getRondasJugador2());
        copia.setPersonajeJugador1(origen.getPersonajeJugador1());
        copia.setKiJugador1(origen.getKiJugador1());
        copia.setPersonajeJugador2(origen.getPersonajeJugador2());
        copia.setKiJugador2(origen.getKiJugador2());
        return copia;
    }

    private void aplicarResultado(JavaBeans jugador, boolean gano) {
        if (gano) {
            jugador.setVictorias(jugador.getVictorias() + 1);
            jugador.setPuntos(jugador.getPuntos() + 100);
        } else {
            jugador.setDerrotas(jugador.getDerrotas() + 1);
            jugador.setPuntos(Math.max(0, jugador.getPuntos() - 25));
        }
        actualizarRango(jugador);
    }

    private void actualizarRango(JavaBeans jugador) {
        int victorias = jugador.getVictorias();
        if (victorias >= 1000) jugador.setRango("Ultra Instinto");
        else if (victorias >= 500) jugador.setRango("Super Saiyajin Blue");
        else if (victorias >= 250) jugador.setRango("Super Saiyajin Dios");
        else if (victorias >= 120) jugador.setRango("Super Saiyajin 3");
        else if (victorias >= 50) jugador.setRango("Super Saiyajin");
        else if (victorias >= 15) jugador.setRango("Guerrero Z");
        else jugador.setRango("Guerrero Clase Baja");
    }

    public List<JavaBeans> obtenerRanking() {
        synchronized (DB_LOCK) {
            ObjectSet<JavaBeans> todos = ManejadorDB4O.getConexion().query(JavaBeans.class);
            List<JavaBeans> ranking = new ArrayList<>();
            for (JavaBeans original : todos) {
                JavaBeans publico = new JavaBeans();
                publico.setNombre(original.getNombre());
                publico.setRango(original.getRango());
                publico.setVictorias(original.getVictorias());
                publico.setDerrotas(original.getDerrotas());
                publico.setPuntos(original.getPuntos());
                ranking.add(publico);
            }
            ranking.sort(Comparator.comparingInt(JavaBeans::getVictorias).reversed()
                    .thenComparing(Comparator.comparingInt(JavaBeans::getPuntos).reversed())
                    .thenComparing(JavaBeans::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
            return ranking;
        }
    }

    private boolean credencialesValidas(String nombre, String contra) {
        return nombre != null && nombre.trim().matches("[a-zA-Z0-9_]{3,24}")
                && contra != null && contra.length() >= 4 && contra.length() <= 72;
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
