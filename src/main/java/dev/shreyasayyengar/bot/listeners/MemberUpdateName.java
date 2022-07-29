package dev.shreyasayyengar.bot.listeners;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.client.ClientInfo;
import net.dv8tion.jda.api.entities.IPermissionContainer;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class MemberUpdateName extends ListenerAdapter {

    @Override
    public void onUserUpdateName(@NotNull UserUpdateNameEvent event) {
        User user = event.getUser();
        String oldName = event.getOldName();
        String newName = event.getNewName();

        if (DiscordBot.get().getClientManger().containsInfoOf(user.getId())) {
            ClientInfo client = DiscordBot.get().getClientManger().get(user.getId());

            Stream.<IPermissionContainer>of(
                    client.getTextChannel().getParentCategory(),
                    client.getTextChannel(),
                    client.getVoiceChannel()
            ).forEach(channel -> channel.getManager().setName(channel.getName().replace(oldName, newName)).queue());
        }
    }
}
