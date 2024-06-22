package dev.shreyasayyengar.bot.functional.action;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public interface ModalSubmitAction {
    void onSubmit(User modalUser, ModalInteractionEvent modalEvent);
}