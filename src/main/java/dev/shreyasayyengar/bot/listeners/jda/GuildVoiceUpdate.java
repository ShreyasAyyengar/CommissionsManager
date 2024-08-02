package dev.shreyasayyengar.bot.listeners.jda;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.customer.Customer;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GuildVoiceUpdate extends ListenerAdapter {
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelJoined() == null) {
            AudioChannelUnion channelLeft = event.getChannelLeft();

            Customer customer = DiscordBot.get().getCustomerManger().getMap().get(event.getMember().getId());
            if (customer == null) return; // either bot or myself
            if (!customer.getTemporaryVoiceChannel().getId().equalsIgnoreCase(channelLeft.getId())) return;

            // customer left their temporary voice channel.
            customer.getTemporaryVoiceChannel().delete();
        }
    }
}
