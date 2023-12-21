package dev.shreyasayyengar.bot.listeners.jda;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

public class MemberRemove extends ListenerAdapter {

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        User user = event.getUser();

        Customer customer = DiscordBot.get().getCustomerManger().get(user.getId());

        DiscordBot.get().getCustomerManger().purgeMemberSQL(user);

        customer.getTextChannel().sendMessageEmbeds(EmbedUtil.requestPurge(user))
                .setActionRow(Button.danger("purge-channel", "Purge Channels").withEmoji(Emoji.fromUnicode("U+1F5D1")))
                .queue();
    }
}