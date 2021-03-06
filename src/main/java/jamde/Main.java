
package jamde;

import jamde.table.*;
import java.io.*;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 *
 * @author kucerj28@fjfi.cvut.cz
 */
public class Main {
    
    private static final class InputArgs{
        private boolean tableClassicBool = false;
        private boolean tableRawBool = false;
        private boolean printDistanceFunctionsBool = false;
        private boolean sendEmailBool = false;

        private String inputFile = "none";
        private int numOfThreads = 25;
        private String outputFile = System.getProperty("user.home") + "/tables/default/defaultTable";
        
        
        public InputArgs() {
        }

        public boolean isTableClassicBool() {
            return tableClassicBool;
        }

        public boolean isTableRawBool() {
            return tableRawBool;
        }

        public boolean isPrintDistanceFunctionsBool() {
            return printDistanceFunctionsBool;
        }

        public boolean isSendEmailBool() {
            return sendEmailBool;
        }
        
        public String getInputFile() {
            return inputFile;
        }

        public int getNumOfThreads() {
            return numOfThreads;
        }

        public String getOutputFile() {
            return outputFile;
        }
       
        
        /**
         * Reads command-Line arguments and translates them into right format.
         * 
         * @param args
         * @return 
         */
        public int setInputArgs(String[] args) {
            if (args.length == 0) {
                System.out.println("Not enough arguments.");
                
            }
            int i = 0;
            while (i < args.length) {

                switch (args[i]) { // CAUTION!: switch on Strings is suported only by source 7, not by source 6.
                    case "infile":
                        inputFile = args[i + 1];
                        break;
                    case "outfile":
                        outputFile = args[i + 1];
                        break;
                    case "threads":
                        numOfThreads = Integer.parseInt(args[i + 1]);
                        break;
                    case "email":
                        sendEmailBool = true;
                        break;
                    case "print":
                        switch (args[i + 1]) {
                            case "raw":
                                tableRawBool = true;
                                break;
                            case "classic":
                                tableClassicBool = true;
                                break;
                            case "distances":
                                printDistanceFunctionsBool = true;
                            case "distance":
                                printDistanceFunctionsBool = true;
                        }
                }
                i = i + 2;
            }
            if (inputFile.equals("none")) {
                System.out.println("You did not specify name of the file, you want to load. Program is terminating.");
                return 1;
            }

            if (!(tableClassicBool || tableRawBool || printDistanceFunctionsBool)) {
                System.out.println("You have not specified ANY output.\nDo this by \"print raw\" or \"print classic\" or \"print distance\"\n Terminating. ");
                return 1;
            }
            
            if ( outputFile.equals(System.getProperty("user.home") + "/tables/default/defaultTable")){
                System.out.println("You have not specify name and path to the output file");
            }
            return 0;
        }
    }

    /**
     * Depending on the command line arguments starts the JAmde with appropriate input and parameters. 
     * 
     * Example: java -jar JAmde infile ./pathToFile/file threads 12 outfile ./pathToTable/table print raw print classic print distances
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException {
        
        InputArgs inputArgs = new InputArgs();
        
        if (inputArgs.setInputArgs(args)==1) {
            return;
        } 
        
        long timeStart = System.currentTimeMillis();


        Table table = new Table();
        File inputFile = new File(inputArgs.getInputFile());
        
        if (! inputFile.exists()) {
            System.out.println("File you chose does not exist. Program is terminating.");
            return;
        } else {
            try {
                table.loadInputsFromFile(inputFile);
                System.out.println("Table input was succesfully loaded from the file.");
            } catch (Exception ex) {
                System.out.println("File you chose does not contain what it should. Program is terminating.");
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } // Now we have loaded the tableInput in Table from the file
        
        
        int numOfThreads = inputArgs.getNumOfThreads();
        numOfThreads = Math.min(numOfThreads, 30); // Designed for 32-core vkstat. So it doesn't take up all the cores.
        
        // the enumeration itself
        int countOutput;
        countOutput =  table.count(numOfThreads);
        if (countOutput == 1){ // Only the distanceTable was printed to the file
          return;
        }
        
        // OUTPUT
        
        // List of *.tex files on which pdflatex will be called
        ArrayList<File> texFiles = new ArrayList<>();
        String tableFileName = inputArgs.getOutputFile();
        File tableFile = OtherUtils.MakeUniqueNamedFile(tableFileName);
        tableFile.mkdir();
        tableFileName =  tableFile.getAbsolutePath();
        
        // CLASSIC TABLE
        if (inputArgs.isTableClassicBool()) {
            File classicTableFile = new File(tableFileName.concat(File.separator + "TableClassic.tex"));
            String classicTableFileName =  classicTableFile.getAbsolutePath();

            ClassicTable classicTable = new ClassicTable(table);
            classicTable.printClassic(classicTableFileName);
            System.out.println("Result is saved in " + classicTableFileName);
            
            texFiles.add(classicTableFile);            
        }
        
        // RAW TABLE || DISTANCE FUNCTIONS
        if (inputArgs.isTableRawBool() || inputArgs.isPrintDistanceFunctionsBool()) {
            
            RawTable rawTable = new RawTable(table);
            
            if (inputArgs.isTableRawBool()) {
                rawTable.printRaw(tableFileName);
            }
            if (inputArgs.isPrintDistanceFunctionsBool()) {
                rawTable.printDistanceFunctions(tableFileName);
                
                // Copies all the distance pictures into folder "pictures" for easier browsing
                new File(tableFileName + File.separator + "pictures").mkdir();
                DirectoryScanner dirtScanner = new DirectoryScanner();
                dirtScanner.setBasedir(tableFile);
                dirtScanner.setIncludes(new String[]{"**/*.png"});
                dirtScanner.scan();
                String[] picturePaths = dirtScanner.getIncludedFiles();
                
                String from;
                String to;
                for (String fromS:picturePaths) {
                    fromS =  dirtScanner.getBasedir() + File.separator + fromS;
                    from = new File(fromS).getAbsolutePath();
                    to = new File(new File(tableFileName + File.separator + "pictures").getAbsolutePath() + File.separator+ new File(fromS).getName()).getAbsolutePath();
                    java.nio.file.Files.copy(Paths.get(from), Paths.get(to));
                }
                // End of Copying
                
            }
        }
        OtherUtils.pdfLatex(texFiles);
        
        // if JAmde runs on vkstat and if argument "email" is present during the start of an app,
        // sends the user mail notifying of the end of the computation.
        // email address username@fjfi.cvut.cz is decided from the username of the user, who started the program on vkstat.
        if (InetAddress.getLocalHost().getHostName().contains("vkstat") && inputArgs.isSendEmailBool()) {
            OtherUtils.sendMail(System.getProperty("user.name") + "@fjfi.cvut.cz");
        }
        
        // stops time 
        Long timeEnd = System.currentTimeMillis();
        Long runTime = timeEnd - timeStart;
        System.out.println("Runtime = " + MathUtil.Long2time(runTime) + ".");
    }
}
