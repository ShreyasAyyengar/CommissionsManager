package dev.shreyasayyengar.bot.listeners;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientInfo;
import dev.shreyasayyengar.bot.misc.managers.ClientInfoManager;
import dev.shreyasayyengar.bot.misc.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

public class MemberRemove extends ListenerAdapter {

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        User user = event.getUser();

        ClientInfo clientInfo = DiscordBot.get().getClientManger().get(user.getId());
        TextChannel channel  = clientInfo.getTextChannel();

        ClientInfoManager.purgeMemberSQL(user);

        channel.sendMessageEmbeds(EmbedUtil.requestPurge(user))
                .setActionRow(Button.danger("purge-channel", "Purge Channels").withEmoji(Emoji.fromMarkdown("\uD83D\uDDD1️")))
                .queue();
    }
}
