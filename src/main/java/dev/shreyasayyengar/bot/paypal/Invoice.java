package dev.shreyasayyengar.bot.paypal;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.CustomerCommission;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.misc.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
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

/**
 * An Invoice is a payment request that is sent to a user via PayPal. This class is used to wrap
 * information provided by PayPal's API and to store it properly within the program. <b>This class is not to be
 * instantiated directly</b>, but rather through the {@link InvoiceDraft} class. The {@link InvoiceDraft} class is
 * a <i>blueprint/builder like</i> class that is used to create an Invoice, and once everything is set,
 * it is pushed to PayPal with an HTTP request, and the information returned is stored in this class. Strong
 * encapsulation is required for this class, as it is not meant to be used directly nor is it meant to have data
 * exposed outside this class.
 * <p></p>
 *
 * @author Shreyas Ayyengar
 * @see InvoiceDraft
 */
public class Invoice {
    public static final Collection<Invoice> INVOICES = new HashSet<>();

    private final Collection<File> filesHolding = new HashSet<>();
    private final Customer customer;
    private final CustomerCommission commission;
    private final String invoiceID;
    private final String messageID;
    private String status;
    private String clientEmail;
    private String total;
    private String currency;
    private String merchantName;
    private String merchantEmail;
    private File QRCodeImg;

    /**
     * The registerInvoices method is used to register all the invoices that are currently stored in the database.
     * This is only called once, when the program is started via {@link DiscordBot#deserialiseMySQLData()}.
     */
    public static void registerInvoices() {
        DiscordBot.get().database.preparedStatementBuilder("SELECT * FROM CM_invoice_info").executeQuery(resultSet -> {
            try {
                while (resultSet.next()) {
                    String customerId = resultSet.getString("client_id");
                    String invoiceId = resultSet.getString("invoice_id");
                    String messageId = resultSet.getString("message_id");
                    String commissionName = resultSet.getString("commission_name");

                    new Invoice(customerId, commissionName, messageId, invoiceId);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        cycleChecks();
    }

    /**
     * The cycleChecks method loops through all invoices inside the {@link #INVOICES} collection, and checks if they
     * are paid or not. Action is only taken if the invoice is paid, and is done so by updating the {@link #status} of the
     * invoice object. The MessageEmbed, which would've been sent to the user immediately after the invoice was created
     * ({@link #getInvoiceEmbed()}) is updated to reflect the new status, provided by {@link #getPaidInvoiceEmbed()}.
     */
    private static void cycleChecks() {
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
    }

    /**
     * This is the main constructor for this object, and is used to create a final Invoice object from an {@link InvoiceDraft} object.
     * It assigns all the private variables and sends embeds to the client's text channel to let them know their invoice has been
     * generated properly. <b>This constructor is not to be used directly</b>, but rather through the {@link InvoiceDraft} class.
     * (hence the protected access modifier).
     */
    protected Invoice(CustomerCommission commission, JSONObject invoiceData, InteractionHook interactionHook) throws IOException {

        this.customer = commission.getCustomer();
        this.commission = commission;

        this.invoiceID = invoiceData.getString("id");
        this.status = invoiceData.getString("status").equalsIgnoreCase("paid") ? "PAID" : "UNPAID";
        this.clientEmail = invoiceData.getJSONArray("primary_recipients").getJSONObject(0).getJSONObject("billing_info").getString("email_address");
        this.total = invoiceData.getJSONObject("amount").getString("value");
        this.currency = invoiceData.getJSONObject("amount").getString("currency_code");
        this.merchantName = invoiceData.getJSONObject("invoicer").getJSONObject("name").getString("full_name");
        this.merchantEmail = invoiceData.getJSONObject("invoicer").getString("email_address");

        setQRCode();

        Button paypalButton = Button.link("https://www.paypal.com/invoice/p/#" + invoiceID, Emoji.fromFormatted("<:PayPal:933225559343923250>")).withLabel("Pay via PayPal");
        Message invoiceEmbed = interactionHook.editOriginalEmbeds(getInvoiceEmbed().build())
                .setActionRow(paypalButton)
                .setFiles(FileUpload.fromData(this.QRCodeImg, "qr_code.png"))
                .complete();
        invoiceEmbed.pin().queue();
        invoiceEmbed.getChannel().getHistory().retrievePast(1).completeAfter(1, TimeUnit.SECONDS).forEach(message -> message.delete().queue());
        this.messageID = invoiceEmbed.getId();

        this.commission.getInvoices().add(this);
        INVOICES.add(this);
    }

    /**
     * This is the secondary constructor for this object, and is invoked when pulling information from the
     * database during a restart/reload of the discord bot. <b>This method cannot be used anywhere else other than this class</b>
     * hence the private access modifier.
     */
    private Invoice(String customerID, String commissionName, String messageID, String invoiceID) {

        this.customer = DiscordBot.get().getCustomerManger().get(customerID);
        this.commission = customer.getCommission(commissionName);

        this.messageID = messageID;
        this.invoiceID = invoiceID;

        this.commission.getInvoices().add(this);
        INVOICES.add(this);
    }

    /**
     * This method makes regular calls to PayPal's API requesting if the Invoice has been paid or not.
     * If the invoice has been paid successfully and the status returned is "<code>PAID</code>", then
     * the {@link #status} is updated to reflect this, and the {@link #getPaidInvoiceEmbed()} method is called to
     * edit the embed to reflect the new status. No action is taken if the return status is not "<code>PAID</code>".
     *
     * @apiNote This method is called automatically by the {@link #cycleChecks()}
     */
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

            Button viewInvoiceButton = Button.link("https://www.paypal.com/invoice/p/#" + invoiceID, Emoji.fromFormatted("<:PayPal:933225559343923250>")).withLabel("View Invoice via PayPal");
            customer.getTextChannel().retrieveMessageById(messageID).complete().editMessageEmbeds(getPaidInvoiceEmbed()).setActionRow(viewInvoiceButton).setFiles().queue();

            MessageEmbed embed = new EmbedBuilder(EmbedUtil.invoicePaid())
                    .setDescription("{name}'s invoice has been paid!".replace("{name}", customer.getHolder().getEffectiveName()))
                    .build();
            customer.getTextChannel().sendMessageEmbeds(embed).setContent("@here").queue();

            closeInvoice();
            releaseFiles();
        }

        response.close();
    }

    /**
     * The closeInvoice method internally closes the invoice and discards the data
     * associated with it. This method is called either when the invoice is paid, cancelled
     * or when removed from the database.
     */
    private void closeInvoice() {
        INVOICES.remove(this);
        this.commission.getInvoices().remove(this);

        try {
            DiscordBot.get().database.preparedStatementBuilder("DELETE FROM CM_invoice_info WHERE invoice_id = ?")
                    .setString(invoiceID).executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method calls the PayPal's API and obtains and stores the QR code image for this invoice, ({@link #QRCodeImg})
     * and uses the image as a thumbnail when generating Embeds.
     */
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
                .setAuthor("Invoice: " + this.invoiceID, null, customer.getHolder().getEffectiveAvatarUrl())
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
        MessageEmbed previousInvoiceEmbed = customer.getTextChannel().retrieveMessageById(messageID).complete().getEmbeds().get(0);
        EmbedBuilder invoiceEmbed = new EmbedBuilder(previousInvoiceEmbed);
        invoiceEmbed.getFields().set(0, new MessageEmbed.Field("**Status:** `" + this.status + "` ✅", "", false));
        invoiceEmbed.setThumbnail("attachment://qr_code.png");
        invoiceEmbed.addField("**Date Paid:**", "<t:" + new Date().getTime() / 1000 + ":F>", false);
        invoiceEmbed.setColor(Color.GREEN);

        return invoiceEmbed.build();
    }

    /**
     * Nudges and prompts the {@link Customer}s holder to pay the invoice.
     */
    public void nudgePayment(ButtonInteractionEvent event) {
        Button paypalButton = Button.link("https://www.paypal.com/invoice/p/#" + this.getID(), Emoji.fromFormatted("<:PayPal:933225559343923250>")).withLabel("Pay via PayPal");
        event.getInteraction().replyEmbeds(EmbedUtil.nudge(this)).addActionRow(paypalButton).setContent(customer.getHolder().getAsMention()).queue();
    }

    /**
     * Adds a file to the invoices holding files. ({@link #filesHolding})
     * This method is directly called via {@link dev.shreyasayyengar.bot.customer.conversation.impl.InvoiceAddFileConversation}
     */
    public void addFileToHolding(File file) {
        this.filesHolding.add(file);
    }

    /**
     * This method releases all files contains in the {@link #filesHolding} list.
     * This enables automatic file release when the invoice is paid, therefore eliminating
     * the need for a human to send the files to the {@link Customer}'s text channel.
     */
    public void releaseFiles() {

        if (filesHolding.size() == 0) return;

        MessageEmbed builder = new EmbedBuilder()
                .setTitle("Files in Holding Below:")
                .setDescription("This commission contained files that were only meant to be released once the invoice was paid. Now that it has been paid, here are the released files:")
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .setFooter("For the invoice: " + invoiceID + " | For the commission: " + commission.getPluginName())
                .build();

        customer.getTextChannel().sendMessageEmbeds(builder).queue();

        List<File> files = filesHolding.stream().toList();

        files.stream()
                .skip(1)
                .reduce(
                        customer.getTextChannel().sendFiles(FileUpload.fromData(files.get(0))),
                        (action, file) -> action.addFiles(FileUpload.fromData(file, file.getName())),
                        (a, b) -> a
                )
                .queue();

        filesHolding.forEach(File::delete);
        filesHolding.clear();
    }

    /**
     * This method cancels the invoice, both internally and externally through PayPal's API.
     */
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
                        .setDescription("{name}'s invoice has been cancelled!".replace("{name}", customer.getHolder().getEffectiveName()))
                        .setColor(Color.RED)
                        .setFooter("For the invoice: " + invoiceID + " | For the commission: " + commission.getPluginName())
                        .setTimestamp(new Date().toInstant())
                        .build();

                customer.getTextChannel().sendMessageEmbeds(embed).setContent("@here").queue();
                customer.getTextChannel().deleteMessageById(messageID).queue();
            }

            response.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serialises the invoice to the MySQL database (if not paid).
     */
    public void serialise() {
        try {
            ResultSet resultSet = DiscordBot.get().database.preparedStatementBuilder("SELECT * FROM CM_invoice_info WHERE invoice_id = ?")
                    .setString(invoiceID)
                    .build().executeQuery();

            if (!resultSet.next()) {
                DiscordBot.get().database.preparedStatementBuilder("insert into CM_invoice_info (invoice_id, message_id, client_id, commission_name) values (?, ?, ?, ?);")
                        .setString(this.invoiceID)
                        .setString(this.messageID)
                        .setString(this.customer.getHolder().getId())
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