package redes.broker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class ConsumoApi {
    /**
     * Método para obtener el precio de una criptomoneda desde la API de Binance.
     * @param symbol El símbolo de la criptomoneda (ejemplo: "BTCUSDT").
     * @return Un String con el símbolo y el precio de la criptomoneda.
     */
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
