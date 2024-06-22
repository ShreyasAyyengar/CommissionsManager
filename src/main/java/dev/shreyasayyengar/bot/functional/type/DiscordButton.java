package dev.shreyasayyengar.bot.functional.type;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.functional.action.ButtonClickAction;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.UUID;

public class DiscordButton {

    private final UUID internalId = UUID.randomUUID();
    private final ButtonClickAction action;
    private final ButtonStyle style;
    private final String label;

    private final Button button;
    private Emoji emoji;

    public DiscordButton(ButtonStyle style, String label, ButtonClickAction action) {
        this.style = style;
        this.label = label;
        this.action = action;
        this.button = Button.of(style, internalId.toString(), label);

        DiscordBot.get().getInteractionManager().addButton(this);
    }

    public DiscordButton(ButtonStyle style, String label, String formattedEmoji, ButtonClickAction action) {
        this.style = style;
        this.label = label;
        this.emoji = Emoji.fromFormatted(formattedEmoji);
        this.action = action;
        this.button = Button.of(style, internalId.toString(), label, emoji);

        DiscordBot.get().getInteractionManager().addButton(this);
    }

    public DiscordButton(String label, String link) {
        this.style = ButtonStyle.LINK;
        this.label = label;
        this.action = (buttonUser, buttonInteractionEvent) -> {};
        this.button = Button.link(link, label);

        DiscordBot.get().getInteractionManager().addButton(this);
    }

    public UUID getInternalId() {
        return internalId;
    }

    public ButtonClickAction getAction() {
        return action;
    }

    public Button asButton() {
        return button;
    }
}
