package dev.shreyasayyengar.bot.listeners.interactions;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.CustomerCommission;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import dev.shreyasayyengar.bot.paypal.InvoiceDraft;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ConstantConditions")
public class ModalSubmit extends ListenerAdapter {

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {

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