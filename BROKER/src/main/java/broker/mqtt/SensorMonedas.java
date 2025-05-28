package broker.mqtt;

import java.util.Timer;
import java.util.TimerTask;

public class SensorMonedas {
    public static void iniciar(){
        String[] monedas = {"BTCUSDT", "SOLUSDT"}; //Monedas de prueba

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for(String moneda : monedas){
                    String precio = ConsumoApi.obtenerPrecio(moneda);
                    String nombre = moneda.replace("USDT", "").toLowerCase();
                    String topic = "crypto/" + nombre;
                    MqttPublicador.publicar(topic,precio);
                }
            }
        }, 0, 10_000); //Se va a ejecutar cada 10 seg
    }
}
