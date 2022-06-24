package dev.shreyasayyengar.bot.paypal;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientCommission;
import dev.shreyasayyengar.bot.client.ClientInfo;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.misc.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import okhttp3.*;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Invoice {

    public static final Collection<Invoice> INVOICES = new HashSet<>();
    private static boolean LOOPING = false;

    private final Collection<File> filesHolding = new HashSet<>();
    private final ClientInfo clientInfo;
    private final ClientCommission commission;
    private final String invoiceID;
    private final String messageID;
    private String status;
    private String clientEmail;
    private String total;
    private String currency;
    private String merchantName;
    private String merchantEmail;
    private File QRCodeImg;

    public static void registerInvoices() {
        try {
            ResultSet resultSet = DiscordBot.get().database.preparedStatementBuilder("SELECT * FROM CM_invoice_info").build().executeQuery();

            while (resultSet.next()) {
                String clientInfoId = resultSet.getString("client_id");
                String invoiceId = resultSet.getString("invoice_id");
                String messageId = resultSet.getString("message_id");
                String commissionName = resultSet.getString("commission_name");

                new Invoice(clientInfoId, commissionName, messageId, invoiceId);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void cycleChecks() {
        if (!LOOPING) {
            ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();
            scheduledService.scheduleAtFixedRate(() -> {
                try {
                    for (Invoice invoice : INVOICES) {
                        invoice.updateIfPaid();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, 0, 10, TimeUnit.SECONDS);

            LOOPING = true;
        }
    }

    public Invoice(ClientCommission commission, JSONObject invoiceData, InteractionHook interactionHook) throws IOException {

        this.clientInfo = commission.getClient();
        this.commission = commission;

        this.invoiceID = invoiceData.getString("id");
        this.status = invoiceData.getString("status").equalsIgnoreCase("paid") ? "PAID" : "UNPAID";
        this.clientEmail = invoiceData.getJSONArray("primary_recipients").getJSONObject(0).getJSONObject("billing_info").getString("email_address");
        this.total = invoiceData.getJSONObject("amount").getString("value");
        this.currency = invoiceData.getJSONObject("amount").getString("currency_code");
        this.merchantName = invoiceData.getJSONObject("invoicer").getJSONObject("name").getString("full_name");
        this.merchantEmail = invoiceData.getJSONObject("invoicer").getString("email_address");

        setQRCode();

        Button paypalButton = Button.link("https://www.paypal.com/invoice/p/#" + invoiceID, Emoji.fromMarkdown("<:PayPal:933225559343923250>")).withLabel("Pay via PayPal");
        Message invoiceEmbed = interactionHook.editOriginalEmbeds(getInvoiceEmbed().build())
                .setActionRow(paypalButton)
                .addFile(this.QRCodeImg, "qr_code.png")
                .complete();
        invoiceEmbed.pin().queue();
        invoiceEmbed.getTextChannel().getHistory().retrievePast(1).completeAfter(1, TimeUnit.SECONDS).forEach(message -> message.delete().queue());
        this.messageID = invoiceEmbed.getId();

        this.commission.getInvoices().add(this);
        INVOICES.add(this);

        cycleChecks();
    }

    public Invoice(String clientInfoID, String commissionName, String messageID, String invoiceID) {

        this.clientInfo = DiscordBot.get().getClientManger().get(clientInfoID);
        this.commission = clientInfo.getCommission(commissionName);

        this.messageID = messageID;
        this.invoiceID = invoiceID;

        this.commission.getInvoices().add(this);
        INVOICES.add(this);

        cycleChecks();
    }

    private void updateIfPaid() throws IOException {

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api-m.paypal.com/v2/invoicing/invoices/" + invoiceID)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + DiscordBot.get().getPaypalAccessToken())
                .build();

        Response response = client.newCall(request).execute();
        JSONObject jsonObject = new JSONObject(response.body().string());

        if (jsonObject.getString("status").equalsIgnoreCase("paid")) {
            this.status = "PAID";

            Button viewInvoiceButton = Button.link("https://www.paypal.com/invoice/p/#" + invoiceID, Emoji.fromMarkdown("<:PayPal:933225559343923250>")).withLabel("View Invoice via PayPal");
            clientInfo.getTextChannel().retrieveMessageById(messageID).complete().editMessageEmbeds(getPaidInvoiceEmbed()).setActionRow(viewInvoiceButton).clearFiles().queue();

            MessageEmbed embed = new EmbedBuilder(EmbedUtil.invoicePaid())
                    .setDescription("{name}'s invoice has been paid!".replace("{name}", clientInfo.getHolder().getEffectiveName()))
                    .build();
            clientInfo.getTextChannel().sendMessageEmbeds(embed).content("@here").queue();
            closeInvoice();

            releaseFiles();
        }

        response.close();
    }

    public void nudgePayment() {
        Button paypalButton = Button.link("https://www.paypal.com/invoice/p/#" + this.getID(), Emoji.fromMarkdown("<:PayPal:933225559343923250>")).withLabel("Pay via PayPal");
        clientInfo.getTextChannel().sendMessageEmbeds(EmbedUtil.nudge(this)).setActionRow(paypalButton).content("@here").queue();
    }

    public void releaseFiles() {

        if (filesHolding.size() == 0) return;

        MessageEmbed builder = new EmbedBuilder()
                .setTitle("Files in Holding Below:")
                .setDescription("This commission contained files that were only meant to be released once the invoice was paid. Now that it has been paid, here are the released files:")
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .setFooter("For the invoice: " + invoiceID + " | For the commission: " + commission.getPluginName())
                .build();

        clientInfo.getTextChannel().sendMessageEmbeds(builder).queue();

        List<File> files = filesHolding.stream().toList();

        files.stream()
                .skip(1)
                .reduce(
                        clientInfo.getTextChannel().sendFile(files.get(0)),
                        (action, file) -> action.addFile(file, file.getName()),
                        (a, b) -> a
                )
                .queue();

        filesHolding.forEach(File::delete);
        filesHolding.clear();
    }

    public void addFileToHolding(File file) {
        this.filesHolding.add(file);
    }

    public void cancel() {
        closeInvoice();

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        RequestBody body = RequestBody.create(new JSONObject().put("subject", "Invoice Cancelled").put("note", "Commission Invoice has been Cancelled!").put("send_to_invoicer", true).put("send_to_recipient", true).toString(), JSON);

        Request request = new Request.Builder()
                .url("https://api-m.paypal.com/v2/invoicing/invoices/" + invoiceID + "/cancel")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + DiscordBot.get().getPaypalAccessToken())
                .post(body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (String.valueOf(response.code()).startsWith("2")) {
                MessageEmbed embed = new EmbedBuilder()
                        .setTitle("Invoice Cancelled")
                        .setDescription("{name}'s invoice has been cancelled!".replace("{name}", clientInfo.getHolder().getEffectiveName()))
                        .setColor(Color.RED)
                        .setFooter("For the invoice: " + invoiceID + " | For the commission: " + commission.getPluginName())
                        .setTimestamp(new Date().toInstant())
                        .build();

                clientInfo.getTextChannel().sendMessageEmbeds(embed).content("@here").queue();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeInvoice() {
        INVOICES.remove(this);
        this.commission.getInvoices().remove(this);

        try {
            DiscordBot.get().database.preparedStatementBuilder("DELETE FROM CM_invoice_info WHERE invoice_id = ?")
                    .setString(invoiceID)
                    .build().executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setQRCode() throws IOException {

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json");

        RequestBody body = RequestBody.create(new JSONObject().put("width", "400").put("height", "400").toString(), JSON);

        Request request = new Request.Builder()
                .url("https://api.paypal.com/v2/invoicing/invoices/" + invoiceID + "/generate-qr-code")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + DiscordBot.get().getPaypalAccessToken())
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        String base64IMG = response.body().string().split("plain")[1].replace("\n", "").split("-")[0].strip();

        BufferedImage image;
        byte[] imageByte;

        imageByte = Base64.getDecoder().decode(base64IMG);

        ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
        image = ImageIO.read(bis);
        bis.close();

        this.QRCodeImg = new File("qr_code.png");
        ImageIO.write(image, "png", this.QRCodeImg);
        response.close();
    }

    private EmbedBuilder getInvoiceEmbed() {
        return new EmbedBuilder()
                .setAuthor("Invoice: " + this.invoiceID, null, clientInfo.getHolder().getEffectiveAvatarUrl())
                .setTitle("Important Information regarding your invoice:")
                .addField("**Status:** `" + this.status + "` ❌", "", false)
                .addField("**Billed to:**", "`" + this.clientEmail + "`\n\n", false)
                .addField("**Merchant Info:**", "Name: `" + this.merchantName + "`\n Email:`" + this.merchantEmail + "`\n\n", false)
                .addField("**Total:**", "`" + this.total + " " + this.currency + "`\n\n", false)
                .addField("**Date Issued:**", "<t:" + new Date().getTime() / 1000 + ":F>" + "\n\n", false)
                .setThumbnail("attachment://qr_code.png")
                .setTimestamp(new Date().toInstant())
                .setFooter("""
                        This message will update upon payment.

                        Reminder: This message only displays the most important information regarding your invoice. Please check the invoice on PayPal for more, official information.""")
                .setColor(Util.getColor());
    }

    private MessageEmbed getPaidInvoiceEmbed() {
        MessageEmbed previousInvoiceEmbed = clientInfo.getTextChannel().retrieveMessageById(messageID).complete().getEmbeds().get(0);
        EmbedBuilder invoiceEmbed = new EmbedBuilder(previousInvoiceEmbed);
        invoiceEmbed.getFields().set(0, new MessageEmbed.Field("**Status:** `" + this.status + "` ✅", "", false));
        invoiceEmbed.setThumbnail("attachment://qr_code.png");
        invoiceEmbed.addField("**Date Paid:**", "<t:" + new Date().getTime() / 1000 + ":F>", false);
        invoiceEmbed.setColor(Color.GREEN);

        return invoiceEmbed.build();
    }

    public void serialise() {
        try {
            ResultSet resultSet = DiscordBot.get().database.preparedStatementBuilder("SELECT * FROM CM_invoice_info WHERE invoice_id = ?")
                    .setString(invoiceID)
                    .build().executeQuery();

            if (!resultSet.next()) {
                DiscordBot.get().database.preparedStatementBuilder("insert into CM_invoice_info (invoice_id, message_id, client_id, commission_name) values (?, ?, ?, ?);")
                        .setString(this.invoiceID)
                        .setString(this.messageID)
                        .setString(this.clientInfo.getHolder().getId())
                        .setString(this.commission.getPluginName())
                        .build().executeUpdate();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------- GETTERS -------------------------------- //

    public String getID() {
        return invoiceID;
    }

    public String getMessageID() {
        return messageID;
    }
}