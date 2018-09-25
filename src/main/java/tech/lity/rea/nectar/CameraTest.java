package tech.lity.rea.nectar;

import processing.core.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

/**
 *
 * @author Jeremy Laviole, <laviole@rea.lity.tech>
 */
@SuppressWarnings("serial")
public class CameraTest extends PApplet {

    Jedis redisSub, redisGet;
    String format;
//    int[] incomingPixels;
    PImage receivedPx;
    int widthStep = 0;

    @Override
    public void settings() {
        // the application will be rendered in full screen, and using a 3Dengine.
        size(640, 480, P3D);
    }

    @Override
    public void setup() {
        connectRedis();

        int w = 640, h = 480;
        widthStep = w * 3;

        try {
            w = Integer.parseInt(redisGet.get(input + ":width"));
            h = Integer.parseInt(redisGet.get(input + ":height"));
            widthStep = w;
            format = redisGet.get(input + ":pixelformat");
            widthStep = Integer.parseInt(redisGet.get(input + ":widthStep"));
        } catch (Exception e) {
            System.err.println("Cannot get image size, using 640x480.");
        }

        receivedPx = createImage(w, h, RGB);
        if (isUnique) {
            setImage(redisGet.get(input.getBytes()));
        } else {
//            noLoop();
            new RedisThread().start();
        }
    }

    class RedisThread extends Thread {

        public void run() {
            byte[] id = input.getBytes();
            // Subscribe tests
            MyListener l = new MyListener();
//        byte[] id = defaultName.getBytes();
            
            redisSub = createRedis();
            redisSub.subscribe(l, id);
        }
    }

    void connectRedis() {
        redisGet = createRedis();
        // redis.auth("156;2Asatu:AUI?S2T51235AUEAIU");
    }
    
    Jedis createRedis() {
        return new Jedis(host, port);
    }

    @Override
    public void draw() {
        background(255);
        image(receivedPx, 0, 0, width, height);
    }

    private void updateImage() {
        setImage(redisGet.get(input.getBytes()));
        log("Image updated", "");
    }

    public void keyPressed() {
       // updateImage();
    }

    public void setImage(byte[] message) {

        receivedPx.loadPixels();
        int[] px = receivedPx.pixels;

        if (message == null || message.length != px.length * 3) {
            if (message == null) {
                die("Cannot get the image in redis.");
            } else {
                die("Cannot get the image: or size mismatch.");
            }
        }
        byte[] incomingImg = message;

        int w = widthStep;
        byte[] lineArray = new byte[w];
        int k = 0;

        int skip = 0;
        int sk = 0;
        if (this.widthStep != receivedPx.width) {
            skip = widthStep - (receivedPx.width * 3);
        }
//        System.out.println("Widthstep "  + widthStep + " w " + receivedPx.width + " Skip: " + skip);

        if (format != null && format.equals("BGR")) {
            for (int i = 0; i < message.length / 3; i++) {

                if (k >= message.length - 3) {
                    break;
                }

                byte b = incomingImg[k++];
                byte g = incomingImg[k++];
                byte r = incomingImg[k++];
                px[i] = (r & 255) << 16 | (g & 255) << 8 | (b & 255);

                sk += 3;
                if (sk == receivedPx.width * 3) {
                    k += skip;
                    sk = 0;
                }
            }

        } else {
            for (int i = 0; i < message.length / 3; i++) {

                if (k >= message.length - 3) {
                    break;
                }

                byte r = incomingImg[k++];
                byte g = incomingImg[k++];
                byte b = incomingImg[k++];
                px[i] = (r & 255) << 16 | (g & 255) << 8 | (b & 255);

                sk += 3;
                if (sk == receivedPx.width * 3) {
                    k += skip;
                    sk = 0;
                }

            }
        }

        receivedPx.updatePixels();
    }

    class MyListener extends BinaryJedisPubSub {

        @Override
        public void onMessage(byte[] channel, byte[] message) {
            updateImage();
        }

        @Override
        public void onSubscribe(byte[] channel, int subscribedChannels) {
        }

        @Override
        public void onUnsubscribe(byte[] channel, int subscribedChannels) {
        }

        @Override
        public void onPSubscribe(byte[] pattern, int subscribedChannels) {
        }

        @Override
        public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
        }

        @Override
        public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
        }
    }

    static String defaultName = "camera";
    static Options options = new Options();

    public static final int REDIS_PORT = 6379;
    public static final String REDIS_HOST = "localhost";
    static private String input = "marker";
    static private String host = REDIS_HOST;
    static private int port = REDIS_PORT;
    static private boolean isUnique = false;

    static boolean isVerbose = false;
    static boolean isSilent = false;

    /**
     * @param passedArgs the command line arguments
     */
    static public void main(String[] passedArgs) {
        checkArguments(passedArgs);

        String[] appletArgs = new String[]{tech.lity.rea.nectar.CameraTest.class.getName()};
        PApplet.main(appletArgs);
    }

    private static void checkArguments(String[] passedArgs) {
        options = new Options();

//        public static Camera createCamera(Camera.Type type, String description, String format)
//        options.addRequiredOption("i", "input", true, "Input key of marker locations.");
        // Generic options
        options.addOption("h", "help", false, "print this help.");
        options.addOption("v", "verbose", false, "Verbose activated.");
        options.addOption("s", "silent", false, "Silent activated.");
        options.addOption("u", "unique", false, "Unique mode, run only once and use get/set instead of pub/sub");
        options.addRequiredOption("i", "input", true, "Inpput key for the image.");
        options.addOption("rp", "redisport", true, "Redis port, default is: " + REDIS_PORT);
        options.addOption("rh", "redishost", true, "Redis host, default is: " + REDIS_HOST);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;

        // -u -i markers -cc data/calibration-AstraS-rgb.yaml -mc data/A4-default.svg -o pose
        try {
            cmd = parser.parse(options, passedArgs);

            if (cmd.hasOption("i")) {
                input = cmd.getOptionValue("i");
            }

            if (cmd.hasOption("h")) {
                die("", true);
            }

            if (cmd.hasOption("u")) {
                isUnique = true;
            }
            if (cmd.hasOption("v")) {
                isVerbose = true;
            }
            if (cmd.hasOption("s")) {
                isSilent = true;
            }
            if (cmd.hasOption("rh")) {
                host = cmd.getOptionValue("rh");
            }
            if (cmd.hasOption("rp")) {
                port = Integer.parseInt(cmd.getOptionValue("rp"));
            }
        } catch (ParseException ex) {
            die(ex.toString(), true);
        }

    }

    public static void die(String why, boolean usage) {
        if (usage) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("CameraTest", options);
        }
        System.out.println(why);
        System.exit(-1);
    }

    public static void log(String normal, String verbose) {
        if (isSilent) {
            return;
        }
        if (normal != null) {
            System.out.println(normal);
        }
        if (isVerbose && verbose != null) {
            System.out.println(verbose);
        }
    }

}
