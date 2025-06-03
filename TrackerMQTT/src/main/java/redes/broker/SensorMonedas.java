package redes.broker;

import com.google.gson.*;
import org.eclipse.paho.client.mqttv3.*;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Clase principal encargada de:
 * - Mantener la lista de monedas activas (según configuración recibida por MQTT)
 * - Consultar el precio de cada moneda periódicamente
 * - Publicar el precio en el tópico MQTT correspondiente
 * - Mostrar logs en consola con arte ASCII y colores según la variación del precio
 */
public class SensorMonedas {
    // Lista de monedas activas (ej: BTCUSDT, SOLUSDT)
    private static final List<String> monedas = new CopyOnWriteArrayList<>();
    private static final Map<String, Double> ultimoPrecio = new ConcurrentHashMap<>();
    // Cliente MQTT
    private static MqttClient cliente;

    // Códigos ANSI para colores
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";

    /**
     * Método principal que inicia el sensor de monedas.
     * Se conecta al broker MQTT, se suscribe a los tópicos necesarios
     * y comienza a consultar precios periódicamente.
     */
    public static void iniciar() {
        // Inicializar con BTCUSDT y SOLUSDT por defecto
        monedas.add("BTCUSDT");
        monedas.add("SOLUSDT");

        String broker = "tcp://broker.hivemq.com:1883";
        String clientId = "SensorMonedasBroker" + System.currentTimeMillis();
        try {
            cliente = new MqttClient(broker, clientId);
            cliente.connect();

            // Suscribirse a esp32/criptomonedas para actualizar la lista de monedas
            cliente.subscribe("esp32/criptomonedas", (topic, message) -> {
                String payload = new String(message.getPayload());
                try {
                    JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                    JsonArray array = json.getAsJsonArray("criptomonedas");
                    monedas.clear();
                    for (JsonElement el : array) {
                        String simbolo = el.getAsJsonObject().get("simbolo").getAsString();
                        monedas.add(simbolo.toUpperCase() + "USDT");
                    }
                    System.out.println("Monedas activas actualizadas: " + monedas);
                } catch (Exception e) {
                    System.err.println("Error procesando JSON de criptomonedas: " + e.getMessage());
                }
            });
        } catch (MqttException e) {
            System.err.println("Error conectando MQTT en SensorMonedas: " + e.getMessage());
            return;
        }

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            /**
             * Método que se ejecuta periódicamente para consultar el precio de cada moneda
             * y publicar el resultado en el tópico correspondiente.
             */
            public void run() {
                for (String moneda : monedas) {
                    String precioStr = ConsumoApi.obtenerPrecio(moneda).split(":")[1].trim();
                    String nombre = moneda.replace("USDT", "").toUpperCase();
                    String topic = "crypto/" + nombre.toLowerCase();
                    MqttPublicador.publicar(topic, precioStr);

                    double precioActual;
                    try {
                        precioActual = Double.parseDouble(precioStr);
                    } catch (Exception e) {
                        precioActual = -1;
                    }
                    Double precioAnterior = ultimoPrecio.get(moneda);
                    String color = ANSI_CYAN;
                    String flecha = "→";
                    String estado = "SIN CAMBIO";
                    if (precioAnterior != null && precioActual != -1) {
                        if (precioActual > precioAnterior) {
                            color = ANSI_GREEN;
                            flecha = "↑";
                            estado = "SUBIÓ";
                            NotificadorTelegram.enviarMensaje("\uD83D\uDE80 La criptomoneda " + nombre + " subió: $" + precioStr);
                        } else if (precioActual < precioAnterior) {
                            color = ANSI_RED;
                            flecha = "↓";
                            estado = "BAJÓ";
                            NotificadorTelegram.enviarMensaje("⚠️ La criptomoneda " + nombre + " bajó: $" + precioStr);
                        } else {
                            color = ANSI_YELLOW;
                            flecha = "→";
                            estado = "IGUAL";
                        }
                    }
                    ultimoPrecio.put(moneda, precioActual);

                    // Imprimir la tarjetica en consola
                    StringBuilder card = new StringBuilder();
                    card.append(color);
                    card.append("\n╔══════════════════════════════╗\n");
                    card.append(String.format("║  %8s  %s   %-7s       ║\n", nombre, flecha, estado));
                    card.append("╠══════════════════════════════╣\n");
                    card.append(String.format("║  Precio: %-17s   ║\n", precioStr));
                    card.append("╚══════════════════════════════╝\n");
                    card.append(ANSI_RESET);
                    System.out.print(card.toString());
                }
            }
        }, 0, 10_000); // Cada 10 segundos
    }
}
