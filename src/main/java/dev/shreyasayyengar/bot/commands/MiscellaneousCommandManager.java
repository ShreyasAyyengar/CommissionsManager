package dev.shreyasayyengar.bot.commands;

import dev.shreyasayyengar.bot.DiscordBot;
import dev.shreyasayyengar.bot.paypal.Invoice;
import dev.shreyasayyengar.bot.utils.Authentication;
import dev.shreyasayyengar.bot.utils.EmbedUtil;
import dev.shreyasayyengar.bot.utils.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@SuppressWarnings("all")
public class MiscellaneousCommandManager extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        try {
            if (event.getAuthor().getId().equalsIgnoreCase(Authentication.OWNER_ID.get())) {
                String message = event.getMessage().getContentRaw();

                if (message.equalsIgnoreCase("!about")) {

                    File file = new File(getClass().getClassLoader().getResource("imgs/shreyasayyengar.png").toURI());

                    MessageEmbed aboutEmbed = new EmbedBuilder()
                            .setTitle("Welcome to shreyasayyengar.dev!")
                            .setDescription("The **#1** place for **all** your plugin development needs & services!")
                            .setThumbnail(event.getGuild().getMemberById(Authentication.OWNER_ID.get()).getEffectiveAvatarUrl())
                            .addField("Who?", "Hi! I'm `Shreyas Ayyengar` and I am a high school student and a Java Developer! I specialise in creating Bukkit plugins for Minecraft Servers of all types and sizes. " +
                                    "I also do some private bot development on the side when I feel like it :D ", false)

                            .addField("What?", "I have worked with over **30 different clients** at the time of writing, aiming to create __sustainable, cheap, and customisable__ Bukkit plugins." +
                                    " Working with this many people has made me very accessible and skillful at what I do. " +
                                    " Back when I started in `2020`, I was quite new, and still navigating my programming journey, and while that's still true, I am extremely more experienced " +
                                    "than I was years back!", false)

                            .addField("Why?", "I am simply here to create your vision with Minecraft plugins! Doing this helps me grow my workspace and experience. " +
                                    "I am also in the process of creating my own portfolio (with testimonials) about my services.", false)

                            .addField("Where?", "To get started have a look through the channels on the side, but more importantly, you will find " +
                                    "your own **private** channels created, **made just for you and I**!", false)

                            .addField("Why should I trust you?", """
                                    It is hard to find developers who are stick to their word, and deliver as promised and I completely understand that! No matter **who** I am working with, or **what** I am working on, I can promise you a friendly, open, and communicative experience on my end.\s

                                    As for the finished product? Just have a look at <#980373571807367208>!""", false)

                            .addField("Extra information:", """
                                        - [**My SpigotMC Developer Page**](https://www.spigotmc.org/threads/open-%E2%9C%A8-high-quality-plugins-configurable-affordable-easy-experienced-%E2%9C%A8.513897/)
                                        - [**My GitHub**](https://github.com/ShreyasAyyengar)
                                        - [**My Personal Website**](https://shreyasayyengar.dev/)
                                    """, false)
                            .setColor(Util.THEME_COLOUR)
                            .setThumbnail("attachment://shreyasayyengar.png")
                            .setFooter("Shreyas Ayyengar", event.getGuild().getMemberById(Authentication.OWNER_ID.get()).getEffectiveAvatarUrl())
                            .build();

                    event.getChannel().sendMessageEmbeds(aboutEmbed).addFiles(FileUpload.fromData(file, "shreyasayyengar.png")).queue();

                }

                if (message.equalsIgnoreCase("!rules")) {

                    MessageEmbed rulesEmbed = new EmbedBuilder()
                            .setTitle("Rules!!!!!")
                            .addField("Where's the rules?", """
                                            Unlike most discord servers, I won't create a list of rules and what's bannable or what's not bannable etc.\s

                                            The general consensus that I ask you to agree to is that your actions must not hurt me, hurt yourself, or hurt anyone else!. Anything that is obviously construed as **offensive**, **hurtful**, **disrespectful**, or otherwise is not allowed. If these acts are committed several times without an behaviour adjustment from a warning, you **will** be removed from this discord, and possibly cut loose from any commissions :(

                                            Creating a list of rules (||that I myself may violate||) just isn't really feasible, so all I ask if that you be a compassionate and respectful person and I **promise** you will have an amazing time here! :purple_heart:

                                            If you have any questions! Feel free to ping me in your private channel"""
                                    , false)
                            .setFooter("- Shreyas A.", event.getGuild().getMemberById(Authentication.OWNER_ID.get()).getEffectiveAvatarUrl())
                            .setColor(Util.THEME_COLOUR)
                            .build();

                    event.getChannel().sendMessageEmbeds(rulesEmbed).queue();
                }

                if (message.equalsIgnoreCase("!special")) {

                    MessageEmbed specialThanks = new EmbedBuilder()
                            .setTitle("Special Thanks")
                            .setDescription("**I would like to thank the following people for their contributions to this discord server, and overall for making this possible!**")
                            .addField("Special Thanks", """ 
                                    <@414314531683303426> - For helping me relentlessly and endlessly with testing and bug fixing my <@979533208012087296> Discord bot.
                                                                        
                                    <@477288891351695381> - Being one of the first people to beta test my discord server and the live version of my <@979533208012087296> Discord bot!
                                                                        
                                    <@715022019284172890> - For helping with PayPal invoice management, and testing out the new features.
                                                                        
                                    And last but not least: You guys! (<@&979538113816838174>). Thank you for using my plugin development services, and growing my community, little by little!
                                                                        
                                    A lot of the features, development, and general server flow would not be possible without some very cool people! So thank you all for being awesome!
                                    """, false)
                            .setFooter("- Shreyas A.", event.getGuild().getMemberById(Authentication.OWNER_ID.get()).getEffectiveAvatarUrl())
                            .setColor(Util.THEME_COLOUR)
                            .build();

                    event.getChannel().sendMessageEmbeds(specialThanks).queue();
                }

                if (message.equalsIgnoreCase("!mismatch")) {
                    for (var member : DiscordBot.get().workingGuild.getMembers()) {
                        if (DiscordBot.get().getCustomerManger().get(member.getId()) == null) {
                            event.getChannel().sendMessage("Mismatched member: " + member.getUser().getAsTag()).queue();
                        }
                    }
                }

                if (message.contains("thank you darling")) {
                    event.getMessage().reply("You're most welcome! :blush: :heart:").queue();
                }

                if (message.contains("!notwork")) {
                    event.getMessage().replyEmbeds(EmbedUtil.doesNotWork()).queue();
                }

                if (message.contains("!filestatus")) {
                    File templateFile = new File("invoice_template.yml");
                    InputStream inputStream = DiscordBot.get().getClass().getResourceAsStream("/invoice_template.yml");
                    FileOutputStream fileOutput = new FileOutputStream(templateFile);
                    inputStream.transferTo(fileOutput);

                    if (templateFile.exists()) {
                        event.getMessage().reply("File exists! (with slash)").queue();
                    } else {
                        event.getMessage().reply("File does not exist! (with slash)").queue();
                    }
                    if (inputStream != null) {
                        event.getMessage().reply("Input stream exists! (with slash)").queue();
                    } else {
                        event.getMessage().reply("Input stream does not exist! (with slash)").queue();
                    }

                    event.getMessage().reply("Done?").queue();
                }

                if (message.contains("!invoice-count")) {
                    event.getMessage().reply("There are %s invoices in cycle to check.".formatted(Invoice.INVOICES.size())).queue();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}