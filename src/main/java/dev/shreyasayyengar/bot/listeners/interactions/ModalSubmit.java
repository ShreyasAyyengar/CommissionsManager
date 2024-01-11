package dev.shreyasayyengar.bot.listeners.interactions;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.customer.CustomerCommission;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.misc.utils.Util;
import dev.shreyasayyengar.bot.paypal.InvoiceDraft;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

@SuppressWarnings("ConstantConditions")
public class ModalSubmit extends ListenerAdapter {

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {

        if (event.getModalId().equalsIgnoreCase("request-form")) {
            Customer customer = DiscordBot.get().getCustomerManger().get(event.getUser().getId());

            String name = event.getValue("name").getAsString();
            String description = event.getValue("description").getAsString();
            String serverType = event.getValue("server-type").getAsString();
            String version = event.getValue("version").getAsString();
            String sourceCode = event.getValue("source-code").getAsString();
            boolean addSourceCode = sourceCode.equalsIgnoreCase("yes");

            String arrow = "<:purple_arrow:980020213863055390>";
            boolean longerDescription = description.length() > 1000;

            EmbedBuilder compiledResponsesEmbedBuilder = new EmbedBuilder();
            compiledResponsesEmbedBuilder.setTitle(customer.getHolder().getEffectiveName() + "'s Plugin Request");
            compiledResponsesEmbedBuilder.setDescription("For the plugin: `" + name + "`");

            if (longerDescription) {
                compiledResponsesEmbedBuilder.addField("Description", "See Below", false);
            } else compiledResponsesEmbedBuilder.addField("Description", arrow + " " + description, false);

            compiledResponsesEmbedBuilder.addField("Plugin/Server Type", arrow + " " + serverType, false);
            compiledResponsesEmbedBuilder.addField("Version", arrow + " " + version, false);
            compiledResponsesEmbedBuilder.addField("Source Code", arrow + " " + (addSourceCode ? "Yes" : "No") + "\nIf you would like this changed, please let me know!", false);
            compiledResponsesEmbedBuilder.setColor(Util.THEME_COLOUR);
            event.deferReply().queue();

            event.getHook().sendMessageEmbeds(compiledResponsesEmbedBuilder.build()).queue(commissionRequestDoneMessage -> {
                if (longerDescription) {
                    MessageEmbed longerDescriptionEmbed = new EmbedBuilder()
                            .setTitle("Description:")
                            .setDescription(description)
                            .setColor(Util.THEME_COLOUR)
                            .build();
                    customer.getTextChannel().sendMessageEmbeds(longerDescriptionEmbed).complete();
                }

                commissionRequestDoneMessage.pin().queue();
                customer.getTextChannel().sendMessage("<@690755476555563019>").complete();
                customer.getTextChannel().getHistory().retrievePast(2).complete().forEach(message -> message.delete().queue());

                customer.getCommissions().add(new CustomerCommission(customer, name, addSourceCode, commissionRequestDoneMessage.getId()));
            });
        }

        if (event.getModalId().equalsIgnoreCase("submit-email")) {

            String email = event.getValue("email").getAsString();

            if (!email.matches("([A-Za-z\\d-_.]+@[A-Za-z\\d-_]+(?:\\.[A-Za-z\\d]+)+)")) {
                event.replyEmbeds(EmbedUtil.invalidEmail()).setEphemeral(true).queue();
                return;
            }

            Customer customer = DiscordBot.get().getCustomerManger().get(event.getUser().getId());
            customer.setPaypalEmail(email);

            MessageEmbed emailRegisteredEmbed = new EmbedBuilder()
                    .setTitle("Register your PayPal Email")
                    .setDescription("**Success!**: `Your email has been registered`")
                    .setColor(Color.GREEN)
                    .setThumbnail("https://pngimg.com/uploads/paypal/paypal_PNG22.png")
                    .setFooter("The email " + customer.getPaypalEmail() + " has been registered and will be used for the future.")
                    .build();

            event.replyEmbeds(emailRegisteredEmbed).queue();
        }

        if (event.getModalId().toLowerCase().startsWith("sub-invoice.")) {

            CustomerCommission commission = DiscordBot.get().getCustomerManger().getByTextChannel(event.getChannel().asTextChannel()).getCommission(event.getModalId().split("\\.")[1]);

            String description = event.getValue("description").getAsString();
            int amount = Integer.parseInt(event.getValue("amount").getAsString());

            try {
                event.replyEmbeds(EmbedUtil.invoiceInProgress()).queue();

                new InvoiceDraft(commission, commission.getPluginName() + "-" + description, amount, event.getHook()).generateInvoice();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (event.getModalId().equalsIgnoreCase("vouch")) {

            String vouch = event.getValue("text-box").getAsString();

            MessageEmbed vouchEmbed;

            if (event.getValue("spigotmc").getAsString() != null && !event.getValue("spigotmc").getAsString().isEmpty()) {
                vouchEmbed = EmbedUtil.vouch(vouch, event.getMember(), event.getValue("spigotmc").getAsString());

            } else vouchEmbed = EmbedUtil.vouch(vouch, event.getMember());

            DiscordBot.get().workingGuild.getTextChannelById("980373571807367208").sendMessageEmbeds(vouchEmbed).queue();

            event.replyEmbeds(EmbedUtil.vouchSuccess()).queue();
        }

        if (event.getModalId().equalsIgnoreCase("feedback")) {

            String feedback = event.getValue("feedback").getAsString();

            event.replyEmbeds(EmbedUtil.feedbackSubmitted()).setEphemeral(true).queue();
            event.getGuild().getTextChannelById("982112203794685963").sendMessageEmbeds(EmbedUtil.feedback(feedback, event.getMember())).queue();
        }

    }
}