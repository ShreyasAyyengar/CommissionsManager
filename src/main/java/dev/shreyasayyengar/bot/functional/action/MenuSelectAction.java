package dev.shreyasayyengar.bot.functional.action;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public interface MenuSelectAction {
    void onSelect(User selectUser, StringSelectInteractionEvent selectMenuInteractionEvent);
}
