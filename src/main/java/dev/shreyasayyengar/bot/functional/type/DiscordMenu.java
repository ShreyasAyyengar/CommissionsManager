package dev.shreyasayyengar.bot.functional.type;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.functional.action.MenuSelectAction;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DiscordMenu {
    private final UUID internalId = UUID.randomUUID();
    private final ArrayList<SelectOption> options = new ArrayList<>();

    private String placeholder;
    private int min, max;
    private MenuSelectAction action;

    public DiscordMenu() {
        DiscordBot.get().getInteractionManager().addMenu(this);
    }

    public DiscordMenu addOptions(SelectOption... options) {
        this.options.addAll(List.of(options));
        return this;
    }

    public DiscordMenu setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public DiscordMenu setRequiredRange(int min, int max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public DiscordMenu onSelect(MenuSelectAction action) {
        this.action = action;
        return this;
    }

    public UUID getInternalId() {
        return internalId;
    }

    public MenuSelectAction getAction() {
        return action;
    }

    public StringSelectMenu asMenu() {
        return StringSelectMenu.create(internalId.toString())
                .addOptions(options)
                .setPlaceholder(placeholder)
                .setRequiredRange(min, max)
                .build();
    }
}
