package dev.shreyasayyengar.bot.misc.utils;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.CustomerCommission;
import dev.shreyasayyengar.bot.paypal.Invoice;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.util.Date;

/**
 * The EmbedUtil class is a static utility class that contains methods
 * that return MessageEmbed objects, depending on what type of
 * embed needs to be sent. Static methods here are called all
 * throughout the lifetime of the bot.
 * <p></p>
 *
 * @author Shreyas A.
 */
public class EmbedUtil {

    private EmbedUtil() {
    }

    public static MessageEmbed onlyInPrivateChannels() {
        return new EmbedBuilder()
                .setTitle("Error while executing command...")
                .setDescription("`This command can only be used in private channels of clients!`")
                .setColor(Color.RED)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed invoiceInProgress() {
        return new EmbedBuilder()
                .setTitle("Generating Invoice...")
                .setFooter("This will just take a moment...")
                .setColor(Util.getColor())
                .build();
    }

    public static MessageEmbed invoicePaid() {
        return new EmbedBuilder()
                .setTitle("Invoice Paid!")
                .setColor(Color.GREEN)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed notAClient() {
        return new EmbedBuilder()
                .setTitle("Error while executing command...")
                .setDescription("`You cannot execute this command as a non-client!`")
                .setColor(Color.RED)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed joinedAsCollaborator(Member member) {
        return new EmbedBuilder()
                .setTitle("Collaborator Added!!")
                .setDescription(String.format("%s has joined as a collaborator for this channel!", member.getAsMention()))
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed removedAsCollaborator(Member member) {
        return new EmbedBuilder()
                .setTitle("Collaborator Removed!")
                .setDescription(String.format("%s has been removed as a collaborator for this channel!", member.getAsMention()))
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed notCollaborator() {
        return new EmbedBuilder()
                .setTitle("Error while executing command...")
                .setDescription("`This member is not a collaborator of this channel!`")
                .setColor(Color.RED)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed alreadyCollaborator() {
        return new EmbedBuilder()
                .setTitle("Error while executing command...")
                .setDescription("`This member is already a collaborator of this channel!`")
                .setColor(Color.RED)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed acceptedQuote() {
        return new EmbedBuilder()
                .setTitle("Quote Accepted!")
                .setDescription("You have accepted your commission quote! Here's a bit of extra information:")
                .addField("What happens now?", "The development will now begin, and progress will constantly be reported to you in this channel. " +
                        "Please let me know ASAP if you would like to cancel your commission.", false)
                .addField("What can I do right now:", "Right now you may use `/email` to input your PayPal email, which will " +
                        "be used to pay for the commission at a later point in time. Other than that, sit back and relax!", false)
                .addField("What happens if I don't respond?", "If you are unable to provide a response to some of my questions within 96 hours (4 days) " +
                        "then all development will halt temporarily until instructed to do so otherwise. ", false)
                .addField("When do I make my payment?", "An invoice will be generated via PayPal once I believe the plugin is functional. " +
                        "From there, you may test your plugin and report any bugs/changes/additions that you see fit, and I will return the new JAR file to you.", false)
                .setColor(Color.GREEN)
                .setFooter("If you have any questions, please reply here :)", DiscordBot.get().workingGuild.getOwner().getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed rejectedQuote() {
        return new EmbedBuilder()
                .setTitle("Quote Denied!")
                .setDescription("You have denied the quote!")
                .setFooter("Since you didn't agree to this quote, describe what you would like changed!")
                .setColor(Color.RED)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed noCommissions() {
        return new EmbedBuilder()
                .setTitle("Error while executing command...")
                .setDescription("There are no commissions available for this client!")
                .setColor(Color.RED)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed noInvoices() {
        return new EmbedBuilder()
                .setTitle("Error while executing command...")
                .setDescription("There are no pending invoices available for this client!")
                .setColor(Color.RED)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed selectCommission() {
        return new EmbedBuilder()
                .setDescription("Select a commission to view more information!")
                .setColor(Util.getColor())
                .build();
    }

    public static MessageEmbed commissionInformation(String pluginName) {
        return new EmbedBuilder()
                .setTitle("Commission Information: " + pluginName)
                .setColor(Util.getColor())
                .build();
    }

    public static MessageEmbed invoiceInformation(String invoiceID) {
        return new EmbedBuilder()
                .setTitle("Invoice Information: " + invoiceID)
                .setColor(Util.getColor())
                .build();
    }

    public static MessageEmbed noPriceSet() {
        return new EmbedBuilder()
                .setTitle("Error while processing...")
                .setDescription("`Cannot create invoice: This commission has no price set!`")
                .setFooter("Set a price for this commission via /commission")
                .setColor(Color.RED)
                .build();
    }

    public static MessageEmbed notConfirmed() {
        return new EmbedBuilder()
                .setTitle("Error while processing...")
                .setDescription("`This commission has not been confirmed by the client yet!`")
                .setFooter("Ask for confirmation via /commission")
                .setColor(Color.RED)
                .build();
    }

    public static MessageEmbed alreadyConfirmed() {
        return new EmbedBuilder()
                .setTitle("Error while processing...")
                .setDescription("`This commission has already been confirmed by the client!`")
                .setColor(Color.RED)
                .build();
    }

    public static MessageEmbed confirmCommission(CustomerCommission commission) {
        return new EmbedBuilder()
                .setTitle("Confirm Commission Price: " + commission.getPluginName())
                .setDescription("The price/quote for this commission is set for `$" + String.format("%.2f", commission.getFinalPrice()) + "` (**Inclusive** of Tax and SRC if requested).")
                .addField(":warning: This may not be the final price! :warning:", "As more work is completed and process continues, the price **may or may not** increase or decrease. " +
                        "If this happens to be the case **you will see a message just like this one** alerting you of a price change. ", false)
                .setFooter("Should there ever be a change in the price, I will always require your confirmation again, before generating any related invoices.", DiscordBot.get().workingGuild.getOwner().getEffectiveAvatarUrl())
                .setColor(Util.getColor())
                .build();
    }

    public static MessageEmbed sourceCodeUpdate(CustomerCommission commission) {
        return new EmbedBuilder()
                .setTitle("Source Code Update: " + commission.getPluginName())
                .setDescription(commission.hasRequestedSourceCode() ? "The client has requested the source code for this plugin." : "The client has removed the request for the source code for this plugin.")
                .setColor(Util.getColor())
                .build();
    }

    public static MessageEmbed priceUpdated(CustomerCommission commission) {
        return new EmbedBuilder()
                .setTitle("Price Updated: " + commission.getPluginName())
                .setDescription("The price for this commission has been updated to `$" + commission.getPrice() + "`.")
                .setColor(Util.getColor())
                .build();
    }

    public static MessageEmbed cancelCommission(CustomerCommission commission) {
        return new EmbedBuilder()
                .setTitle("Cancelled Commission: " + commission.getPluginName())
                .setDescription("This commission has been cancelled and all relevant information has been discarded.")
                .setColor(Color.RED)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed commissionComplete(CustomerCommission commission) {
        return new EmbedBuilder()
                .setTitle("Commission Complete: " + commission.getPluginName())
                .setDescription("This commission has been completed and all relevant information has been discarded.")
                .addField("Thank you!", "Thank you for using my plugin services, and I hope you are satisfied with the final product!", false)
                .addField("Final Request", "It would mean **a lot** to me if you could vouch for my service, either on [My SpigotMC page](https://www.spigotmc.org/threads/open-%E2%9C%A8-high-quality-plugins-configurable-affordable-easy-experienced-%E2%9C%A8.513897/), " +
                        "or by clicking the vouch button below! **This would be extremely helpful to me**, and I would be very grateful for any feedback you may have!", false)
                .addField("Feedback", "If you **have any feedback** regarding my services or this discord server, please use `/feedback`.", false)
                .setColor(Color.GREEN)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed paypalEmailNotSet() {
        return new EmbedBuilder()
                .setTitle("Error while processing...")
                .setDescription("`The client has not set their PayPal email yet! Requesting now...`")
                .setColor(Color.RED)
                .build();
    }

    public static MessageEmbed vouch(String vouch, Member member, String spigotMC) {

        return new EmbedBuilder()
                .setAuthor(null, null, member.getUser().getAvatarUrl())
                .setTitle(member.getEffectiveName() + "'s Commission Vouch!")
                .addField("Vouch: ", vouch + " - " + member.getAsMention(), false)
                .addField("SpigotMC Username: ", "`" + spigotMC + "`", false)
                .setFooter("Thank you for leaving a vouch!")
                .setColor(Color.PINK)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed vouch(String vouch, Member member) {
        return new EmbedBuilder()
                .setAuthor(null, null, member.getUser().getAvatarUrl())
                .setTitle(member.getEffectiveName() + "'s Commission Vouch!")
                .addField("Vouch: ", vouch + " - " + member.getAsMention(), false)
                .setFooter("Thank you for leaving a vouch!")
                .setColor(Color.PINK)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed vouchSuccess() {
        return new EmbedBuilder()
                .setTitle("Vouch Success!")
                .setDescription("Thank you for leaving a vouch! You can find your vouch in the <#980373571807367208> channel!")
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed invalidEmail() {
        return new EmbedBuilder()
                .setTitle("Invalid Email!")
                .setDescription("The email you provided is not a valid email address!")
                .setColor(Color.RED)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed feedback(String feedback, Member member) {
        return new EmbedBuilder()
                .setTitle("New Feedback!")
                .addField("Feedback: ", "`" + feedback + "` - " + member.getAsMention(), false)
                .setColor(Color.PINK)
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed feedbackSubmitted() {
        return new EmbedBuilder()
                .setTitle("Feedback Submitted!")
                .setDescription("Thank you for submitting your feedback! The feedback will be reviewed and responded to as soon as possible!")
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed statistics() {
        return new EmbedBuilder()
                .setTitle("Bot Usage Statistics")
                .addField("Total Memory Allocated:", Runtime.getRuntime().totalMemory() + "", false)
                .addField("Total Memory Free:", Runtime.getRuntime().freeMemory() + "", false)
                .addField("Total Memory Used:", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) + "", false)
                .addField("Total Megabytes Used: ", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1000 * 1000) + "M", false)
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed garbageCollected() {
        return new EmbedBuilder()
                .setTitle("Garbage Collected!")
                .setDescription("The garbage collector has been run!")
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed inviteEmbed() {
        return new EmbedBuilder()
                .setDescription("**__https://discord.gg/5nFQBzy7Xx__**\n\n**Use this link to invite others!**\n*shreyasayyengar.dev*")
                .setColor(Util.getColor())
                .setThumbnail(DiscordBot.get().workingGuild.getIconUrl())
                .build();
    }

    public static MessageEmbed recordingFinished() {
        return new EmbedBuilder()
                .setTitle("Recording Finished!")
                .setDescription("The recent voice channel has been recorded and can be found below!")
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed exiting() {
        return new EmbedBuilder()
                .setTitle("Exiting...")
                .setDescription("The bot is now exiting and shutting down. If this is an IDE stagnant crash, please restart the bot through your IDE!")
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed requestPurge(User user) {
        return new EmbedBuilder()
                .setTitle(user.getAsTag() + " has left the server!")
                .setDescription("Their information has already been removed from the database!")
                .addField("User ID: ", user.getId(), false)
                .setColor(new Color(213, 171, 255))
                .setTimestamp(new Date().toInstant())
                .setFooter("To purge their channel, click the button below.")
                .build();
    }

    public static MessageEmbed nudge(Invoice invoice) {
        return new EmbedBuilder()
                .setTitle("Outstanding Invoice!")
                .setDescription("This is a reminder that you have an outstanding invoice!")
                .addField("Invoice ID: ", invoice.getID(), false)
                .setFooter("Click the button below to pay it via PayPal!")
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed askPurpose(CustomerCommission commission) {
        return new EmbedBuilder()
                .setTitle("Before generating an invoice:")
                .setDescription("Is this invoice for the main product of the commission: `" + commission.getPluginName() + "`?")
                .setColor(Util.getColor())
                .build();
    }

    public static MessageEmbed noAttachments() {
        return new EmbedBuilder()
                .setTitle("No Attachments!")
                .setDescription("You must attach a file to your message to be added to the invoice's file holding.")
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed addAttachments(Invoice invoice) {
        return new EmbedBuilder()
                .setTitle("Add attachments to the invoice: " + invoice.getID())
                .setDescription("These files will be added to the invoice's file holding. " +
                        "Once the invoice is paid, the files will be released to the client!")
                .setFooter("Please send any attachments below.")
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed attachmentsAdded() {
        return new EmbedBuilder()
                .setTitle("Attachments Added!")
                .setDescription("Success! The attachments have been added to the invoice!")
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed checkDMForMore() {
        return new EmbedBuilder()
                .setTitle("Check your DMs for more instructions")
                .setDescription("You can upload files to holding from there.")
                .setColor(Util.getColor())
                .setTimestamp(new Date().toInstant())
                .build();
    }

    public static MessageEmbed doesNotWork() {
        return new EmbedBuilder()
                .setTitle("\"This isn't working!\", \"Nothing happens!\", \"I am getting errors!\"")
                .setDescription("""
                        If you are having issues with the latest .jar sent to you, please be more detailed on what isn't working.\s

                        Is there a specific command that is failing or not responding? Is there a console error that is being thrown. Is there a specific error message that is being thrown? Please include all of the above in your message to aid in troubleshooting.""")
                .setColor(Util.getColor())
                .setFooter("--CommissionsManager--")
                .build();
    }

    public static MessageEmbed alreadyRequesting() {
        return new EmbedBuilder()
                .setTitle("Already Requesting!")
                .setDescription("You are already requesting a commission! Please wait until your current request is finished before requesting another one!")
                .setColor(Color.RED)
                .setTimestamp(new Date().toInstant())
                .setFooter("To cancel your current request, type !stoprequest")
                .build();
    }
}