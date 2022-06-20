package dev.shreyasayyengar.bot.paypal;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.misc.utils.Department;
import dev.shreyasayyengar.bot.properties.PayPalProperty;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;

public class AccessTokenRequest {

    private final String access_token;

    public AccessTokenRequest() throws IOException {
        DiscordBot.log(Department.PayPal, "Obtaining access token through PayPal OAuth2...");

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create("grant_type=client_credentials", JSON);

        Request request = new Request.Builder()
                .url("https://api-m.paypal.com/v1/oauth2/token")
                .addHeader("Accept", "application/json")
                .addHeader("Accept-Language", "en_US")
                .addHeader("Authorization", Credentials.basic(PayPalProperty.CLIENT_ID.get(), PayPalProperty.CLIENT_SECRET.get()))
                .post(body)
                .build();

        Response response = client.newCall(request).execute();

        String responseJSON = response.body().string();
        JSONObject decode = new JSONObject(responseJSON);

        this.access_token = decode.getString("access_token");
        response.close();
    }

    public String getToken() {
        return access_token;
    }
}
