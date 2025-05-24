package broker.mqtt;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class consumoApi {
    public void consumir (String symbol){
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
            else{
                // Abrir un scanner que lea el flujo de datos
                StringBuilder infoString = new StringBuilder();
                Scanner s = new Scanner(url.openStream());

                while(s.hasNext()){
                    infoString.append(s.nextLine());
                }
                s.close();

                System.out.println(infoString);
            }
        }
        catch (IOException e){
            e.getMessage();
        }
    }
}
