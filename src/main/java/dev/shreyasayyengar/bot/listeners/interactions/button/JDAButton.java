package dev.shreyasayyengar.bot.listeners.interactions.button;

import dev.shreyasayyengar.bot.DiscordBot;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.UUID;

public class JDAButton {

    private final UUID internalId = UUID.randomUUID();
    private final ButtonClickAction action;
    private final ButtonStyle style;
    private final String label;

    private int expiration = 60;
    private Button button;
    private Emoji emoji;

    public static JDAButton of(ButtonStyle style, String label, ButtonClickAction action) {
        return new JDAButton(style, label, action);
    }

    public static JDAButton of(ButtonStyle style, String label, String formattedEmoji, ButtonClickAction action) {
        return new JDAButton(style, label, formattedEmoji, action);
    }

    public static JDAButton of(ButtonStyle style, String label, String formattedEmoji, int expiration, ButtonClickAction action) {
        return new JDAButton(style, label, formattedEmoji, expiration, action);
    }

    private JDAButton(ButtonStyle style, String label, ButtonClickAction action) {
        this.style = style;
        this.label = label;
        this.button = Button.of(style, internalId.toString(), label);
        this.action = action;

        DiscordBot.get().getButtonManager().add(this);
    }

    private JDAButton(ButtonStyle style, String label, String formattedEmoji, ButtonClickAction action) {
        this.style = style;
        this.label = label;
        this.emoji = Emoji.fromFormatted(formattedEmoji);
        this.action = action;

        this.button = Button.of(style, internalId.toString(), label, emoji);

        DiscordBot.get().getButtonManager().add(this);
    }

    private JDAButton(ButtonStyle style, String label, String formattedEmoji, int expiration, ButtonClickAction action) {
        this.style = style;
        this.label = label;
        this.emoji = Emoji.fromFormatted(formattedEmoji);
        this.action = action;
        this.expiration = expiration;

        this.button = Button.of(style, internalId.toString(), label, emoji);

        DiscordBot.get().getButtonManager().add(this);
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

    public int getExpiration() {
        return expiration;
    }

    public void decrementExpiration() {
        expiration--;
    }
}