package dev.shreyasayyengar.bot.listeners.jda;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

public class GuildVoiceUpdate extends ListenerAdapter {
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelJoined() == null) {
            AudioChannelUnion channelLeft = event.getChannelLeft();

            for (Customer customer : DiscordBot.get().getCustomerManger().getMap().values()) {
                if (channelLeft.getId().equalsIgnoreCase(customer.getTemporaryVoiceChannel().getId())) {
                    // customer left their temporary voice channel.
                    if (channelLeft.getMembers().isEmpty()) {
                        customer.getTemporaryVoiceChannel().delete().queueAfter(5, TimeUnit.SECONDS);
                    }
                }
            }
        }
    }
}
