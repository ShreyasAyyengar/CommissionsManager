package dev.shreyasayyengar.bot.listeners.interactions.button;

import dev.shreyasayyengar.bot.DiscordBot;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ButtonActionManager extends ListenerAdapter {
    public final Set<JDAButton> activeButtons = new HashSet<>();

    public ButtonActionManager() {
        DiscordBot.get().bot().addEventListener(this);

        ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();
        scheduledService.scheduleAtFixedRate(() -> {

            for (JDAButton activeButton : activeButtons) {
                if (activeButton.getExpiration() == -1) continue;

                if (activeButton.getExpiration() <= 0) {
                    activeButtons.remove(activeButton);
                } else {
                    activeButton.decrementExpiration();
                }
            }

        }, 0, 1, TimeUnit.SECONDS);
    }

    public void add(JDAButton button) {
        activeButtons.add(button);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        for (JDAButton button : activeButtons) {
            if (button.getInternalId().toString().equals(event.getComponentId())) {
                button.getAction().onClick(event.getUser(), event);
                break;
            }
        }
    }
}