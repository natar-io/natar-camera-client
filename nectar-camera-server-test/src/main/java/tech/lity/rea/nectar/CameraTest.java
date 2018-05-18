package tech.lity.rea.nectar;

import processing.core.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

/**
 *
 * @author Jeremy Laviole, <laviole@rea.lity.tech>
 */
@SuppressWarnings("serial")
public class CameraTest extends PApplet {

    Jedis redis;

    @Override
    public void settings() {
        // the application will be rendered in full screen, and using a 3Dengine.
        size(640, 480, P3D);
    }

    @Override
    public void setup() {
        connectRedist();
        noLoop();

//        byte[] id = defaultName.getBytes();
//        // Subscribe tests
//        MyListener l = new MyListener();
////        byte[] id = defaultName.getBytes();
//        redis.subscribe(l, id);
        new RedisThread().start();
    }

    class RedisThread extends Thread {

        public void run() {
            byte[] id = defaultName.getBytes();
            // Subscribe tests
            MyListener l = new MyListener();
//        byte[] id = defaultName.getBytes();
            redis.subscribe(l, id);

        }
    }

    void connectRedist() {
        redis = new Jedis("127.0.0.1", 6379);
        // redis.auth("156;2Asatu:AUI?S2T51235AUEAIU");
    }

    byte[] imageBytes;

    @Override
    public void draw() {

        background(255);
        loadPixels();
        String name = defaultName;
//            byte[] id = {(byte) 0};

        if (imageBytes == null) {
            return;
        }
        // Get instead of subscribe
//        byte[] imgBytes = redis.get(id);
//        ByteArrayInputStream bis = new ByteArrayInputStream(imgBytes);
        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            Object o = in.readObject();
            byte[] px = (byte[]) o;

            println("Read " + px.length);
            println("this px image " + pixels.length);
            
            byteToInt(px, true, pixels);

//for(int i = 0; i < 10000; i++){
            //pixels[i] = px[i];
            // print(px[i] + " " );
//}
        } catch (IOException ex) {
            println("unpack issue " + ex);
        } catch (Exception ex) {
            println("unpack issue2 " + ex);
            ex.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                println("Reading issue");
                // ignore close exception
            }
        }
        updatePixels();

    }

    public void byteToInt(byte[] incomingImg, boolean RGB, int[] outputImg) {
        //        conversionCount++;
        //        if (conversionCount % 600 == 0) {
        //            System.gc();
        //        }
        assert (incomingImg.length == 3 * outputImg.length);

        // WidthStep to take into account ?!!
        int k = 0;
        for (int j = 0; j < outputImg.length; j++) {
            byte b = incomingImg[k++];
            byte g = incomingImg[k++];
            byte r = incomingImg[k++];
            outputImg[j] = (r & 255) << 16 | (g & 255) << 8 | (b & 255);
        }
    }

    class MyListener extends BinaryJedisPubSub {

        @Override
        public void onMessage(byte[] channel, byte[] message) {
            System.out.println("Message received ");
            imageBytes = message;
            redraw();
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
//
//    class MyListener extends JedisPubSub {
//
//        public void onMessage(String channel, String message) {
//        }
//
//        public void onSubscribe(String channel, int subscribedChannels) {
//        }
//
//        public void onUnsubscribe(String channel, int subscribedChannels) {
//        }
//
//        public void onPSubscribe(String pattern, int subscribedChannels) {
//        }
//
//        public void onPUnsubscribe(String pattern, int subscribedChannels) {
//        }
//
//        public void onPMessage(String pattern, String channel, String message) {
//        }
//    }

    public byte[] intToBytes(int my_int) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeInt(my_int);
        out.close();
        byte[] int_bytes = bos.toByteArray();
        bos.close();
        return int_bytes;
    }

    public int bytesToInt(byte[] int_bytes) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(int_bytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        int my_int = ois.readInt();
        ois.close();
        return my_int;
    }

    // TODO: add hostname ?
    public static final String OUTPUT_PREFIX = "nectar:";
    public static final String OUTPUT_PREFIX2 = ":camera-server:camera";
    public static final String REDIS_PORT = "6379";

    static String defaultHost = "jiii-mi";
    static String defaultName = OUTPUT_PREFIX + defaultHost + OUTPUT_PREFIX2 + "#0";

    /**
     * @param passedArgs the command line arguments
     */
    static public void main(String[] passedArgs) {

        Options options = new Options();
//         options.addOption("i", "input", true, "Input line in Redis if any.");
        options.addOption("o", "output", true, "Output line in Redis if any, default is:" + defaultName);
        options.addOption("rp", "redisport", true, "Redis port, default is: " + REDIS_PORT);
        options.addOption("rh", "redishost", true, "Redis host, default is: 127.0.0.1");
        options.addOption("h", "host", true, "this computer's name.");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, passedArgs);

            if (cmd.hasOption("o")) {
                String output = cmd.getOptionValue("o");

                System.out.println("Output: " + output);
            } else {
                System.out.println("No output value"); // print the date
                System.out.println("Default output: " + defaultName); // print the date
            }

        } catch (ParseException ex) {
            Logger.getLogger(CameraTest.class.getName()).log(Level.SEVERE, null, ex);
        }

//        if (passedArgs != null) {
//            PApplet.main(concat(appletArgs, passedArgs));
//        } else {
        String[] appletArgs = new String[]{tech.lity.rea.nectar.CameraTest.class.getName()};
        PApplet.main(appletArgs);
//        }
    }
}
