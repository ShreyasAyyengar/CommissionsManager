package dev.shreyasayyengar.bot.functional.type;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.functional.action.ModalSubmitAction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class DiscordModal {
    private final UUID internalId = UUID.randomUUID();

    private final String title;
    private final ArrayList<TextInput> inputs = new ArrayList<>();
    private ModalSubmitAction action;

    public DiscordModal(String title) {
        this.title = title;

        DiscordBot.get().getInteractionManager().addModal(this);
    }

    public DiscordModal addTextInput(TextInput... boxes) {
        inputs.addAll(Arrays.asList(boxes));
        return this;
    }

    public DiscordModal onSubmit(ModalSubmitAction action) {
        this.action = action;
        return this;
    }

    public UUID getInternalId() {
        return internalId;
    }

    public ModalSubmitAction getAction() {
        return action;
    }

    public Modal asModal() {
        Modal.Builder builder = Modal.create(internalId.toString(), title);
        for (TextInput input : inputs) builder.addComponents(ActionRow.of(input));
        return builder.build();
    }
}

