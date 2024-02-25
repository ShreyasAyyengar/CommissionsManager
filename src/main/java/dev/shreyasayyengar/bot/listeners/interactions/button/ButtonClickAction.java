package dev.shreyasayyengar.bot.listeners.interactions.button;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface ButtonClickAction {
    void onClick(User user, ButtonInteractionEvent buttonInteractionEvent);
}