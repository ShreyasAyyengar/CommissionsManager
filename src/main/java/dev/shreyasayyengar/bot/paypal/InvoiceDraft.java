package dev.shreyasayyengar.bot.paypal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientCommission;
import dev.shreyasayyengar.bot.properties.PayPalProperty;
import net.dv8tion.jda.api.interactions.InteractionHook;
import okhttp3.*;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;

@SuppressWarnings("ConstantConditions")
public class InvoiceDraft {

    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ClientCommission commission;
    private final String invoiceName = "Minecraft Plugin Development Services";
    private final String productName;
    private final double price;

    private final InteractionHook interactionHook;

    private String invoiceURL;

    public InvoiceDraft(ClientCommission commission, String productName, double price, InteractionHook interactionHook) {
        this.commission = commission;
        this.productName = productName;
        this.price = price + 0.30;
        this.interactionHook = interactionHook;
    }

    public void generateInvoice() throws IOException, URISyntaxException {
        String fixedInvoiceJSONData = getBaseJSON().fixJSON();
        String draftURL = getInvoiceDraftURL(fixedInvoiceJSONData);

        pushDraft(draftURL);
    }

    private BaseJSON getBaseJSON() throws URISyntaxException, FileNotFoundException {
        File f = new File(getClass().getClassLoader().getResource("invoice_template.yml").toURI());
        Yaml yaml = new Yaml();
        Object loadedYaml = yaml.load(new FileReader(f));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return new BaseJSON(gson.toJson(loadedYaml, LinkedHashMap.class));
    }

    private String getInvoiceDraftURL(String invoiceData) throws IOException {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(invoiceData, JSON);
        Request request = new Request.Builder()
                .url("https://api-m.paypal.com/v2/invoicing/invoices")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + DiscordBot.get().getPaypalAccessToken())
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        String href = new JSONObject(response.body().string()).getString("href");

        response.close();
        return href;
    }

    private void pushDraft(String hrefURL) throws IOException {
        this.invoiceURL = hrefURL;
        OkHttpClient client = new OkHttpClient();
        Request pushDraftRequest = new Request.Builder()
                .url(hrefURL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + DiscordBot.get().getPaypalAccessToken())
                .build();

        String draftRequestResponse = client.newCall(pushDraftRequest).execute().body().string();

        JSONObject responseObject = new JSONObject(draftRequestResponse);
        String draftInvoiceID = responseObject.getString("id"); // gets the PayPal api invoice ID

        // region Attempt to Submit
        RequestBody body = RequestBody.create(new JSONObject().put("send_to_invoicer", true).toString(), JSON);
        Request submitRequest = new Request.Builder()
                .url("https://api-m.paypal.com/v2/invoicing/invoices/" + draftInvoiceID + "/send")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + DiscordBot.get().getPaypalAccessToken())
                .post(body)
                .build();

        Response response = client.newCall(submitRequest).execute();

        if ((response.code() + "").startsWith("2")) {
            success();
        }
        response.close();
        //endregion
    }

    private void success() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(invoiceURL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + DiscordBot.get().getPaypalAccessToken())
                .build();

        JSONObject invoiceDataObject = new JSONObject(client.newCall(request).execute().body().string());

        new Invoice(commission, invoiceDataObject, interactionHook);
    }

    class BaseJSON {
        private String rawJSON;

        public BaseJSON(String rawJSON) {
            this.rawJSON = rawJSON;
        }

        public String fixJSON() {
            rawJSON = this.rawJSON
                    .replace("{client_email}", InvoiceDraft.this.commission.getClient().getPaypalEmail())
                    .replace("{email}", PayPalProperty.EMAIL.get()) // TODO
//                    .replace("{email}", "sb-37wlt16728216@business.example.com")
                    .replace("{website}", PayPalProperty.WEBSITE.get())
                    .replace("{invoice_name}", InvoiceDraft.this.invoiceName)
                    .replace("{invoice_description}", InvoiceDraft.this.productName + " (Plugin Service)")
                    .replace("{invoice_amount}", InvoiceDraft.this.price + "");

            return rawJSON;
        }
    }
}