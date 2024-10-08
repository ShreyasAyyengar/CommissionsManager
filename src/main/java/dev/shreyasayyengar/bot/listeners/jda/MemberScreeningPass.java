package dev.shreyasayyengar.bot.listeners.jda;

import dev.shreyasayyengar.bot.customer.Customer;
import dev.shreyasayyengar.bot.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class MemberScreeningPass extends ListenerAdapter {

    @Override
    public void onGuildMemberUpdatePending(@NotNull GuildMemberUpdatePendingEvent event) {
        if (event.getNewPending()) return;

        Member member = event.getMember();
        Guild guild = event.getGuild();

        if (!Util.isThisGuild(guild)) return;

        setupMember(member);
    }

    private void setupMember(Member member) {
        member.getGuild().addRoleToMember(member, member.getGuild().getRoleById("979538113816838174")).complete();
        sendWelcomeMessage(member);

        Customer customer = new Customer(member);

        String textChanelMention = customer.getTextChannel().getAsMention();

        MessageEmbed embed = new EmbedBuilder()
                .setDescription("Welcome to the server, " + member.getAsMention() + "!")
                .addField("How to get started:", "If you would like to **request a plugin commission**, you can run the command </request:979936705425592361>, and fill in the details that follows.\n", false)
                .addField("Text & Voice Channels", "This is your private channel! This text channel (" + textChanelMention + ") can be used at any time, and will remain here until you leave the server." +
                        " A voice channel can be generated by using </voice:1268740916747698289> \n\nFeel free to use them however you'd like. Do not hesitate to ping <@690755476555563019> whenever necessary.", false)
                .setColor(Util.THEME_COLOUR)
                .setThumbnail(member.getUser().getAvatarUrl())
                .setFooter("--CommissionsManager--")
                .setTimestamp(new Date().toInstant())
                .build();

        customer.getTextChannel().sendMessage(customer.getUser().getAsMention()).queue(message -> message.delete().queue());
        customer.getTextChannel().sendMessageEmbeds(embed).queue();
    }

    private void sendWelcomeMessage(Member member) {
        TextChannel welcomeChannel = member.getGuild().getTextChannelById("979537874003312731");
        assert welcomeChannel != null;

        MessageEmbed embed = new EmbedBuilder()
                .setAuthor(" - Welcome to shreyasayyengar.dev!", null, member.getUser().getEffectiveAvatarUrl())
                .setDescription(":wave: Welcome to the server, " + member.getAsMention() + "! :wave:")
                .addField("How to get started:", """
                        Private channels have been created for you! Further information can be found there





                         If you have any other questions please do not hesitate to ping <@690755476555563019> whenever necessary.""", true)
                .addField("I'll just look around for now...", """
                        Still need some time to figure out what this is? No problem; have a look at:
                        - <#979537532347908106>
                        - <#979537736224636928>
                        - <#979537813903142973>
                        - <#979781353572814968>""", true)
                .setColor(Util.THEME_COLOUR)
                .setFooter("--CommissionsManager--")
                .setTimestamp(new Date().toInstant())
                .setThumbnail(member.getEffectiveAvatarUrl())
                .build();

        welcomeChannel.sendMessageEmbeds(embed).queue();
    }
}