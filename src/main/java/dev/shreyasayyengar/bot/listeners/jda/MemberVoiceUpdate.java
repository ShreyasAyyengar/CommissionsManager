package dev.shreyasayyengar.bot.listeners.jda;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
public class MemberVoiceUpdate extends ListenerAdapter implements AudioReceiveHandler { // TODO

    private final List<byte[]> audioData = new ArrayList<>();

    // TODO: Disabled for now, will be re-enabled when I have time to work on it if ever.
//    @Override
//    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
//        if (event.getMember().getId().equalsIgnoreCase(Authentication.OWNER_ID.get())) {
//
//            if (Util.privateChannel(event.getChannelJoined())) return;
//
//            AudioManager audioManager = DiscordBot.get().workingGuild.getAudioManager();
//            audioManager.setReceivingHandler(this);
//
//            event.getGuild().getAudioManager().openAudioConnection(event.getChannelJoined());
//        }
//    }
//
//    @Override
//    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
//        if (event.getMember().getId().equalsIgnoreCase(Authentication.OWNER_ID.get())) {
//
//            if (Util.privateChannel(event.getChannelLeft())) return;
//
//            AudioManager audioManager = DiscordBot.get().workingGuild.getAudioManager();
//            audioManager.setReceivingHandler(null);
//
//            event.getGuild().getAudioManager().closeAudioConnection();
//
//            createRecording(event.getChannelLeft());
//        }
//    }

    @Override
    public boolean canReceiveCombined() {
        return true;
    }

    @Override
    public void handleCombinedAudio(@NotNull CombinedAudio combinedAudio) {
        audioData.add(combinedAudio.getAudioData(1.0));
    }

    private void createRecording(Channel channel) {
        try {
            int size = 0;
            for (byte[] bs : audioData) {
                size += bs.length;
            }
            byte[] decodedData = new byte[size];
            int i = 0;
            for (byte[] bs : audioData) {
                for (byte b : bs) {
                    decodedData[i++] = b;
                }
            }

            String presentableDate = new Date().toString().replace(" ", "_").replace(":", "-");

            // convert the data into a WAV file and send to hcannel
            File file = new File("/Users/ShreyasSrinivasAyyengar/JetBrains/IdeaProjects/CommissionsManager/src/main/resources/recordings/" + presentableDate + ".wav");
            getWavFile(file, decodedData);


//            File file = new File("/Users/ShreyasSrinivasAyyengar/JetBrains/IdeaProjects/CommissionsManager/src/main/resources/recordings/" + presentableDate + ".mp3");
//            getWavFile(file, decodedData);
//
//            Runtime.getRuntime().exec("ffmpeg -i " + file.getAbsolutePath() + " " + file.getAbsolutePath().replace(".wav", "-final.mp3"));
//
//            TextChannel textChannel = Util.getCustomerByChannelId(channel).getTextChannel();
//            textChannel.sendMessageEmbeds(EmbedUtil.recordingFinished()).queue();
//            textChannel.sendFiles(FileUpload.fromData(file)).queue();
//
//            file.delete();
//
//            audioData.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void getWavFile(File outFile, byte[] decodedData) throws IOException {
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(decodedData), AudioReceiveHandler.OUTPUT_FORMAT, decodedData.length), AudioFileFormat.Type.WAVE, outFile);
    }
}