package dev.shreyasayyengar.bot.listeners.jda;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.functional.type.DiscordButton;
import dev.shreyasayyengar.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

public class MemberRemove extends ListenerAdapter {

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        User user = event.getUser();

        Customer customer = DiscordBot.get().getCustomerManger().get(user.getId());
        DiscordBot.get().getCustomerManger().handleRemoval(user);

        DiscordButton purgeButton = new DiscordButton(ButtonStyle.DANGER, "Purge Channels", "U+1F5D1", (buttonUser, buttonInteractionEvent) -> {
            customer.getTextChannel().delete().queue();
            if (customer.getTemporaryVoiceChannel() != null) customer.getTemporaryVoiceChannel().delete().queue();

            DiscordBot.get().getCustomerManger().getMap().remove(customer.getUser().getId());
        });
        customer.getTextChannel().sendMessageEmbeds(EmbedUtil.requestPurge(user)).setActionRow(purgeButton.asButton()).queue();
    }
}