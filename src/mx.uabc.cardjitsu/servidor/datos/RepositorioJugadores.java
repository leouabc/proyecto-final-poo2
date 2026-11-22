package servidor.datos;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import modelo.JavaBeans;

public class RepositorioJugadores {

    public synchronized boolean registrarJugador(String nombre, String contra) {
        if (buscarPorNombre(nombre) != null) {
            return false;
        }
        ObjectContainer db = ManejadorDB4O.getConexion();
        JavaBeans nuevo = new JavaBeans(nombre, contra);
        db.store(nuevo);
        db.commit();
        return true;
    }

    public synchronized JavaBeans iniciarSesion(String nombre, String contra) {
        JavaBeans encontrado = buscarPorNombre(nombre);
        if (encontrado != null && encontrado.getContra().equals(contra)) {
            return encontrado;
        }
        return null;
    }

    public synchronized JavaBeans buscarPorNombre(String nombre) {
        ObjectContainer db = ManejadorDB4O.getConexion();
        JavaBeans ejemplo = new JavaBeans();
        ejemplo.setNombre(nombre);

        ObjectSet<JavaBeans> resultados = db.queryByExample(ejemplo);
        if (resultados.hasNext()) {
            return resultados.next();
        }
        return null;
    }

    public synchronized void actualizarResultadoPartida(String nombre, boolean gano) {
        ObjectContainer db = ManejadorDB4O.getConexion();
        JavaBeans guerrero = buscarPorNombre(nombre);

        if (guerrero != null) {
            if (gano) {
                guerrero.setVictorias(guerrero.getVictorias() + 1);
                guerrero.setPuntos(guerrero.getPuntos() + 100);
            } else {
                guerrero.setDerrotas(guerrero.getDerrotas() + 1);
                guerrero.setPuntos(Math.max(0, guerrero.getPuntos() - 25));
            }
            actualizarRango(guerrero);
            db.store(guerrero);
            db.commit();
        }
    }

    private void actualizarRango(JavaBeans guerrero) {
        int v = guerrero.getVictorias();
        if (v >= 1000) {
            guerrero.setRango("Ultra Instinto");
        } else if (v >= 500) {
            guerrero.setRango("Super Saiyajin Blue");
        } else if (v >= 250) {
            guerrero.setRango("Super Saiyajin Dios");
        } else if (v >= 120) {
            guerrero.setRango("Super Saiyajin 3");
        } else if (v >= 50) {
            guerrero.setRango("Super Saiyajin");
        } else if (v >= 15) {
            guerrero.setRango("Guerrero Z");
        } else {
            guerrero.setRango("Guerrero Clase Baja");
        }
    }

    public synchronized List<JavaBeans> obtenerRanking() {
        ObjectContainer db = ManejadorDB4O.getConexion();
        ObjectSet<JavaBeans> todos = db.query(JavaBeans.class);
        List<JavaBeans> ranking = new ArrayList<>(todos);
        ranking.sort(Comparator.comparingInt(JavaBeans::getVictorias)
                .thenComparingInt(JavaBeans::getPuntos)
                .reversed());
        return ranking;
    }
}
