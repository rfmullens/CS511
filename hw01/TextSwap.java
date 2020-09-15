import java.io.*;
import java.util.*;
//I pledge my honor that I have abided by the Stevens Honor System Ryan Mullens And Sun bachrach
public class TextSwap {

    private static String readFile(String filename) throws Exception {
        String line;
        StringBuilder buffer = new StringBuilder();
        File file = new File(filename);
        BufferedReader br = new BufferedReader(new FileReader(file));
        while ((line = br.readLine()) != null) {
            buffer.append(line);
        }
        br.close();
        return buffer.toString();
    }

    private static Interval[] getIntervals(int numChunks, int chunkSize) {
        Interval[] bufferarray = new Interval[numChunks]; //creates a new array of Intervals = to Interval[numChunks]
        for(int i = 0; i < numChunks; i++){
            Interval buffer = new Interval(i*chunkSize, (chunkSize*(i+1)-1));
            bufferarray[i] = buffer;
        }
        return bufferarray;
    }

    private static List<Character> getLabels(int numChunks) {
        Scanner scanner = new Scanner(System.in);
        List<Character> labels = new ArrayList<Character>();
        int endChar = numChunks == 0 ? 'a' : 'a' + numChunks - 1;
        System.out.printf("Input %d character(s) (\'%c\' - \'%c\') for the pattern.\n", numChunks, 'a', endChar);
        for (int i = 0; i < numChunks; i++) {
            labels.add(scanner.next().charAt(0));
        }
        scanner.close();
        // System.out.println(labels);
        return labels;
    }

    private static char[] runSwapper(String content, int chunkSize, int numChunks) {
        List<Character> labels = getLabels(numChunks);
        Interval[] intervals = getIntervals(numChunks, chunkSize);
        Interval[] newInterval = new Interval[numChunks];
        char[] newBuff = new char[chunkSize*numChunks];
        Thread[] threadList = new Thread[numChunks];

        for(int i = 0; i< labels.size(); i++){
            newInterval[i] = intervals[(labels.get(i)) - 'a'];
        }

        
        for(int j = 0; j < numChunks; j++){
            int offset = j *chunkSize;
            Thread newThread = new Thread(new Swapper(newInterval[j], content, newBuff, offset));
            threadList[j] = newThread;
            newThread.start();
        }
        for(int k = 0; k < threadList.length; k++){
            try{
                threadList[k].join();
            } catch(InterruptedException except){
                except.printStackTrace();
            }
        }
        return newBuff;
    }

    private static void writeToFile(String contents, int chunkSize, int numChunks) throws Exception {
        char[] buff = runSwapper(contents, chunkSize, contents.length() / chunkSize);
        PrintWriter writer = new PrintWriter("output.txt", "UTF-8");
        writer.print(buff);
        writer.close();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java TextSwap <chunk size> <filename>");
            return;
        }
        String contents = "";
        int chunkSize = Integer.parseInt(args[0]);

        try {
            contents = readFile(args[1]);
            writeToFile(contents, chunkSize, contents.length() / chunkSize);
        } catch (Exception e) {
            System.out.println("Error with IO.");
            return;
        }
    }
}