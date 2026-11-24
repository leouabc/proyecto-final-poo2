package modelo;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;

public final class InspectorReglasUtil {
    private InspectorReglasUtil() { }


    public static boolean inyectarPropiedadDinamica(Object bean, String propiedad, Object valor) {
        if (bean == null || propiedad == null || propiedad.isBlank()) return false;
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass(), Object.class);
            for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
                if (!descriptor.getName().equals(propiedad)) continue;
                Method setter = descriptor.getWriteMethod();
                if (setter == null) return false;
                Class<?> tipo = setter.getParameterTypes()[0];
                if (valor != null && !tipo.isAssignableFrom(valor.getClass())) return false;
                setter.invoke(bean, valor);
                return true;
            }
        } catch (Exception ex) {
            System.err.println("Error de introspección en '" + propiedad + "': " + ex.getMessage());
        }
        return false;
    }

    public static Carta crearCartaDesdeApi(String nombre, String raza, String imagenUrl, String poderApi, java.math.BigDecimal escalaPoder) {
        Carta carta = new Carta();
        boolean correcto = inyectarPropiedadDinamica(carta, "nombre", nombre)
                && inyectarPropiedadDinamica(carta, "raza", raza)
                && inyectarPropiedadDinamica(carta, "imagenUrl", imagenUrl)
                && inyectarPropiedadDinamica(carta, "poderApi", poderApi)
                && inyectarPropiedadDinamica(carta, "escalaPoder", escalaPoder);
        if (!correcto) {
            throw new IllegalStateException("No fue posible mapear todos los campos de Dragon Ball API a Carta.");
        }
        return carta;
    }

    public static void cargarCartaDesdeDragonBallAPI(Carta destino, int idPersonaje) {
        if (destino == null || idPersonaje <= 0) {
            throw new IllegalArgumentException("La carta y el ID del personaje deben ser válidos.");
        }
        try {
            Carta origen = DragonBallApiService.obtenerPersonajePorId(idPersonaje);
            inyectarPropiedadDinamica(destino, "nombre", origen.getNombre());
            inyectarPropiedadDinamica(destino, "raza", origen.getRaza());
            inyectarPropiedadDinamica(destino, "imagenUrl", origen.getImagenUrl());
            inyectarPropiedadDinamica(destino, "poderApi", origen.getPoderApi());
            inyectarPropiedadDinamica(destino, "escalaPoder", origen.getEscalaPoder());
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible cargar el personaje desde Dragon Ball API: " + ex.getMessage(), ex);
        }
    }
}
