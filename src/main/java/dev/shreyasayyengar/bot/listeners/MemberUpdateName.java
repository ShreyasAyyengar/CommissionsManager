package dev.shreyasayyengar.bot.listeners;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
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

        if (DiscordBot.get().getCustomerManger().containsInfoOf(user.getId())) {
            Customer client = DiscordBot.get().getCustomerManger().get(user.getId());

            Stream.<IPermissionContainer>of(
                    client.getTextChannel(),
                    client.getVoiceChannel()
            ).forEach(channel -> channel.getManager().setName(channel.getName().replace(oldName, newName)).queue());

            client.getTextChannel().getManager().setTopic(client.getTextChannel().getTopic().replace(oldName, newName)).queue();
        }
    }
}
