package dev.shreyasayyengar.bot.functional;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.functional.type.DiscordButton;
import dev.shreyasayyengar.bot.functional.type.DiscordMenu;
import dev.shreyasayyengar.bot.functional.type.DiscordModal;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class InteractionManager extends ListenerAdapter {
    private final Collection<DiscordButton> buttons = new HashSet<>();
    private final Collection<DiscordModal> modals = new HashSet<>();
    private final Collection<DiscordMenu> menus = new HashSet<>();

    public InteractionManager() {
        DiscordBot.get().bot().addEventListener(this);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        Collection<DiscordButton> buttonsCopy = new ArrayList<>(buttons);
        for (DiscordButton button : buttonsCopy) {
            if (button.getInternalId().toString().equals(event.getComponentId())) {
                button.getAction().onClick(event.getUser(), event);
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        Iterator<DiscordModal> iterator = modals.iterator();
        while (iterator.hasNext()) {
            DiscordModal modal = iterator.next();
            if (modal.getInternalId().toString().equals(event.getModalId())) {
                modal.getAction().onSubmit(event.getUser(), event);
                iterator.remove();
            }
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        for (DiscordMenu menu : menus) {
            if (menu.getInternalId().toString().equals(event.getComponentId())) {
                menu.getAction().onSelect(event.getUser(), event);
            }
        }
    }

    public void addButton(DiscordButton button) {
        buttons.add(button);
    }

    public void addModal(DiscordModal discordModal) {
        modals.add(discordModal);
    }

    public void addMenu(DiscordMenu discordMenu) {
        menus.add(discordMenu);
    }
}