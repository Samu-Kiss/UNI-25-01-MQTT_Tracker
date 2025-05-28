package broker.mqtt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class ConsumoApi {
    public static String obtenerPrecio (String symbol){
        try{
            // Hacer petición
            URL url = new URL("https://api.binance.com/api/v3/ticker/price?symbol=" + symbol);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            // ¿Petición correcta? Codigo 200 (Bueno)
            int responseCod = conn.getResponseCode();
            if(responseCod != 200){
                throw new RuntimeException("Ha ocurrido un error en tiempo de ejecución.");
            }

            // Leer y hacer el parsing al JSON
            InputStreamReader lector = new InputStreamReader(conn.getInputStream());
            JsonObject json = JsonParser.parseReader(lector).getAsJsonObject();

            String simboloMoneda = json.get("symbol").getAsString();
            String precio = json.get("price").getAsString();

            return simboloMoneda + ": " + precio;

        }
        catch (IOException e){
            return "Error: " + e.getMessage();
        }
    }
}
