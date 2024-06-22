package dev.shreyasayyengar.bot.functional.action;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface ButtonClickAction {
    void onClick(User buttonUser, ButtonInteractionEvent buttonInteractionEvent);
}