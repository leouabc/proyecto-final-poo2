package modelo;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class InspectorReglasUtil {

    public static void inyectarPropiedadDinamica(Object bean, String propiedad, Object valor) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass(), Object.class);
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if (pd.getName().equals(propiedad)) {
                    Method setter = pd.getWriteMethod();
                    if (setter != null) {
                        setter.invoke(bean, valor);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error de introspección: " + e.getMessage());
        }
    }

    public static void cargarCartaDesdeDragonBallAPI(Carta carta, int idPersonaje) {
        try {
            URL url = new URL("https://dragonball-api.com/api/characters/" + idPersonaje);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(url.openStream());
                StringBuilder respuesta = new StringBuilder();
                while (scanner.hasNext()) {
                    respuesta.append(scanner.nextLine());
                }
                scanner.close();

                String json = respuesta.toString();
                
                String nombre = extraerValorJson(json, "name");
                String ki = extraerValorJson(json, "ki"); 
                
                ki = ki.replace(".", "").replace(",", "");
                int poderCalculado = ki.equals("unknown") ? 0 : Integer.parseInt(ki);
                inyectarPropiedadDinamica(carta, "nombre", nombre);
                inyectarPropiedadDinamica(carta, "poder", poderCalculado);
                inyectarPropiedadDinamica(carta, "elemento", "Fuego"); 
                
                System.out.println("Éxito: Se cargó e inyectó dinámicamente a " + carta.getNombre() + " con poder " + carta.getPoder());
            } else {
                System.out.println("No se encontró el personaje en la web.");
            }
        } catch (Exception e) {
            System.err.println("Error en la conexión web: " + e.getMessage());
        }
    }

    private static String extraerValorJson(String json, String clave) {
        String claveBuscada = "\"" + clave + "\":\"";
        int inicio = json.indexOf(claveBuscada);
        if (inicio == -1) return "0";
        inicio += claveBuscada.length();
        int fin = json.indexOf("\"", inicio);
        return json.substring(inicio, fin);
    }
}
