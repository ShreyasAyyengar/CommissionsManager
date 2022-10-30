package dev.shreyasayyengar.bot.paypal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientCommission;
import net.dv8tion.jda.api.interactions.InteractionHook;
import okhttp3.*;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.LinkedHashMap;

/**
 * The main purpose of an InvoiceDraft is to prepare data to be
 * sent to <a href="https://api-m.paypal.com/">PayPal's Rest API Endpoint</a>.
 * All information, like amount, items, currency, emails and addresses are stored
 * in the invoice_template.yml file from the resources' folder. Once converted
 * into a JSON object, and once placeholders are filled with the relevant data,
 * the JSON object is sent to the PayPal REST API endpoint, and the response is
 * parsed and turned into a {@link Invoice} object.
 * <p></p>
 *
 * @author Shreyas Ayyengar
 * @see Invoice
 */

@SuppressWarnings("ConstantConditions")
public class InvoiceDraft {

    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ClientCommission commission;
    private final InteractionHook interactionHook;
    private final String invoiceName = "Minecraft Plugin Development Services";
    private final String productName;
    private final double price;

    private String invoiceURL;

    /**
     * This is the public constructor of the InvoiceDraft class.
     *
     * @param commission      The {@link ClientCommission} object that is being drafted.
     * @param productName     The name of the product being sold. This is most commonly
     *                        is the value of {@link ClientCommission#pluginName}.
     * @param price           The price of the product being sold. This is most commonly
     * @param interactionHook The {@link InteractionHook} that triggered this request.
     */
    public InvoiceDraft(ClientCommission commission, String productName, double price, InteractionHook interactionHook) {
        this.commission = commission;
        this.productName = productName;
        this.price = price + 0.30;
        this.interactionHook = interactionHook;
    }

    /**
     * The BaseJSON object is builder-type object which formats the
     * invoice_template.yml file into a JSON object.
     */
    private BaseJSON getBaseJSON() throws IOException {

        File templateFile = new File("invoice_template.yml");
        InputStream inputStream = DiscordBot.get().getClass().getResourceAsStream("/invoice_template.yml");
        FileOutputStream fileOutput = new FileOutputStream(templateFile);
        inputStream.transferTo(fileOutput);

        Yaml yaml = new Yaml();
        Object loadedYaml = yaml.load(new FileReader(templateFile));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return new BaseJSON(gson.toJson(loadedYaml, LinkedHashMap.class));
    }

    /**
     * This method makes an HTTP request to the PayPal REST API endpoint, and returns
     * the Invoice data as a String response. This is used to obtain the final URL
     * to push the invoice from a Draft Invoice to a Sent Invoice.
     */
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

    /**
     * This method pushes the invoice draft (provided as a hrefURL as the Invoice URL)
     * to the PayPal REST API endpoint, and returns the Invoice data as a String response.
     * It is at this point where the invoice is no longer modified and the response is
     * entirely dictated by the PayPal REST API.
     */
    private void pushDraft(String hrefURL) throws IOException {
        this.invoiceURL = hrefURL;
        OkHttpClient client = new OkHttpClient();
        Request pushDraftRequest = new Request.Builder()
                .url(hrefURL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + DiscordBot.get().getPaypalAccessToken())
                .build();

        Response response = client.newCall(pushDraftRequest).execute();
        String draftRequestResponse = response.body().string();

        JSONObject responseObject = new JSONObject(draftRequestResponse);
        String draftInvoiceID = responseObject.getString("id"); // gets the PayPal api invoice ID

        // region Attempt to Submit to PayPal as a Sent Invoice
        RequestBody body = RequestBody.create(new JSONObject().put("send_to_invoicer", true).toString(), JSON);
        Request submitRequest = new Request.Builder()
                .url("https://api-m.paypal.com/v2/invoicing/invoices/" + draftInvoiceID + "/send")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + DiscordBot.get().getPaypalAccessToken())
                .post(body)
                .build();

        Response submitResponse = client.newCall(submitRequest).execute();

        if ((submitResponse.code() + "").startsWith("2")) {
            success();
        }
        submitResponse.close();
        response.close();
        //endregion
    }

    /**
     * This method is called when {@link #pushDraft(String)} is successful and
     * mainly notifies the client that the invoice has been sent.
     * <p></p>
     * <b>Most importantly, it takes the response of the HTTP request, and turns it into
     * a final {@link Invoice} object, which is then linked to the {@link dev.shreyasayyengar.bot.client.ClientInfo}</b>
     */
    private void success() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(invoiceURL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + DiscordBot.get().getPaypalAccessToken())
                .build();

        Response execute = client.newCall(request).execute();
        JSONObject invoiceDataObject = new JSONObject(execute.body().string());

        new Invoice(commission, invoiceDataObject, interactionHook);

        execute.close();
    }

    /**
     * This method triggers all the methods above and is called when an invoice needs
     * to be generated. It is essentially the main method of the InvoiceDraft class
     * and informs the program that the invoice has to be drafted, configured and then sent
     * to PayPal's API.
     */
    public void generateInvoice() throws IOException {
        String fixedInvoiceJSONData = getBaseJSON().fixJSON();
        String draftURL = getInvoiceDraftURL(fixedInvoiceJSONData);

        pushDraft(draftURL);
    }

    class BaseJSON {
        private String rawJSON;

        public BaseJSON(String rawJSON) {
            this.rawJSON = rawJSON;
        }

        /**
         * This method fixes the JSON object by replacing the placeholders with the
         * actual data of the to-be-created invoice.
         */
        public String fixJSON() {
            rawJSON = this.rawJSON
                    .replace("{client_email}", InvoiceDraft.this.commission.getClient().getPaypalEmail())
                    .replace("{email}", "shreyas.ayyengar@gmail.com")
                    .replace("{website}", "https://shreyasayyengar.dev")
                    .replace("{invoice_name}", InvoiceDraft.this.invoiceName)
                    .replace("{invoice_description}", InvoiceDraft.this.productName + " (Plugin Service)")
                    .replace("{invoice_amount}", InvoiceDraft.this.price + "");

            return rawJSON;
        }
    }
}