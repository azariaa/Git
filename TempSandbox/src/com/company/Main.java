package com.company;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AlgorithmParameters;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {

//    static class GMailAuthenticator extends Authenticator {
//        String user;
//        String pw;
//        public GMailAuthenticator (String username, String password)
//        {
//            super();
//            this.user = username;
//            this.pw = password;
//        }
//        public PasswordAuthentication getPasswordAuthentication()
//        {
//            return new PasswordAuthentication(user, pw);
//        }
//    }

    static private final String USER_AGENT = "Mozilla/5.0";

    public static void main(String[] args) throws Exception
    {
        String[] a = "hello wolrd 7".split(" ");
        Arrays

        String agentNameKeyword = "jessica";
        String bestResult = "is this JesSica or is it djessica?";
        String regex = "\\s*\\b"+agentNameKeyword+"\\b\\s*";
        String text = bestResult.replaceAll("(?i)"+regex, " ");
        System.out.println(text);

        //for (int i = 0; i < 1000; i++)
            //System.out.println(NameGenerator.generateNewName());

//        String email = "inmindenc@gmail.com";
//        //Pattern pattern = Pattern.compile("(^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$)", Pattern.CASE_INSENSITIVE);
//        Pattern p = Pattern.compile("(^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$)", Pattern.CASE_INSENSITIVE);
//        Matcher m = p.matcher(email);
//        System.out.println(m.matches());


//        String userId = "fdsadfgssdf3482ds98cvc98ew";
//        String first = IntStream.range(0, userId.length()).filter(i->(i%2)==0).mapToObj(i -> (Character.toString(userId.charAt(i)))).collect(Collectors.joining(""));
//        String second = IntStream.range(0,userId.length()).filter(i->(i%2)==1).mapToObj(i -> (Character.toString(userId.charAt(i)))).collect(Collectors.joining(""));
//
//        System.out.println(first + " " + second);



//        String originalPassword = "???";
//        StAndIV encryptedValue = encryptOrDecrypt(true, originalPassword, null);
//        System.out.println(encryptedValue.st);
//
//        StAndIV backToOrg = encryptOrDecrypt(false, encryptedValue.st, encryptedValue.iv);
//        System.out.println(backToOrg.st);

        //piazza();
        //Callable<Integer> r = () -> 7 + 3;
        //System.out.println(r.call());
        //r.run();

    }

    static class StAndIV
    {
        public String st;
        public String iv;
    }

    private static StAndIV encryptOrDecrypt(boolean enc, String encOrDec, String ivs) throws Exception
    {
        byte[] input;
        if (enc)
            input = encOrDec.getBytes(StandardCharsets.UTF_8);
        else
            input = new BASE64Decoder().decodeBuffer(encOrDec);
        char[] password = "4328s38".toCharArray();
        byte[] salt = "siel234n".getBytes(StandardCharsets.UTF_8);
        //byte[] salt = new byte[]{4,6,2,6,8,3,5,7};
        //SecureRandom.


        /* Derive the key, given password and salt. */
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, 65536, 128);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

        //SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
        //IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        if (enc)
        {
            cipher.init(Cipher.ENCRYPT_MODE, secret);//key, ivSpec);


            AlgorithmParameters params = cipher.getParameters();
            byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();

            byte[] encVal = cipher.doFinal(input);
            String encryptedValue = new BASE64Encoder().encode(encVal);
            StAndIV stAndIV = new StAndIV();
            stAndIV.st = encryptedValue;
            stAndIV.iv = new BASE64Encoder().encode(iv);
            return stAndIV;
        }
        else
        {
            byte[] iv =  new BASE64Decoder().decodeBuffer(ivs);

            cipher.init(Cipher.DECRYPT_MODE, secret,new IvParameterSpec(iv));

            String plaintext = new String(cipher.doFinal(input), "UTF-8");;
            StAndIV stAndIV = new StAndIV();
            stAndIV.st = plaintext;
            return stAndIV;
        }
    }

    private static void piazza() throws IOException
    {
        //login
        String url = "https://piazza.com/logic/api?method=user.login";

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //using post
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        String parameters =  "{\"method\":\"user.login\",\"params\":{\"email\":\"inmindenc@gmail.com\",\"pass\":\"CNEdnimni\"}}";

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(parameters);
        wr.flush();
        wr.close();
        int responseCode = con.getResponseCode();
        String cookie = con.getHeaderField("Set-Cookie");
        if (responseCode == 200)
        {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
            {
                System.out.println(inputLine + "\n");
            }
        }
        else
        {
            System.out.println("S: error. (response code is: " + responseCode + ")");
        }


        //post something!
        String postUrl = "https://piazza.com/logic/api?content.answer";//"https://piazza.com/logic/api?content.create";

        URL obj2 = new URL(postUrl);
        HttpURLConnection con2 = (HttpURLConnection) obj2.openConnection();

        //using post
        con2.setRequestMethod("POST");
        con2.setRequestProperty("User-Agent", USER_AGENT);
        con2.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con2.setRequestProperty("Cookie",cookie);

        String parameters2 =  "{\"method\":\"content.answer\",\"params\":{\"content\":\"automatically generated2\",\"type\":\"s_answer\",\"cid\":\"icp4mkg3ypu6tx\",\"revision\":0,\"anonymous\":\"no\"}}";//"{\"method\":\"content.create\",\"params\":{\"nid\":\"ic53erv8juk488\",\"type\":\"question\",\"subject\":\"Amos testing (automatically created)\",\"content\": \"nothing\",\"folder:\":\"other\"}}";

        // Send post request
        con2.setDoOutput(true);
        DataOutputStream wr2 = new DataOutputStream(con2.getOutputStream());
        wr2.writeBytes(parameters2);
        wr2.flush();
        wr2.close();
        int responseCode2 = con2.getResponseCode();
        if (responseCode2 == 200)
        {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con2.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
            {
                System.out.println(inputLine + "\n");
            }
        }
        else
        {
            System.out.println("S: error. (response code is: " + responseCode2 + ")");
        }
    }

    private static void mySqlConnection()
    {
        String url = "jdbc:mysql://localhost:3306/instructable_kb";
        String username = "root";
        String password = "InMind7";

        System.out.println("Connecting database...");

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setDatabaseName("instructable_kb");
        dataSource.setUser(username);
        dataSource.setPassword(password);
        dataSource.setPort(3306);
        dataSource.setServerName("localhost");

        try
        {
            Connection connection = dataSource.getConnection();//DriverManager.getConnection(url, username, password);
            System.out.println("Database connected!");
            Statement stmt = connection.createStatement();
            stmt.execute("insert into concepts (user_id,concept_name) values ('all','temp3')");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        }
    }

    public static void testEmail()
    {
        final String username = "amospam2@gmail.com";
        final String password = "Azaria12";
        final int emailsToFetch = 5;

        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        try {
            Session session = Session.getInstance(props, null);//new GMailAuthenticator(username,password));
            //session.setDebug(true);
            Store store = session.getStore();
            store.connect("imap.gmail.com", username, password);
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            for (int idx = inbox.getMessageCount() - emailsToFetch; idx < inbox.getMessageCount(); idx++)
            {
                System.out.println("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                System.out.println("EMAIL INDEX:" + idx);
                Message msg = inbox.getMessage(idx);
                Address[] in = msg.getFrom();
                for (Address address : in)
                {
                    System.out.println("FROM:" + address.toString());
                }
                String bodyStr = "Error!";
                Object msgContent = msg.getContent();
                if (msgContent instanceof String)
                    bodyStr = (String)msgContent;
                else if (msgContent instanceof Multipart)
                {
                    BodyPart bp = ((Multipart)msgContent).getBodyPart(0);
                    bodyStr = bp.getContent().toString();
                }
                System.out.println("SENT DATE:" + msg.getSentDate());
                System.out.println("SUBJECT:" + msg.getSubject());
                System.out.println("CONTENT:" + bodyStr);
            }
        } catch (Exception mex) {
            mex.printStackTrace();
        }
    }


//    public static void activem(String[] args) throws Exception {
//        thread(new HelloWorldProducer(), false);
////        thread(new HelloWorldProducer(), false);
//        Thread.sleep(1000);
//        thread(new HelloWorldConsumer(), false);
//
////        thread(new HelloWorldConsumer(), false);
////        thread(new HelloWorldProducer(), false);
////        thread(new HelloWorldConsumer(), false);
////        thread(new HelloWorldProducer(), false);
////        Thread.sleep(1000);
////        thread(new HelloWorldConsumer(), false);
////        thread(new HelloWorldProducer(), false);
////        thread(new HelloWorldConsumer(), false);
////        thread(new HelloWorldConsumer(), false);
////        thread(new HelloWorldProducer(), false);
////        thread(new HelloWorldProducer(), false);
////        Thread.sleep(1000);
////        thread(new HelloWorldProducer(), false);
////        thread(new HelloWorldConsumer(), false);
////        thread(new HelloWorldConsumer(), false);
////        thread(new HelloWorldProducer(), false);
////        thread(new HelloWorldConsumer(), false);
////        thread(new HelloWorldProducer(), false);
////        thread(new HelloWorldConsumer(), false);
////        thread(new HelloWorldProducer(), false);
////        thread(new HelloWorldConsumer(), false);
////        thread(new HelloWorldConsumer(), false);
////        thread(new HelloWorldProducer(), false);
//    }

    public static void thread(Runnable runnable, boolean daemon) {
        Thread brokerThread = new Thread(runnable);
        brokerThread.setDaemon(daemon);
        brokerThread.start();
    }

//    public static class HelloWorldProducer implements Runnable {
//        public void run() {
//            try {
//                // Create a ConnectionFactory
//                ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://128.2.210.187:61616");//"vm://localhost");
//
//                // Create a Connection
//                Connection connection = connectionFactory.createConnection();
//                connection.start();
//
//                // Create a Session
//                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//
//                // Create the destination (Topic or Queue)
//                Destination destination = session.createTopic("asrinput");//.createQueue("TEST.FOO");
//
//                // Create a MessageProducer from the Session to the Topic or Queue
//                MessageProducer producer = session.createProducer(destination);
//                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
//
//                // Create a messages
//                //String text = "Hello world! From: " + Thread.currentThread().getName() + " : " + this.hashCode();
//                String text = "fdkasjd543tre43efh^ASR^Hello world";
//                TextMessage message = session.createTextMessage(text);
//
//                // Tell the producer to send the message
//                System.out.println("Sent message: "+ text + ". " + message.hashCode() + " : " + Thread.currentThread().getName());
//                producer.send(message);
//
//                // Clean up
//                session.close();
//                connection.close();
//            }
//            catch (Exception e) {
//                System.out.println("Caught: " + e);
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public static class HelloWorldConsumer implements Runnable, ExceptionListener
//    {
//        public void run() {
//            try {
//
//                // Create a ConnectionFactory
//                ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://128.2.210.187:61616");
//
//                // Create a Connection
//                Connection connection = connectionFactory.createConnection();
//                connection.start();
//
//                connection.setExceptionListener(this);
//
//                // Create a Session
//                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//
//                // Create the destination (Topic or Queue)
//                Destination destination = session.createTopic("nlgoutput");
//
//                // Create a MessageConsumer from the Session to the Topic or Queue
//                MessageConsumer consumer = session.createConsumer(destination);
//
//                // Wait for a message
//                Message message = consumer.receive(1000);
//
//                if (message instanceof TextMessage) {
//                    TextMessage textMessage = (TextMessage) message;
//                    String text = textMessage.getText();
//                    System.out.println("Received: " + text);
//                } else {
//                    System.out.println("Received: " + message);
//                }
//
//                consumer.close();
//                session.close();
//                connection.close();
//            } catch (Exception e) {
//                System.out.println("Caught: " + e);
//                e.printStackTrace();
//            }
//        }
//
//        public synchronized void onException(JMSException ex) {
//            System.out.println("JMS Exception occured.  Shutting down client.");
//        }
//    }



//    private static void oldSphinx()
//    {
//        ConfigurationManager cm;
//
//
//        cm = new ConfigurationManager(Main.class.getResource("hellongram.config.xml"));
//
//        // allocate the recognizer
//        System.out.println("Loading...");
//        Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
//        recognizer.allocate();
//
//        recognizer.addResultListener(new ResultListener()
//        {
//            @Override
//            public void newResult(Result result)
//            {
//                System.out.println(result.getBestResultNoFiller());
//            }
//
//            @Override
//            public void newProperties(PropertySheet propertySheet) throws PropertyException
//            {
//
//            }
//        });
//
//        // start the microphone or exit if the programm if this is not possible
//        Microphone microphone = (Microphone) cm.lookup("microphone");
//        if (!microphone.startRecording()) {
//            System.out.println("Cannot start microphone.");
//            recognizer.deallocate();
//            System.exit(1);
//        }
//
//
//        // loop the recognition until the programm exits.
//        while (true) {
//            System.out.println("Start speaking. Press Ctrl-C to quit.\n");
//
//            //Thread.sleep(1000);
//
//
//            Result result = recognizer.recognize();
//        }
//    }

    private static void dontknow() throws IOException
    {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("c:\\InMind\\Git\\TempSandbox\\words.txt")));
        String line;
        StringBuilder allWords = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null)
        {
            allWords.append(line+"|");
        }


        String sAllwords = allWords.toString();
        String pat = ".{0,5}" + "("+sAllwords+")";
        long start = System.currentTimeMillis();
        Pattern p = Pattern.compile(pat);
        Matcher m = p.matcher("afdssazebra");
        boolean match = m.matches();
        long took = System.currentTimeMillis() - start;
        System.out.println("match: " + match + ", time: " + took + " millisec");

        //System.out.println(MicrosoftASR.callFromFile(Paths.get("C:\\InMind\\git\\UserData\\InputAt311214-124907.050.raw")));

//        String valStr="as+fjk- 5";
//        String tmpValStr = valStr.replaceAll("[!?,\\+\\-\\*/]", " ");
//        String[] allRefsToVariables = tmpValStr.split("[ ]+");
//
        //ScriptEngineManager mgr = new ScriptEngineManager();
        //ScriptEngine engine = mgr.getEngineByName("JavaScript");
        //String foo = "Math.floor(Math.random() * 6) + 1";
        ////Boolean t = (Boolean)engine.eval(foo);
        ////Boolean b = new Boolean(engine.eval(foo).toString());
        //System.out.println(engine.eval(foo));

        //isSilentButDidTalk("C:\\InMind\\git\\UserData\\InputAt231214-122558.982.raw");

        //reflection();
        //String format = (new SimpleDateFormat("File-ddMMyy-hhmmss.SSS.raw")).format(new Date());
        //regexify();
    }

    private static boolean isSilentButDidTalk(String fileName)
    {
        int silentLengthNeeded = 500;
        int sampleRate = 44100;
        int considerSilent = 1500;
        int considerSpeech = 3000;

        int silentSampleLength = 0;
        int talkSampleLength = 0;
        try
        {
            byte[] asByte = Files.readAllBytes(Paths.get(fileName));
            //short[] asShort = new short[asByte.length/2];
            for (int i = 0; 2*i < asByte.length; i++)
            {
                short sample = (short) (asByte[2*i+1] << 8 | asByte[2*i]); //little endian 16bit
                if (Math.abs(sample) < considerSilent)
                    silentSampleLength++;
                else
                {
                    silentSampleLength = 0;
                    if (Math.abs(sample) > considerSpeech)
                    {
                        talkSampleLength++;
                    }
                }
            }
            double silentLength = silentSampleLength/(double)sampleRate;
            if (silentLength*1000 > silentLengthNeeded && talkSampleLength > sampleRate / 100)
                return true;

        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;

    }

    private static void reflection()
    {
        Method method = null;
        try {
            Package pack = Main.class.getPackage();
            method = Class.forName(pack.getName() + "." + "Main").getMethod("regexify");
                method.invoke(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void regexify() {
        String inputText = "tell me exactly what I am happy to hear you say";
        List<String> response = new LinkedList<String>();


        URI filePath = URI.create("file:///C:/InMind/git/Configurations/logic.csv");
        String line = "";
        String cvsSplitBy = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(new File(filePath)));

            while ((line = bufferedReader.readLine()) != null) {

                // use comma as separator
                String[] row = line.split(cvsSplitBy, -1);
                Pattern p = Pattern.compile((row[0]), Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(inputText);
                if (m.matches()) {
                    row[1] = row[1].replace("\"\"", "\"");
                    row[1] = row[1].replaceAll("^\"|\"$", "");
                    for (int i = 1; ; i++) {
                        if (row[1].contains("%" + i)) {
                            row[1] = row[1].replace("%" + i, m.group(i));
                        } else if (row[1].contains("%r" + i)) //replace with reflection
                        {
                            row[1] = row[1].replace("%r" + i, reflect(m.group(i)));
                        } else
                            break;
                    }
                    response.add("Say^" + row[1]);
                    if (!row[2].isEmpty())
                        response.add(row[2]);
                    break;
                }

            }


        } catch (Exception ex) {
            System.out.println("Logic: error in response");
        }


        System.out.println(response);
    }

    private static String reflect(String text) {

        URI filePath = URI.create("file:///C:/InMind/git/Configurations/reflection.csv");
        String line = "";
        String cvsSplitBy = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
        BufferedReader bufferedReader = null;



        String alreadyReplaced = "~";

        try {
            bufferedReader = new BufferedReader(new FileReader(new File(filePath)));

            while ((line = bufferedReader.readLine()) != null) {

                // use comma as separator
                String[] row = line.split(cvsSplitBy, -1);
                String wordsToMatch = "(?i)"+row[0]; //ignore case
                String toMatch = "^"+wordsToMatch+" | "+wordsToMatch+" | "+wordsToMatch+"$|^"+wordsToMatch+"$";
                String excludeAlreadyReplaced = toMatch+"(?=([^"+alreadyReplaced+"]*"+alreadyReplaced+"[^"+alreadyReplaced+"]*"+alreadyReplaced+")*[^"+alreadyReplaced+"]*$)";

                text = text.replaceAll(excludeAlreadyReplaced,"~"+row[1]+"~");
            }
        } catch (Exception ex) {
            System.out.println("Logic: error in reflection");
            return text;
        }

        return text.replaceAll(alreadyReplaced," ").trim();

    }
}





