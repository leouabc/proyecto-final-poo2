package modelo;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class InspectorReglasUtil {

    public static Map<String, Object> inspeccionarAtributos(Object bean) {
        Map<String, Object> propiedades = new HashMap<>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass(), Object.class);
            
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                Method getter = pd.getReadMethod();
                if (getter != null) {
                    propiedades.put(pd.getName(), getter.invoke(bean));
                }
            }
        } catch (Exception e) {
            System.err.println("Error al inspeccionar el JavaBean: " + e.getMessage());
        }
        return propiedades;
    }

    public static void inyectarReglaOConfiguracion(Object bean, String propiedad, Object valor) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass(), Object.class);
            
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if (pd.getName().equals(propiedad)) {
                    Method setter = pd.getWriteMethod();
                    if (setter != null) {
                        setter.invoke(bean, valor);
                        System.out.println("Propiedad '" + propiedad + "' actualizada dinámicamente a: " + valor);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error al inyectar configuración: " + e.getMessage());
        }
    }
}
