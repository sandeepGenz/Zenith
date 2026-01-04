package samples.liveaudio;

import com.google.adk.agents.LiveRequestQueue;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.InMemorySessionService;
import com.google.common.collect.ImmutableList;
import com.google.genai.types.Blob;
import com.google.genai.types.Modality;
import com.google.genai.types.PrebuiltVoiceConfig;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.SpeechConfig;
import com.google.genai.types.VoiceConfig;
import io.reactivex.rxjava3.core.Flowable;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public final class ZenithRun {

  private final String userId;
  private final String sessionId;
  private final Runner runner;
  


  private static final javax.sound.sampled.AudioFormat MIC_AUDIO_FORMAT = 
      new javax.sound.sampled.AudioFormat(16000.0f, 16, 1, true, false);

  private static final javax.sound.sampled.AudioFormat SPEAKER_AUDIO_FORMAT = 
      new javax.sound.sampled.AudioFormat(24000.0f, 16, 1, true, false);
      
  private static final int BUFFER_SIZE = 4096;

  public ZenithRun() {
    this.userId = "test_user";
    String appName = "ZenithApp";
    this.sessionId = UUID.randomUUID().toString();
    InMemorySessionService sessionService = new InMemorySessionService();

    this.runner = new Runner(ZenithAgent.ROOT_AGENT, appName, null, sessionService);
    ConcurrentMap<String, Object> initialState = new ConcurrentHashMap<>();
    var unused = sessionService.createSession(appName, userId, initialState, sessionId).blockingGet();
  }

  private void runConversation() throws Exception {
    System.out.println("--------------------------------------------------");
    System.out.println("   Zenith - Peak Performance Interview Coach");
    System.out.println("--------------------------------------------------");
    System.out.println("Initializing audio devices...");

    RunConfig runConfig = RunConfig.builder()
        .setStreamingMode(RunConfig.StreamingMode.BIDI)
        .setResponseModalities(ImmutableList.of(new Modality("AUDIO")))
        .setSpeechConfig(
            SpeechConfig.builder()
                .voiceConfig(
                    VoiceConfig.builder()
                        .prebuiltVoiceConfig(
                            PrebuiltVoiceConfig.builder().voiceName("Aoede").build())
                        .build())
                .languageCode("en-US")
                .build())
        .build();

    LiveRequestQueue liveRequestQueue = new LiveRequestQueue();
    

    Flowable<Event> eventStream = this.runner.runLive(
        runner.sessionService().createSession(userId, sessionId).blockingGet(),
        liveRequestQueue,
        runConfig);

    AtomicBoolean isRunning = new AtomicBoolean(true);
    AtomicBoolean conversationEnded = new AtomicBoolean(false);
    ExecutorService executorService = Executors.newFixedThreadPool(2);


    Future<?> microphoneTask = executorService.submit(() -> captureAndSendMicrophoneAudio(liveRequestQueue, isRunning));


    Future<?> outputTask = executorService.submit(
        () -> {
          try {
            processAudioOutput(eventStream, isRunning, conversationEnded);
          } catch (Exception e) {
            System.err.println("Error processing audio output: " + e.getMessage());
            e.printStackTrace();
            isRunning.set(false);
          }
        });

    System.out.println("\n>>> Zenith is listening. Press ENTER to stop the session.\n");
    System.in.read();
    
    System.out.println("Ending session...");
    isRunning.set(false);

    try {

      microphoneTask.get(2, TimeUnit.SECONDS);
      outputTask.get(2, TimeUnit.SECONDS);
    } catch (Exception e) {
      System.out.println("Forcing shutdown of tasks...");
    }

    liveRequestQueue.close();
    executorService.shutdownNow();
    System.out.println("Session ended.");
  }

  private void captureAndSendMicrophoneAudio(LiveRequestQueue liveRequestQueue, AtomicBoolean isRunning) {
    TargetDataLine micLine = null;
    try {
      DataLine.Info info = new DataLine.Info(TargetDataLine.class, MIC_AUDIO_FORMAT);
      if (!AudioSystem.isLineSupported(info)) {
        System.err.println("Microphone line not supported!");
        return;
      }
      micLine = (TargetDataLine) AudioSystem.getLine(info);
      micLine.open(MIC_AUDIO_FORMAT);
      micLine.start();
      
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;
      
      while (isRunning.get()) {
        if (micLine.available() > 0) {
             bytesRead = micLine.read(buffer, 0, buffer.length);
             if (bytesRead > 0) {
               byte[] audioChunk = new byte[bytesRead];
               System.arraycopy(buffer, 0, audioChunk, 0, bytesRead);
               Blob audioBlob = Blob.builder().data(audioChunk).mimeType("audio/pcm").build();
               liveRequestQueue.realtime(audioBlob);
             }
        } else {
             try { Thread.sleep(10); } catch (InterruptedException ie) {} 
        }
    } catch (Exception e) {
      System.err.println("Error accessing microphone: " + e.getMessage());
      e.printStackTrace();
    } finally {
      if (micLine != null) {
        micLine.stop();
        micLine.close();
      }
    }
  }

  private void processAudioOutput(Flowable<Event> eventStream, AtomicBoolean isRunning, AtomicBoolean conversationEnded) {
    SourceDataLine speakerLine = null;
    try {
      DataLine.Info info = new DataLine.Info(SourceDataLine.class, SPEAKER_AUDIO_FORMAT);
      if (!AudioSystem.isLineSupported(info)) {
        System.err.println("Speaker line not supported!");
        return;
      }
      final SourceDataLine finalSpeakerLine = (SourceDataLine) AudioSystem.getLine(info);
      finalSpeakerLine.open(SPEAKER_AUDIO_FORMAT);
      finalSpeakerLine.start();
      
      speakerLine = finalSpeakerLine;


      for (Event event : eventStream.blockingIterable()) {
        if (!isRunning.get()) {
          break;
        }
        

        event.content().ifPresent(content -> 
            content.parts().ifPresent(parts -> 
                parts.forEach(part -> {
                    part.text().ifPresent(text -> System.out.println("Zenith (Text): " + text));
                })
            )
        );


        event.content().ifPresent(content -> 
            content.parts().ifPresent(parts -> 
                parts.forEach(part -> playAudioData(part, finalSpeakerLine))
            )
        );
      }
    } catch (LineUnavailableException e) {
      System.err.println("Error accessing speaker: " + e.getMessage());
      e.printStackTrace();
    } finally {
      if (speakerLine != null) {
        speakerLine.drain();
        speakerLine.stop();
        speakerLine.close();
      }
      conversationEnded.set(true);
    }
  }

  private void playAudioData(Part part, SourceDataLine speakerLine) {
    part.inlineData()
        .ifPresent(
            inlineBlob ->
                inlineBlob
                    .data()
                    .ifPresent(
                        audioBytes -> {
                          if (audioBytes.length > 0) {
                            
                            speakerLine.write(audioBytes, 0, audioBytes.length);
                          }
                        }));
  }

  public static void main(String[] args) throws Exception {
    ZenithRun zenithRun = new ZenithRun();
    zenithRun.runConversation();
  }
}
